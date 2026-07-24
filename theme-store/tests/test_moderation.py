from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))

from moderation_common import (  # noqa: E402
    normalize_github_login,
    parse_issue_form,
    validate_registry,
)
from approve_creator import validate_creator_approval  # noqa: E402
from process_submission import (  # noqa: E402
    parse_manifest,
    update_catalog,
    validate_submission_event,
)
from validate_catalog import ValidationFailure, validate_embedded_assets  # noqa: E402


class ModerationTest(unittest.TestCase):
    def test_issue_form_parser_extracts_rendered_json(self) -> None:
        fields = parse_issue_form(
            "### Theme ID\n\naurora-night\n\n"
            "### Submission manifest\n\n```json\n{\"version\": 1}\n```\n"
        )
        self.assertEqual("aurora-night", fields["Theme ID"])
        self.assertEqual('{"version": 1}', fields["Submission manifest"])

    def test_github_login_rejects_consecutive_hyphens(self) -> None:
        with self.assertRaises(ValidationFailure):
            normalize_github_login("bad--login")

    def test_registry_rejects_duplicate_case_insensitive_login(self) -> None:
        registry = self.valid_registry()
        registry["creators"].append(
            {
                "github": "ALICE-theme",
                "displayName": "Duplicate",
                "approvedAt": 2,
                "status": "approved",
            }
        )
        with self.assertRaises(ValidationFailure):
            validate_registry(registry)

    def test_manifest_rejects_issue_author_mismatch(self) -> None:
        with self.assertRaises(ValidationFailure):
            parse_manifest(json.dumps(self.valid_manifest()), "another-user")

    def test_manifest_rejects_release_owned_by_another_account(self) -> None:
        manifest = self.valid_manifest()
        manifest["theme"]["packageUrl"] = (
            "https://github.com/another-user/themes/releases/download/v1/theme.kstheme"
        )
        with self.assertRaises(ValidationFailure):
            parse_manifest(json.dumps(manifest), "alice-theme")

    def test_creator_approval_rejects_non_fixz232_labeler(self) -> None:
        with self.assertRaises(ValidationFailure):
            validate_creator_approval(
                self.creator_event(sender="repository-bot"),
                "fixz232",
                100,
            )

    def test_submission_rejects_non_fixz232_labeler(self) -> None:
        with self.assertRaises(ValidationFailure):
            validate_submission_event(
                self.submission_event(sender="repository-bot"),
                "fixz232",
                self.valid_registry(),
            )

    def test_submission_rejects_issue_and_manifest_author_mismatch(self) -> None:
        event = self.submission_event(issue_author="another-user")
        with self.assertRaises(ValidationFailure):
            validate_submission_event(event, "fixz232", self.valid_registry())

    def test_cloud_package_rejects_device_uri_even_with_embedded_asset(self) -> None:
        metadata = {
            "cards": {
                "lkm": {
                    "asset": {"path": "assets/lkm.png"},
                    "uri": "content://private/lkm.png",
                }
            }
        }
        with self.assertRaises(ValidationFailure):
            validate_embedded_assets(metadata, {"assets/lkm.png"}, "theme")

    def test_catalog_adds_custom_category_and_preserves_owner(self) -> None:
        catalog = {
            "schema": "io.github.fixz.apkesu.theme-catalog",
            "version": 1,
            "generatedAt": 0,
            "categories": [],
            "themes": [],
        }
        submission = parse_manifest(json.dumps(self.valid_manifest()), "alice-theme")
        tag, asset = update_catalog(catalog, submission, "fixz232/ApkeSU", 100)

        self.assertEqual("theme-aurora-night-v1", tag)
        self.assertEqual("aurora-night-v1.kstheme", asset)
        self.assertEqual([{"id": "appearance", "name": "Appearance"}], catalog["categories"])
        self.assertEqual("alice-theme", catalog["themes"][0]["author"]["id"])

    def test_catalog_rejects_theme_id_takeover_without_mutating_catalog(self) -> None:
        catalog = self.catalog_with_existing_theme()
        before = json.loads(json.dumps(catalog))
        manifest = self.valid_manifest()
        manifest["theme"]["author"]["github"] = "mallory-theme"
        manifest["theme"]["packageUrl"] = (
            "https://github.com/mallory-theme/themes/releases/download/v1/theme.kstheme"
        )
        submission = parse_manifest(json.dumps(manifest), "mallory-theme")

        with self.assertRaises(ValidationFailure):
            update_catalog(catalog, submission, "fixz232/ApkeSU", 200)

        self.assertEqual(before, catalog)

    def test_catalog_requires_higher_version_for_changed_metadata(self) -> None:
        catalog = self.catalog_with_existing_theme()
        manifest = self.valid_manifest()
        manifest["theme"]["description"] = "Changed without a version bump."
        submission = parse_manifest(json.dumps(manifest), "alice-theme")

        with self.assertRaises(ValidationFailure):
            update_catalog(catalog, submission, "fixz232/ApkeSU", 200)

    def test_catalog_rejects_duplicate_category_name_with_new_id(self) -> None:
        catalog = self.catalog_with_existing_theme()
        before = json.loads(json.dumps(catalog))
        manifest = self.valid_manifest()
        manifest["theme"]["category"] = {
            "id": "appearance-copy",
            "name": "appearance",
        }
        submission = parse_manifest(json.dumps(manifest), "alice-theme")

        with self.assertRaises(ValidationFailure):
            update_catalog(catalog, submission, "fixz232/ApkeSU", 200)

        self.assertEqual(before, catalog)

    def test_catalog_allows_exact_same_version_retry(self) -> None:
        catalog = self.catalog_with_existing_theme()
        before = json.loads(json.dumps(catalog))
        submission = parse_manifest(json.dumps(self.valid_manifest()), "alice-theme")

        update_catalog(catalog, submission, "fixz232/ApkeSU", 200)

        self.assertEqual(before, catalog)

    def valid_registry(self) -> dict:
        return {
            "schema": "io.github.fixz.apkesu.theme-creators",
            "version": 1,
            "generatedAt": 1,
            "reviewer": "fixz232",
            "creators": [
                {
                    "github": "alice-theme",
                    "displayName": "Alice",
                    "approvedAt": 1,
                    "status": "approved",
                }
            ],
        }

    def creator_event(self, *, sender: str = "fixz232") -> dict:
        return {
            "sender": {"login": sender},
            "label": {"name": "creator-approved"},
            "issue": {
                "title": "[Creator application] alice-theme",
                "user": {"login": "alice-theme"},
                "labels": [{"name": "creator-approved"}],
                "body": (
                    "### GitHub login\n\nalice-theme\n\n"
                    "### Public creator name\n\nAlice\n\n"
                    "### Declarations\n\n- [x] one\n- [x] two\n- [x] three"
                ),
            },
        }

    def submission_event(
        self,
        *,
        sender: str = "fixz232",
        issue_author: str = "alice-theme",
    ) -> dict:
        manifest = self.valid_manifest()
        theme = manifest["theme"]
        return {
            "sender": {"login": sender},
            "label": {"name": "theme-approved"},
            "issue": {
                "title": "[Cloud theme] aurora-night - Aurora Night",
                "user": {"login": issue_author},
                "labels": [{"name": "theme-approved"}],
                "body": (
                    f"### Theme ID\n\n{theme['id']}\n\n"
                    f"### Category\n\n{theme['category']['id']} | {theme['category']['name']}\n\n"
                    f"### GitHub-hosted package URL\n\n{theme['packageUrl']}\n\n"
                    "### Submission manifest\n\n```json\n"
                    f"{json.dumps(manifest)}\n```\n\n"
                    "### Declarations\n\n- [x] one\n- [x] two\n- [x] three\n- [x] four"
                ),
            },
        }

    def catalog_with_existing_theme(self) -> dict:
        catalog = {
            "schema": "io.github.fixz.apkesu.theme-catalog",
            "version": 1,
            "generatedAt": 100,
            "categories": [],
            "themes": [],
        }
        submission = parse_manifest(json.dumps(self.valid_manifest()), "alice-theme")
        update_catalog(catalog, submission, "fixz232/ApkeSU", 100)
        return catalog

    def valid_manifest(self) -> dict:
        return {
            "schema": "io.github.fixz.apkesu.theme-submission",
            "version": 1,
            "theme": {
                "id": "aurora-night",
                "name": "Aurora Night",
                "description": "A complete theme.",
                "category": {"id": "appearance", "name": "Appearance"},
                "tags": ["dark"],
                "versionCode": 1,
                "versionName": "1.0.0",
                "packageSchema": "io.github.fixz.apkesu.theme",
                "packageVersion": 4,
                "minManagerVersionCode": 32700,
                "maxManagerVersionCode": None,
                "coverUrl": "https://raw.githubusercontent.com/alice-theme/themes/main/cover.png",
                "screenshots": [],
                "packageUrl": "https://github.com/alice-theme/themes/releases/download/v1/theme.kstheme",
                "sha256": "a" * 64,
                "sizeBytes": 4096,
                "license": "CC-BY-4.0",
                "changelog": "Initial",
                "author": {
                    "github": "alice-theme",
                    "name": "Alice",
                    "profileUrl": "https://github.com/alice-theme",
                    "bio": "Creator",
                },
            },
        }


if __name__ == "__main__":
    unittest.main()
