#!/usr/bin/env python3
"""Download and verify one package before publishing a canonical GitHub Release."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

from validate_catalog import (
    MAX_PACKAGE_BYTES,
    MAX_ZIP_ASSET_BYTES,
    PACKAGE_SCHEMA,
    ValidationFailure,
    open_remote,
    validate_embedded_assets,
    validate_url,
    validate_zip_entry,
)

THEME_ID_RE = re.compile(r"^[a-z0-9][a-z0-9._-]{1,79}$")
ASSET_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,119}\.kstheme$")
SHA_RE = re.compile(r"^[a-fA-F0-9]{64}$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--theme-id", required=True)
    parser.add_argument("--version-code", required=True, type=int)
    parser.add_argument("--package-version", required=True, type=int)
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--asset-name", required=True)
    parser.add_argument("--sha256", required=True)
    parser.add_argument("--size-bytes", required=True, type=int)
    parser.add_argument("--output-dir", required=True, type=Path)
    return parser.parse_args()


def verify_local_package(path: Path, package_version: int) -> None:
    try:
        with zipfile.ZipFile(path) as archive:
            infos = archive.infolist()
            if len(infos) > 80:
                raise ValidationFailure("package has too many ZIP entries")
            expanded = 0
            archive_names: set[str] = set()
            for info in infos:
                entry_name = validate_zip_entry(info.filename)
                if entry_name in archive_names:
                    raise ValidationFailure(f"package contains duplicate entry: {entry_name}")
                archive_names.add(entry_name)
                expanded += info.file_size
                if expanded > MAX_ZIP_ASSET_BYTES:
                    raise ValidationFailure("expanded package is too large")
            try:
                theme_json = archive.read("theme.json")
            except KeyError as error:
                raise ValidationFailure("theme.json is missing") from error
            if len(theme_json) > 256 * 1024:
                raise ValidationFailure("theme.json is too large")
            metadata = json.loads(theme_json.decode("utf-8"))
            if metadata.get("schema") != PACKAGE_SCHEMA:
                raise ValidationFailure("package schema mismatch")
            if metadata.get("version") != package_version:
                raise ValidationFailure("package version mismatch")
            author = metadata.get("author", {})
            if metadata.get("version", 0) >= 4 and (
                author.get("realName", "").strip()
                or author.get("gender", "unspecified") != "unspecified"
            ):
                raise ValidationFailure("cloud package exposes private author profile fields")
            validate_embedded_assets(metadata, archive_names, "package", archive)
    except zipfile.BadZipFile as error:
        raise ValidationFailure("invalid ZIP package") from error


def download_and_verify_package(
    *,
    theme_id: str,
    version_code: int,
    package_version: int,
    source_url: str,
    asset_name: str,
    sha256: str,
    size_bytes: int,
    output_dir: Path,
) -> Path:
    if not THEME_ID_RE.fullmatch(theme_id):
        raise ValidationFailure("invalid theme id")
    if version_code < 1 or not 1 <= package_version <= 4:
        raise ValidationFailure("invalid version")
    if not ASSET_RE.fullmatch(asset_name):
        raise ValidationFailure("invalid release asset name")
    if not SHA_RE.fullmatch(sha256):
        raise ValidationFailure("invalid SHA-256")
    if not 1 <= size_bytes <= MAX_PACKAGE_BYTES:
        raise ValidationFailure("invalid package size")

    validated_source_url = validate_url(source_url, "source URL", package=True)
    output_dir.mkdir(parents=True, exist_ok=True)
    output = output_dir / asset_name
    digest = hashlib.sha256()
    copied = 0
    with open_remote(validated_source_url) as response, output.open("wb") as handle:
        while True:
            chunk = response.read(64 * 1024)
            if not chunk:
                break
            copied += len(chunk)
            if copied > size_bytes or copied > MAX_PACKAGE_BYTES:
                raise ValidationFailure("package exceeds declared size")
            digest.update(chunk)
            handle.write(chunk)
    if copied != size_bytes:
        raise ValidationFailure("downloaded byte count mismatch")
    if digest.hexdigest().lower() != sha256.lower():
        raise ValidationFailure("SHA-256 mismatch")
    verify_local_package(output, package_version)
    return output


def main() -> int:
    args = parse_args()
    try:
        output = download_and_verify_package(
            theme_id=args.theme_id,
            version_code=args.version_code,
            package_version=args.package_version,
            source_url=args.source_url,
            asset_name=args.asset_name,
            sha256=args.sha256,
            size_bytes=args.size_bytes,
            output_dir=args.output_dir,
        )
        print(output)
        return 0
    except (ValidationFailure, OSError, ValueError, json.JSONDecodeError) as error:
        print(f"release preparation failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
