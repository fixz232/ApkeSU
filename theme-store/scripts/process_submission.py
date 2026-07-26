#!/usr/bin/env python3
"""Verify an approved creator submission and update the cloud-theme catalog."""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

from moderation_common import (
    SUBMISSION_SCHEMA,
    clean_text,
    issue_labels,
    load_object,
    normalize_github_login,
    parse_issue_form,
    validate_registry,
    write_object,
)
from prepare_release import download_and_verify_package
from validate_catalog import (
    CATEGORY_RE,
    ID_RE,
    LICENSE_RE,
    MAX_PACKAGE_BYTES,
    PACKAGE_SCHEMA,
    SHA_RE,
    ValidationFailure,
    validate_image,
    validate_semantics,
    validate_url,
)

THEME_KEYS = {
    "id",
    "name",
    "description",
    "category",
    "tags",
    "versionCode",
    "versionName",
    "packageSchema",
    "packageVersion",
    "minManagerVersionCode",
    "maxManagerVersionCode",
    "coverUrl",
    "screenshots",
    "packageUrl",
    "sha256",
    "sizeBytes",
    "license",
    "changelog",
    "author",
}
THEME_REQUIRED_KEYS = THEME_KEYS - {"coverUrl"}
AUTHOR_KEYS = {"github", "name", "profileUrl", "avatarUrl", "bio"}
CATEGORY_KEYS = {"id", "name"}
DEFAULT_COVER_URL = (
    "https://raw.githubusercontent.com/fixz232/ApkeSU/ApkeSU/"
    "manager/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--registry", required=True, type=Path)
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--event", required=True, type=Path)
    parser.add_argument("--reviewer", required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--result", required=True, type=Path)
    return parser.parse_args()


def require_exact_keys(value: dict, allowed: set[str], required: set[str], label: str) -> None:
    unknown = set(value) - allowed
    missing = required - set(value)
    if unknown:
        raise ValidationFailure(f"{label} contains unsupported fields: {sorted(unknown)}")
    if missing:
        raise ValidationFailure(f"{label} is missing fields: {sorted(missing)}")


def required_integer(value: object, label: str, minimum: int, maximum: int | None = None) -> int:
    if not isinstance(value, int) or isinstance(value, bool) or value < minimum:
        raise ValidationFailure(f"{label} is invalid")
    if maximum is not None and value > maximum:
        raise ValidationFailure(f"{label} is invalid")
    return value


def optional_url(value: object, label: str) -> str | None:
    if value is None or value == "":
        return None
    return validate_url(value, label)


def validate_creator_release_url(value: str, github_login: str) -> None:
    parsed = urlparse(value)
    raw_parts = parsed.path.split("/")
    if raw_parts[0] or any(not part for part in raw_parts[1:]):
        raise ValidationFailure("package URL has an invalid GitHub Release path")
    parts = raw_parts[1:]
    if (
        (parsed.hostname or "").lower() != "github.com"
        or len(parts) < 6
        or parts[0].lower() != github_login
        or parts[2] != "releases"
        or parts[3] != "download"
        or any(part in {".", ".."} for part in parts[4:])
    ):
        raise ValidationFailure(
            "package must be uploaded to a Release under the creator's GitHub account"
        )


def parse_manifest(raw: str, issue_author: str) -> dict:
    if len(raw.encode("utf-8")) > 64 * 1024:
        raise ValidationFailure("submission manifest is too large")
    try:
        manifest = json.loads(raw)
    except json.JSONDecodeError as error:
        raise ValidationFailure("submission manifest is not valid JSON") from error
    if not isinstance(manifest, dict):
        raise ValidationFailure("submission manifest root must be an object")
    require_exact_keys(manifest, {"schema", "version", "theme"}, {"schema", "version", "theme"}, "manifest")
    if manifest.get("schema") != SUBMISSION_SCHEMA or manifest.get("version") != 1:
        raise ValidationFailure("unsupported submission manifest")

    theme = manifest.get("theme")
    if not isinstance(theme, dict):
        raise ValidationFailure("manifest theme must be an object")
    require_exact_keys(theme, THEME_KEYS, THEME_REQUIRED_KEYS, "manifest theme")

    theme_id = clean_text(theme.get("id"), "theme id", 80)
    if not ID_RE.fullmatch(theme_id):
        raise ValidationFailure("theme id is invalid")
    name = clean_text(theme.get("name"), "theme name", 80)
    description = clean_text(theme.get("description"), "theme description", 1000)

    category = theme.get("category")
    if not isinstance(category, dict):
        raise ValidationFailure("theme category must be an object")
    require_exact_keys(category, CATEGORY_KEYS, CATEGORY_KEYS, "theme category")
    category_id = clean_text(category.get("id"), "category id", 40)
    category_name = clean_text(category.get("name"), "category name", 48)
    if not CATEGORY_RE.fullmatch(category_id):
        raise ValidationFailure("category id is invalid")

    tags = theme.get("tags")
    if not isinstance(tags, list) or len(tags) > 12:
        raise ValidationFailure("theme tags are invalid")
    normalized_tags: list[str] = []
    seen_tags: set[str] = set()
    for index, tag in enumerate(tags):
        cleaned = clean_text(tag, f"tag {index}", 32)
        folded = cleaned.casefold()
        if folded in seen_tags:
            raise ValidationFailure("theme tags must be unique")
        seen_tags.add(folded)
        normalized_tags.append(cleaned)

    version_code = required_integer(theme.get("versionCode"), "version code", 1)
    version_name = clean_text(theme.get("versionName"), "version name", 40)
    if theme.get("packageSchema") != PACKAGE_SCHEMA:
        raise ValidationFailure("package schema is unsupported")
    package_version = required_integer(theme.get("packageVersion"), "package version", 1, 4)
    minimum = required_integer(
        theme.get("minManagerVersionCode"),
        "minimum Manager version",
        1,
    )
    maximum_value = theme.get("maxManagerVersionCode")
    maximum = None if maximum_value is None else required_integer(
        maximum_value,
        "maximum Manager version",
        minimum,
    )

    screenshots_value = theme.get("screenshots")
    if not isinstance(screenshots_value, list) or len(screenshots_value) > 8:
        raise ValidationFailure("screenshots are invalid")
    screenshots = [
        validate_url(value, f"screenshot {index}")
        for index, value in enumerate(screenshots_value)
    ]
    if len(set(screenshots)) != len(screenshots):
        raise ValidationFailure("screenshot URLs must be unique")
    cover_value = theme.get("coverUrl")
    if cover_value is None or (isinstance(cover_value, str) and not cover_value.strip()):
        cover_url = screenshots[0] if screenshots else DEFAULT_COVER_URL
    else:
        cover_url = validate_url(cover_value, "cover URL")
    package_url = validate_url(theme.get("packageUrl"), "package URL", package=True)
    sha256 = clean_text(theme.get("sha256"), "SHA-256", 64).lower()
    if not SHA_RE.fullmatch(sha256):
        raise ValidationFailure("SHA-256 is invalid")
    size_bytes = required_integer(theme.get("sizeBytes"), "package size", 1, MAX_PACKAGE_BYTES)
    license_id = clean_text(theme.get("license"), "asset license", 48)
    if not LICENSE_RE.fullmatch(license_id):
        raise ValidationFailure("asset license is invalid")
    changelog = clean_text(theme.get("changelog"), "changelog", 4000, required=False)

    author = theme.get("author")
    if not isinstance(author, dict):
        raise ValidationFailure("theme author must be an object")
    require_exact_keys(
        author,
        AUTHOR_KEYS,
        {"github", "name", "bio"},
        "theme author",
    )
    author_login = normalize_github_login(author.get("github"))
    if author_login != issue_author:
        raise ValidationFailure("manifest author does not match the issue author")
    validate_creator_release_url(package_url, author_login)
    author_name = clean_text(author.get("name"), "author name", 64)
    author_bio = clean_text(author.get("bio"), "author bio", 512, required=False)
    profile_url = optional_url(author.get("profileUrl"), "author profile URL")
    avatar_url = optional_url(author.get("avatarUrl"), "author avatar URL")

    return {
        "id": theme_id,
        "name": name,
        "description": description,
        "category": {"id": category_id, "name": category_name},
        "tags": normalized_tags,
        "versionCode": version_code,
        "versionName": version_name,
        "packageSchema": PACKAGE_SCHEMA,
        "packageVersion": package_version,
        "minManagerVersionCode": minimum,
        "maxManagerVersionCode": maximum,
        "coverUrl": cover_url,
        "screenshots": screenshots,
        "packageUrl": package_url,
        "sha256": sha256,
        "sizeBytes": size_bytes,
        "license": license_id,
        "changelog": changelog,
        "author": {
            "github": author_login,
            "name": author_name,
            "profileUrl": profile_url,
            "avatarUrl": avatar_url,
            "bio": author_bio,
        },
    }


def validate_submission_event(event: dict, reviewer: str, registry: dict) -> dict:
    normalized_reviewer = normalize_github_login(reviewer)
    if normalized_reviewer != "fixz232":
        raise ValidationFailure("only fixz232 may approve theme submissions")
    sender = normalize_github_login((event.get("sender") or {}).get("login"))
    if sender != normalized_reviewer:
        raise ValidationFailure("theme-approved label was not applied by fixz232")
    if str((event.get("label") or {}).get("name", "")).lower() != "theme-approved":
        raise ValidationFailure("theme-approved label is required")

    issue = event.get("issue")
    if not isinstance(issue, dict) or not str(issue.get("title", "")).startswith("[Cloud theme]"):
        raise ValidationFailure("issue is not a cloud-theme submission")
    if "theme-approved" not in issue_labels(issue):
        raise ValidationFailure("theme approval label is missing")
    issue_author = normalize_github_login((issue.get("user") or {}).get("login"))

    creators = validate_registry(registry)
    approved = {
        normalize_github_login(item["github"])
        for item in creators
        if item.get("status") == "approved"
    }
    if issue_author not in approved:
        raise ValidationFailure("issue author is not an approved creator")

    fields = parse_issue_form(issue.get("body"))
    declarations = fields.get("Declarations", "").lower()
    if declarations.count("- [x]") < 4:
        raise ValidationFailure("all submission declarations must be accepted")
    submission = parse_manifest(fields.get("Submission manifest", ""), issue_author)
    if clean_text(fields.get("Theme ID"), "issue theme id", 80) != submission["id"]:
        raise ValidationFailure("visible theme ID does not match the manifest")
    if clean_text(fields.get("GitHub-hosted package URL"), "issue package URL", 768) != submission["packageUrl"]:
        raise ValidationFailure("visible package URL does not match the manifest")
    visible_category = clean_text(fields.get("Category"), "issue category", 96)
    expected_category = f"{submission['category']['id']} | {submission['category']['name']}"
    if visible_category != expected_category:
        raise ValidationFailure("visible category does not match the manifest")
    return submission


def update_catalog(catalog: dict, submission: dict, repository: str, published_at: int) -> tuple[str, str]:
    original = json.loads(json.dumps(catalog))
    updated_catalog = json.loads(json.dumps(catalog))
    categories = updated_catalog.get("categories")
    themes = updated_catalog.get("themes")
    if not isinstance(categories, list) or not isinstance(themes, list):
        raise ValidationFailure("catalog arrays are invalid")

    category = submission["category"]
    existing_category = next((item for item in categories if item.get("id") == category["id"]), None)
    if existing_category is None:
        if len(categories) >= 64:
            raise ValidationFailure("catalog category limit reached")
        if any(
            str(item.get("name", "")).casefold() == category["name"].casefold()
            for item in categories
        ):
            raise ValidationFailure("category name already belongs to another category ID")
        categories.append({"id": category["id"], "name": category["name"]})
        categories.sort(key=lambda item: item["id"])
    elif str(existing_category.get("name", "")).casefold() != category["name"].casefold():
        raise ValidationFailure("category ID already exists with a different name")

    existing = next((item for item in themes if item.get("id") == submission["id"]), None)
    if existing is not None:
        existing_author = normalize_github_login((existing.get("author") or {}).get("id"))
        if existing_author != submission["author"]["github"]:
            raise ValidationFailure("theme ID belongs to another creator")
        old_version = int(existing.get("versionCode", 0))
        if submission["versionCode"] < old_version:
            raise ValidationFailure("theme version code cannot decrease")

    repository_parts = repository.split("/", maxsplit=1)
    if len(repository_parts) != 2 or not all(repository_parts):
        raise ValidationFailure("invalid GitHub repository name")
    tag = f"theme-{submission['id']}-v{submission['versionCode']}"
    asset_name = f"{submission['id']}-v{submission['versionCode']}.kstheme"
    canonical_url = f"https://github.com/{repository}/releases/download/{tag}/{asset_name}"
    author = {
        "id": submission["author"]["github"],
        "name": submission["author"]["name"],
        "bio": submission["author"]["bio"],
    }
    if submission["author"]["profileUrl"]:
        author["profileUrl"] = submission["author"]["profileUrl"]
    if submission["author"]["avatarUrl"]:
        author["avatarUrl"] = submission["author"]["avatarUrl"]

    same_version = existing is not None and submission["versionCode"] == existing.get("versionCode")
    entry = {
        "id": submission["id"],
        "name": submission["name"],
        "author": author,
        "description": submission["description"],
        "category": category["id"],
        "tags": submission["tags"],
        "versionCode": submission["versionCode"],
        "versionName": submission["versionName"],
        "packageSchema": PACKAGE_SCHEMA,
        "packageVersion": submission["packageVersion"],
        "minManagerVersionCode": submission["minManagerVersionCode"],
        "maxManagerVersionCode": submission["maxManagerVersionCode"],
        "coverUrl": submission["coverUrl"],
        "screenshots": submission["screenshots"],
        "downloadUrl": canonical_url,
        "sha256": submission["sha256"],
        "sizeBytes": submission["sizeBytes"],
        "license": submission["license"],
        "changelog": submission["changelog"],
        "publishedAt": existing.get("publishedAt", published_at) if same_version else published_at,
        "status": "published",
        "featured": bool(existing.get("featured", False)) if existing else False,
        "downloadCount": int(existing.get("downloadCount", 0)) if existing else 0,
    }
    if same_version:
        replay_fields = set(entry) - {"publishedAt", "featured", "downloadCount"}
        if any(existing.get(field) != entry.get(field) for field in replay_fields):
            raise ValidationFailure("theme updates require a higher version code")

    updated_catalog["themes"] = sorted(
        [item for item in themes if item.get("id") != submission["id"]] + [entry],
        key=lambda item: item["id"],
    )
    updated_catalog["generatedAt"] = original.get("generatedAt", 0) if same_version else published_at
    validate_semantics(updated_catalog, original)
    catalog.clear()
    catalog.update(updated_catalog)
    return tag, asset_name


def main() -> int:
    args = parse_args()
    try:
        event = load_object(args.event)
        registry = load_object(args.registry)
        submission = validate_submission_event(event, args.reviewer, registry)

        published_at = int(time.time() * 1000)
        catalog = load_object(args.catalog)
        tag, asset_name = update_catalog(catalog, submission, args.repository, published_at)
        package_path = download_and_verify_package(
            theme_id=submission["id"],
            version_code=submission["versionCode"],
            package_version=submission["packageVersion"],
            source_url=submission["packageUrl"],
            asset_name=asset_name,
            sha256=submission["sha256"],
            size_bytes=submission["sizeBytes"],
            output_dir=args.output_dir,
        )
        validate_image(submission["coverUrl"])
        for screenshot in submission["screenshots"]:
            validate_image(screenshot)

        write_object(args.catalog, catalog)
        result = {
            "themeId": submission["id"],
            "versionCode": submission["versionCode"],
            "tag": tag,
            "assetName": asset_name,
            "packagePath": str(package_path),
        }
        write_object(args.result, result)
        print(json.dumps(result))
        return 0
    except (
        ValidationFailure,
        OSError,
        ValueError,
        TypeError,
        KeyError,
        AttributeError,
        json.JSONDecodeError,
    ) as error:
        print(f"theme submission failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
