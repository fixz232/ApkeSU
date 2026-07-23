#!/usr/bin/env python3
"""Fail closed when an ApkeSU kernel build uses the wrong app identity or version."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


EXPECTED_PACKAGE = "io.github.fixz.apkesu"
EXPECTED_CERT_SIZE = "0x02e8"
EXPECTED_CERT_SHA256 = "1c89980c03432844cfe195dab90bfaecbcd987d19309da648014164be78007d1"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
SIZE_RE = re.compile(r"^0x[0-9a-fA-F]{4}$")


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def parse_make_assignments(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    assignment = re.compile(r"^([A-Za-z0-9_]+)\s*(?::|\?)?=\s*(.*?)\s*$")
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        match = assignment.match(raw_line.strip())
        if match:
            values[match.group(1)] = match.group(2)
    return values


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def verify_build_log(path: Path, version: str, identity: dict[str, str]) -> None:
    log = path.read_text(encoding="utf-8", errors="replace")
    required_lines = (
        f"-- KernelSU version: {version} from KSU_VERSION_OVERRIDE",
        f"-- KernelSU Manager package name: {identity['KSU_MANAGER_PACKAGE']}",
        f"-- KernelSU Manager signature size: {identity['KSU_EXPECTED_SIZE']}",
        f"-- KernelSU Manager signature hash: {identity['KSU_EXPECTED_HASH']}",
    )
    for line in required_lines:
        require(line in log, f"build log did not confirm expected identity: {line}")


def verify_module(path: Path, identity: dict[str, str]) -> None:
    data = path.read_bytes()
    for key in ("KSU_MANAGER_PACKAGE", "KSU_EXPECTED_HASH"):
        value = identity[key].encode("ascii")
        require(value in data, f"{path} does not contain {key}={identity[key]}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--build-log", type=Path)
    parser.add_argument("--module", type=Path)
    args = parser.parse_args()

    root = args.root.resolve()
    identity_path = root / "dist" / "manager_identity.mk"
    version_path = root / "version.properties"
    kbuild_path = root / "kernel" / "Kbuild"
    app_build_path = root / "manager" / "app" / "build.gradle.kts"

    identity = parse_make_assignments(identity_path)
    version = parse_properties(version_path)
    kbuild = parse_make_assignments(kbuild_path)
    app_build = app_build_path.read_text(encoding="utf-8")

    for key in ("KSU_MANAGER_PACKAGE", "KSU_EXPECTED_SIZE", "KSU_EXPECTED_HASH"):
        require(identity.get(key), f"{identity_path} is missing {key}")
        require(kbuild.get(key) == identity[key], f"kernel/Kbuild fallback {key} is out of sync")

    package = identity["KSU_MANAGER_PACKAGE"]
    cert_size = identity["KSU_EXPECTED_SIZE"]
    cert_hash = identity["KSU_EXPECTED_HASH"]
    version_code = version.get("versionCode", "")

    require(package == EXPECTED_PACKAGE, f"unexpected ApkeSU package: {package}")
    require(SIZE_RE.fullmatch(cert_size) is not None, f"invalid certificate size: {cert_size}")
    require(SHA256_RE.fullmatch(cert_hash) is not None, f"invalid certificate SHA-256: {cert_hash}")
    require(cert_size == EXPECTED_CERT_SIZE, f"unexpected ApkeSU certificate size: {cert_size}")
    require(cert_hash == EXPECTED_CERT_SHA256, f"unexpected ApkeSU certificate SHA-256: {cert_hash}")
    require(version_code.isdigit() and int(version_code) > 0, f"invalid versionCode: {version_code}")
    require(
        re.search(rf'applicationId\s*=\s*"{re.escape(package)}"', app_build) is not None,
        "Manager applicationId does not match kernel manager identity",
    )

    if args.build_log:
        verify_build_log(args.build_log, version_code, identity)
    if args.module:
        verify_module(args.module, identity)

    print(
        "verified ApkeSU kernel identity: "
        f"version={version_code} package={package} size={cert_size} sha256={cert_hash}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
