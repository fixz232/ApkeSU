#!/usr/bin/env python3
"""Shared validation helpers for GitHub Issue based theme moderation."""

from __future__ import annotations

import json
import re
from pathlib import Path

from validate_catalog import ValidationFailure

CREATOR_REGISTRY_SCHEMA = "io.github.fixz.apkesu.theme-creators"
SUBMISSION_SCHEMA = "io.github.fixz.apkesu.theme-submission"
GITHUB_LOGIN_RE = re.compile(
    r"^(?!.*--)[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$"
)
ISSUE_HEADING_RE = re.compile(r"^###\s+(.+?)\s*$")


def load_object(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValidationFailure(f"{path}: root must be an object")
    return value


def write_object(path: Path, value: dict) -> None:
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.parent.mkdir(parents=True, exist_ok=True)
    temporary.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    temporary.replace(path)


def normalize_github_login(value: object) -> str:
    if not isinstance(value, str):
        raise ValidationFailure("GitHub login must be a string")
    login = value.strip()
    if not GITHUB_LOGIN_RE.fullmatch(login):
        raise ValidationFailure("invalid GitHub login")
    return login.lower()


def clean_text(value: object, label: str, maximum: int, *, required: bool = True) -> str:
    if not isinstance(value, str):
        raise ValidationFailure(f"{label} must be a string")
    text = value.strip()
    if required and not text:
        raise ValidationFailure(f"{label} is required")
    if len(text) > maximum or any(ord(character) < 32 and character not in "\n\t" for character in text):
        raise ValidationFailure(f"{label} is invalid or too long")
    return text


def parse_issue_form(body: object) -> dict[str, str]:
    if not isinstance(body, str) or len(body) > 128 * 1024:
        raise ValidationFailure("issue body is missing or too large")
    fields: dict[str, list[str]] = {}
    current: str | None = None
    for line in body.splitlines():
        heading = ISSUE_HEADING_RE.match(line)
        if heading:
            current = heading.group(1).strip()
            if current in fields:
                raise ValidationFailure(f"duplicate issue field: {current}")
            fields[current] = []
        elif current is not None:
            fields[current].append(line)

    parsed: dict[str, str] = {}
    for heading, lines in fields.items():
        value = "\n".join(lines).strip()
        if value.startswith("```" ):
            first_newline = value.find("\n")
            if first_newline >= 0 and value.endswith("```"):
                value = value[first_newline + 1 : -3].strip()
        if value == "_No response_":
            value = ""
        parsed[heading] = value
    return parsed


def validate_registry(registry: dict) -> list[dict]:
    if registry.get("schema") != CREATOR_REGISTRY_SCHEMA or registry.get("version") != 1:
        raise ValidationFailure("unsupported creator registry")
    if normalize_github_login(registry.get("reviewer")) != "fixz232":
        raise ValidationFailure("creator registry reviewer must be fixz232")
    creators = registry.get("creators")
    if not isinstance(creators, list) or len(creators) > 500:
        raise ValidationFailure("creator registry is invalid")
    seen: set[str] = set()
    for index, creator in enumerate(creators):
        if not isinstance(creator, dict):
            raise ValidationFailure(f"creator {index} is invalid")
        login = normalize_github_login(creator.get("github"))
        if login in seen:
            raise ValidationFailure(f"duplicate creator: {login}")
        seen.add(login)
        clean_text(creator.get("displayName"), f"creator {login} display name", 64)
        if creator.get("status") != "approved":
            raise ValidationFailure(f"creator {login} has an unsupported status")
        if not isinstance(creator.get("approvedAt"), int) or creator["approvedAt"] < 0:
            raise ValidationFailure(f"creator {login} has an invalid approval time")
    return creators


def issue_labels(issue: dict) -> set[str]:
    labels = issue.get("labels", [])
    return {
        str(item.get("name", "")).strip().lower()
        for item in labels
        if isinstance(item, dict) and item.get("name")
    }
