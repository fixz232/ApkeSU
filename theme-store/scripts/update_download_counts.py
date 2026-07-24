#!/usr/bin/env python3
"""Update catalog download counts from GitHub Release asset statistics."""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.request
from pathlib import Path

from validate_catalog import ValidationFailure, load_json, validate_semantics


def github_json(url: str, token: str) -> list[dict]:
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "User-Agent": "ApkeSU-theme-statistics/1",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        value = json.load(response)
    if not isinstance(value, list):
        raise ValidationFailure("GitHub releases response is invalid")
    return value


def fetch_asset_counts(repository: str, token: str) -> dict[str, int]:
    result: dict[str, int] = {}
    for page in range(1, 101):
        releases = github_json(
            f"https://api.github.com/repos/{repository}/releases?per_page=100&page={page}",
            token,
        )
        for release in releases:
            for asset in release.get("assets", []):
                url = asset.get("browser_download_url")
                count = asset.get("download_count")
                if isinstance(url, str) and isinstance(count, int) and count >= 0:
                    result[url] = count
        if len(releases) < 100:
            break
    return result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path)
    parser.add_argument("--repository", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        print("GITHUB_TOKEN is required", file=sys.stderr)
        return 1
    try:
        catalog = load_json(args.catalog)
        validate_semantics(catalog, previous=None)
        counts = fetch_asset_counts(args.repository, token)
        changed = False
        for theme in catalog["themes"]:
            count = counts.get(theme["downloadUrl"])
            if count is not None and theme.get("downloadCount") != count:
                theme["downloadCount"] = count
                changed = True
        if changed:
            catalog["generatedAt"] = int(time.time() * 1000)
            args.catalog.write_text(
                json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        print(f"updated {len(catalog['themes'])} theme entries; changed={changed}")
        return 0
    except (ValidationFailure, OSError, ValueError) as error:
        print(f"statistics update failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
