#!/usr/bin/env python3
"""Validate the ApkeSU cloud-theme catalog and optional remote assets."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath
from urllib.parse import urlparse

CATALOG_SCHEMA = "io.github.fixz.apkesu.theme-catalog"
PACKAGE_SCHEMA = "io.github.fixz.apkesu.theme"
MAX_PACKAGE_BYTES = 100 * 1024 * 1024
MAX_IMAGE_BYTES = 12 * 1024 * 1024
MAX_ZIP_ASSET_BYTES = 512 * 1024 * 1024
ALLOWED_EXACT_HOSTS = {
    "github.com",
    "raw.githubusercontent.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
    "githubusercontent.com",
}
ID_RE = re.compile(r"^[a-z0-9][a-z0-9._-]{1,79}$")
CATEGORY_RE = re.compile(r"^[a-z0-9][a-z0-9_-]{1,39}$")
SHA_RE = re.compile(r"^[a-fA-F0-9]{64}$")
LICENSE_RE = re.compile(r"^[A-Za-z0-9.+-]{1,48}$")


class ValidationFailure(Exception):
    pass


def load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValidationFailure(f"{path}: root must be an object")
    return value


def is_allowed_host(host: str | None) -> bool:
    normalized = (host or "").lower()
    return normalized in ALLOWED_EXACT_HOSTS or normalized.endswith(".githubusercontent.com")


def validate_url(value: object, label: str, package: bool = False) -> str:
    if not isinstance(value, str) or not value or len(value) > 768:
        raise ValidationFailure(f"{label}: invalid URL")
    parsed = urlparse(value)
    if parsed.scheme != "https" or not is_allowed_host(parsed.hostname):
        raise ValidationFailure(f"{label}: only approved GitHub HTTPS hosts are allowed")
    if parsed.username or parsed.password or parsed.fragment or not parsed.path:
        raise ValidationFailure(f"{label}: URL contains unsupported data")
    if package and not parsed.path.lower().endswith(".kstheme"):
        raise ValidationFailure(f"{label}: package URL must end in .kstheme")
    return value


def validate_semantics(catalog: dict, previous: dict | None) -> list[dict]:
    if catalog.get("schema") != CATALOG_SCHEMA or catalog.get("version") != 1:
        raise ValidationFailure("unsupported catalog schema or version")
    if not isinstance(catalog.get("generatedAt"), int) or catalog["generatedAt"] < 0:
        raise ValidationFailure("generatedAt must be a non-negative integer")

    categories = catalog.get("categories")
    themes = catalog.get("themes")
    if not isinstance(categories, list) or len(categories) > 64:
        raise ValidationFailure("categories must be an array with at most 64 entries")
    if not isinstance(themes, list) or len(themes) > 500:
        raise ValidationFailure("themes must be an array with at most 500 entries")

    category_ids: set[str] = set()
    for index, category in enumerate(categories):
        if not isinstance(category, dict):
            raise ValidationFailure(f"categories[{index}] must be an object")
        category_id = category.get("id")
        name = category.get("name")
        if not isinstance(category_id, str) or not CATEGORY_RE.fullmatch(category_id):
            raise ValidationFailure(f"categories[{index}].id is invalid")
        if category_id in category_ids:
            raise ValidationFailure(f"duplicate category id: {category_id}")
        if not isinstance(name, str) or not name.strip() or len(name.strip()) > 48:
            raise ValidationFailure(f"categories[{index}].name is invalid")
        category_ids.add(category_id)

    theme_ids: set[str] = set()
    for index, theme in enumerate(themes):
        if not isinstance(theme, dict):
            raise ValidationFailure(f"themes[{index}] must be an object")
        theme_id = theme.get("id")
        label = f"themes[{index}]"
        if not isinstance(theme_id, str) or not ID_RE.fullmatch(theme_id):
            raise ValidationFailure(f"{label}.id is invalid")
        if theme_id in theme_ids:
            raise ValidationFailure(f"duplicate theme id: {theme_id}")
        theme_ids.add(theme_id)
        if theme.get("category") not in category_ids:
            raise ValidationFailure(f"{label}.category is not declared")
        if theme.get("packageSchema") != PACKAGE_SCHEMA:
            raise ValidationFailure(f"{label}.packageSchema is unsupported")
        if not isinstance(theme.get("packageVersion"), int) or not 1 <= theme["packageVersion"] <= 4:
            raise ValidationFailure(f"{label}.packageVersion is unsupported")
        if not isinstance(theme.get("versionCode"), int) or theme["versionCode"] < 1:
            raise ValidationFailure(f"{label}.versionCode is invalid")
        minimum = theme.get("minManagerVersionCode")
        maximum = theme.get("maxManagerVersionCode")
        if not isinstance(minimum, int) or minimum < 1:
            raise ValidationFailure(f"{label}.minManagerVersionCode is invalid")
        if maximum is not None and (not isinstance(maximum, int) or maximum < minimum):
            raise ValidationFailure(f"{label}.maxManagerVersionCode is invalid")
        if not isinstance(theme.get("sizeBytes"), int) or not 1 <= theme["sizeBytes"] <= MAX_PACKAGE_BYTES:
            raise ValidationFailure(f"{label}.sizeBytes is invalid")
        if not isinstance(theme.get("sha256"), str) or not SHA_RE.fullmatch(theme["sha256"]):
            raise ValidationFailure(f"{label}.sha256 is invalid")
        if not isinstance(theme.get("license"), str) or not LICENSE_RE.fullmatch(theme["license"]):
            raise ValidationFailure(f"{label}.license must be a valid SPDX-style identifier")
        if theme.get("status") not in {"published", "deprecated"}:
            raise ValidationFailure(f"{label}.status is invalid")
        validate_url(theme.get("coverUrl"), f"{label}.coverUrl")
        validate_url(theme.get("downloadUrl"), f"{label}.downloadUrl", package=True)
        screenshots = theme.get("screenshots")
        if not isinstance(screenshots, list) or len(screenshots) > 8 or len(set(screenshots)) != len(screenshots):
            raise ValidationFailure(f"{label}.screenshots is invalid")
        for screenshot_index, screenshot in enumerate(screenshots):
            validate_url(screenshot, f"{label}.screenshots[{screenshot_index}]")
        author = theme.get("author")
        if not isinstance(author, dict) or not ID_RE.fullmatch(str(author.get("id", ""))):
            raise ValidationFailure(f"{label}.author is invalid")
        for key in ("profileUrl", "avatarUrl"):
            if key in author:
                validate_url(author[key], f"{label}.author.{key}")

    if previous:
        previous_by_id = {
            item.get("id"): item
            for item in previous.get("themes", [])
            if isinstance(item, dict) and isinstance(item.get("id"), str)
        }
        identity_fields = ("versionName", "downloadUrl", "sha256", "sizeBytes", "packageVersion")
        for theme in themes:
            old = previous_by_id.get(theme["id"])
            if not old:
                continue
            if theme["versionCode"] < old.get("versionCode", 0):
                raise ValidationFailure(f"{theme['id']}: versionCode cannot decrease")
            identity_changed = any(theme.get(field) != old.get(field) for field in identity_fields)
            if identity_changed and theme["versionCode"] <= old.get("versionCode", 0):
                raise ValidationFailure(
                    f"{theme['id']}: package identity changed without increasing versionCode"
                )
    return themes


def validate_with_schema(catalog: dict, schema_path: Path | None) -> None:
    if schema_path is None:
        return
    try:
        import jsonschema
    except ImportError as error:
        raise ValidationFailure("jsonschema is required when --schema is used") from error
    schema = load_json(schema_path)
    validator = jsonschema.Draft202012Validator(
        schema,
        format_checker=jsonschema.FormatChecker(),
    )
    errors = sorted(validator.iter_errors(catalog), key=lambda item: list(item.absolute_path))
    if errors:
        first = errors[0]
        location = ".".join(str(part) for part in first.absolute_path) or "root"
        raise ValidationFailure(f"schema error at {location}: {first.message}")


def open_remote(url: str):
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "ApkeSU-theme-validator/1", "Accept": "*/*"},
    )
    response = urllib.request.urlopen(request, timeout=45)
    final_url = urlparse(response.geturl())
    final_host = final_url.hostname
    if final_url.scheme != "https" or not is_allowed_host(final_host):
        response.close()
        raise ValidationFailure(f"remote URL redirected to unsupported host: {final_host}")
    return response


def read_remote(url: str, maximum: int) -> bytes:
    with open_remote(url) as response:
        declared = response.headers.get("Content-Length")
        if declared and int(declared) > maximum:
            raise ValidationFailure(f"remote asset exceeds {maximum} bytes: {url}")
        chunks: list[bytes] = []
        total = 0
        while True:
            chunk = response.read(64 * 1024)
            if not chunk:
                break
            total += len(chunk)
            if total > maximum:
                raise ValidationFailure(f"remote asset exceeds {maximum} bytes: {url}")
            chunks.append(chunk)
        return b"".join(chunks)


def validate_image(url: str) -> None:
    payload = read_remote(url, MAX_IMAGE_BYTES)
    known = (
        payload.startswith(b"\x89PNG\r\n\x1a\n")
        or payload.startswith(b"\xff\xd8\xff")
        or payload.startswith((b"GIF87a", b"GIF89a"))
        or (payload.startswith(b"RIFF") and payload[8:12] == b"WEBP")
        or (len(payload) >= 12 and payload[4:12] in {b"ftypavif", b"ftypavis"})
    )
    if not known:
        raise ValidationFailure(f"remote image has an unsupported format: {url}")


def validate_zip_entry(name: str) -> str:
    if not name or "\\" in name or "\x00" in name or name.startswith("/"):
        raise ValidationFailure(f"package contains unsafe ZIP path: {name!r}")
    normalized = name[:-1] if name.endswith("/") else name
    parts = PurePosixPath(normalized).parts
    if not normalized or any(part in {"", ".", ".."} or ":" in part for part in parts):
        raise ValidationFailure(f"package contains unsafe ZIP path: {name!r}")
    return normalized


def validate_embedded_assets(metadata: dict, archive_names: set[str], label: str) -> None:
    owners: list[dict] = []
    for section in ("cards", "navigationIcons", "pageBackgrounds"):
        section_value = metadata.get(section, {})
        if not isinstance(section_value, dict):
            raise ValidationFailure(f"{label}: invalid {section} metadata")
        for owner in section_value.values():
            if isinstance(owner, dict):
                owners.append(owner)
    owners.extend(
        owner
        for key in ("wallpaper", "startupSound", "clickSound", "backgroundMusic", "startupAnimation")
        if isinstance((owner := metadata.get(key)), dict)
    )
    author = metadata.get("author")
    if isinstance(author, dict):
        asset = author.get("avatar")
        if asset is not None:
            if not isinstance(asset, dict):
                raise ValidationFailure(f"{label}: invalid embedded author avatar")
            path = asset.get("path")
            if not isinstance(path, str) or not path.startswith("assets/") or path not in archive_names:
                raise ValidationFailure(f"{label}: embedded author avatar is missing")

    for owner in owners:
        for asset_key, uri_key in (("asset", "uri"), ("videoAsset", "videoUri")):
            asset = owner.get(asset_key)
            uri = owner.get(uri_key)
            if isinstance(uri, str) and uri.strip():
                raise ValidationFailure(f"{label}: device-specific {uri_key} is not allowed")
            if asset is None:
                continue
            if not isinstance(asset, dict):
                raise ValidationFailure(f"{label}: invalid embedded asset metadata")
            path = asset.get("path")
            if not isinstance(path, str) or not path.startswith("assets/") or path not in archive_names:
                raise ValidationFailure(f"{label}: embedded asset is missing: {path}")


def validate_package(theme: dict) -> None:
    expected_size = theme["sizeBytes"]
    with tempfile.NamedTemporaryFile(suffix=".kstheme") as package_file:
        digest = hashlib.sha256()
        copied = 0
        with open_remote(theme["downloadUrl"]) as response:
            declared = response.headers.get("Content-Length")
            if declared and int(declared) != expected_size:
                raise ValidationFailure(f"{theme['id']}: remote Content-Length mismatch")
            while True:
                chunk = response.read(64 * 1024)
                if not chunk:
                    break
                copied += len(chunk)
                if copied > MAX_PACKAGE_BYTES or copied > expected_size:
                    raise ValidationFailure(f"{theme['id']}: package exceeds declared size")
                digest.update(chunk)
                package_file.write(chunk)
        package_file.flush()
        if copied != expected_size:
            raise ValidationFailure(f"{theme['id']}: downloaded byte count mismatch")
        if digest.hexdigest().lower() != theme["sha256"].lower():
            raise ValidationFailure(f"{theme['id']}: SHA-256 mismatch")
        package_file.seek(0)
        try:
            with zipfile.ZipFile(package_file) as archive:
                infos = archive.infolist()
                if len(infos) > 64:
                    raise ValidationFailure(f"{theme['id']}: package has too many ZIP entries")
                expanded = 0
                archive_names: set[str] = set()
                for info in infos:
                    entry_name = validate_zip_entry(info.filename)
                    if entry_name in archive_names:
                        raise ValidationFailure(
                            f"{theme['id']}: package contains duplicate entry: {entry_name}"
                        )
                    archive_names.add(entry_name)
                    expanded += info.file_size
                    if expanded > MAX_ZIP_ASSET_BYTES:
                        raise ValidationFailure(f"{theme['id']}: expanded package is too large")
                try:
                    theme_json = archive.read("theme.json")
                except KeyError as error:
                    raise ValidationFailure(f"{theme['id']}: theme.json is missing") from error
                if len(theme_json) > 256 * 1024:
                    raise ValidationFailure(f"{theme['id']}: theme.json is too large")
                metadata = json.loads(theme_json.decode("utf-8"))
                if metadata.get("schema") != PACKAGE_SCHEMA:
                    raise ValidationFailure(f"{theme['id']}: package schema mismatch")
                if metadata.get("version") != theme["packageVersion"]:
                    raise ValidationFailure(f"{theme['id']}: package version mismatch")
                author = metadata.get("author", {})
                if metadata.get("version", 0) >= 4 and (
                    author.get("realName", "").strip()
                    or author.get("gender", "unspecified") != "unspecified"
                ):
                    raise ValidationFailure(
                        f"{theme['id']}: cloud package exposes private author profile fields"
                    )
                validate_embedded_assets(metadata, archive_names, theme["id"])
        except zipfile.BadZipFile as error:
            raise ValidationFailure(f"{theme['id']}: invalid ZIP package") from error


def validate_remote_assets(themes: list[dict], previous: dict | None) -> None:
    previous_by_id = {
        item.get("id"): item
        for item in (previous or {}).get("themes", [])
        if isinstance(item, dict)
    }
    checked_images: set[str] = set()
    for theme in themes:
        old = previous_by_id.get(theme["id"])
        package_unchanged = old and all(
            theme.get(field) == old.get(field)
            for field in ("downloadUrl", "sha256", "sizeBytes", "packageVersion")
        )
        if not package_unchanged:
            print(f"checking package {theme['id']}...", flush=True)
            validate_package(theme)
        for image_url in [theme["coverUrl"], *theme["screenshots"]]:
            if image_url in checked_images:
                continue
            old_images = [] if not old else [old.get("coverUrl"), *old.get("screenshots", [])]
            if image_url not in old_images:
                print(f"checking image {image_url}...", flush=True)
                validate_image(image_url)
            checked_images.add(image_url)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", type=Path)
    parser.add_argument("--schema", type=Path)
    parser.add_argument("--previous", type=Path)
    parser.add_argument("--check-remote", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        catalog = load_json(args.catalog)
        previous = load_json(args.previous) if args.previous and args.previous.is_file() else None
        validate_with_schema(catalog, args.schema)
        themes = validate_semantics(catalog, previous)
        if args.check_remote:
            validate_remote_assets(themes, previous)
        print(
            f"catalog valid: {len(themes)} themes, "
            f"{len(catalog.get('categories', []))} categories"
        )
        return 0
    except (ValidationFailure, json.JSONDecodeError, OSError, ValueError) as error:
        print(f"catalog validation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
