#!/usr/bin/env python3
"""Add a reviewed GitHub Issue author to the cloud-theme creator registry."""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

from moderation_common import (
    clean_text,
    issue_labels,
    load_object,
    normalize_github_login,
    parse_issue_form,
    validate_registry,
    write_object,
)
from validate_catalog import ValidationFailure


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--registry", required=True, type=Path)
    parser.add_argument("--event", required=True, type=Path)
    parser.add_argument("--reviewer", required=True)
    return parser.parse_args()


def validate_creator_approval(event: dict, reviewer: str, approved_at: int) -> dict:
    normalized_reviewer = normalize_github_login(reviewer)
    if normalized_reviewer != "fixz232":
        raise ValidationFailure("only fixz232 may approve creators")
    sender = normalize_github_login((event.get("sender") or {}).get("login"))
    if sender != normalized_reviewer:
        raise ValidationFailure("approval label was not applied by fixz232")
    if (event.get("label") or {}).get("name", "").lower() != "creator-approved":
        raise ValidationFailure("creator-approved label is required")

    issue = event.get("issue")
    if not isinstance(issue, dict) or not str(issue.get("title", "")).startswith("[Creator application]"):
        raise ValidationFailure("issue is not a creator application")
    if "creator-approved" not in issue_labels(issue):
        raise ValidationFailure("creator approval label is missing")
    issue_author = normalize_github_login((issue.get("user") or {}).get("login"))
    expected_title = f"[creator application] {issue_author}"
    if str(issue.get("title", "")).strip().lower() != expected_title:
        raise ValidationFailure("creator application title does not match the issue author")
    fields = parse_issue_form(issue.get("body"))
    declared_login = normalize_github_login(fields.get("GitHub login"))
    if declared_login != issue_author:
        raise ValidationFailure("declared GitHub login does not match the issue author")
    display_name = clean_text(fields.get("Public creator name"), "public creator name", 64)
    clean_text(fields.get("Introduction"), "introduction", 2000)
    declarations = fields.get("Declarations", "").lower()
    if declarations.count("- [x]") < 3:
        raise ValidationFailure("all creator declarations must be accepted")
    if not isinstance(approved_at, int) or approved_at < 0:
        raise ValidationFailure("approval time is invalid")
    return {
        "github": issue_author,
        "displayName": display_name,
        "approvedAt": approved_at,
        "status": "approved",
    }


def main() -> int:
    args = parse_args()
    try:
        event = load_object(args.event)
        registry = load_object(args.registry)
        creators = validate_registry(registry)
        approved_at = int(time.time() * 1000)
        replacement = validate_creator_approval(event, args.reviewer, approved_at)
        issue_author = replacement["github"]
        updated = [
            replacement if normalize_github_login(item["github"]) == issue_author else item
            for item in creators
        ]
        if not any(normalize_github_login(item["github"]) == issue_author for item in creators):
            updated.append(replacement)
        registry["creators"] = sorted(updated, key=lambda item: item["github"].lower())
        registry["generatedAt"] = approved_at
        write_object(args.registry, registry)
        print(json.dumps(replacement, ensure_ascii=False))
        return 0
    except (ValidationFailure, OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"creator approval failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
