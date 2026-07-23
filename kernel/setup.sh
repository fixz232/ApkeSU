#!/bin/sh
set -eu

GKI_ROOT=$(pwd)
APKESU_REPO=${APKESU_REPO:-https://github.com/fixz232/ApkeSU.git}
APKESU_SOURCE_DIR=${APKESU_SOURCE_DIR:-$GKI_ROOT/KernelSU}
APKESU_MANAGER_PACKAGE=io.github.fixz.apkesu
APKESU_EXPECTED_SIZE=0x02e8
APKESU_EXPECTED_HASH=1c89980c03432844cfe195dab90bfaecbcd987d19309da648014164be78007d1
KSU_MAKE_ENTRY="obj-\$(CONFIG_KSU) += kernelsu/"
KSU_MAKE_ENTRY_PATTERN="obj-\\\$(CONFIG_KSU) += kernelsu/"

display_usage() {
    echo "Usage: $0 [--cleanup | <commit-or-tag-or-branch>]"
    echo "  --cleanup:                   Revert integration changes made by this script."
    echo "  <commit-or-tag-or-branch>:   Integrate a specific ApkeSU revision."
    echo "  -h, --help:                  Display this help."
    echo "  (no args):                   Integrate the default branch of the ApkeSU repository."
    echo
    echo "Environment overrides:"
    echo "  APKESU_REPO                  ApkeSU Git repository URL."
    echo "  APKESU_SOURCE_DIR            Clone directory (default: <kernel tree>/KernelSU)."
}

fail() {
    echo "[ERROR] $*" >&2
    exit 1
}

initialize_variables() {
    if test -d "$GKI_ROOT/common/drivers"; then
        DRIVER_DIR="$GKI_ROOT/common/drivers"
    elif test -d "$GKI_ROOT/drivers"; then
        DRIVER_DIR="$GKI_ROOT/drivers"
    else
        fail '"drivers/" directory not found. Run this script from the kernel tree root.'
    fi

    DRIVER_MAKEFILE=$DRIVER_DIR/Makefile
    DRIVER_KCONFIG=$DRIVER_DIR/Kconfig
}

read_make_value() {
    key=$1
    file=$2
    sed -n "s/^[[:space:]]*${key}[[:space:]]*:=[[:space:]]*//p" "$file" | head -n 1
}

verify_apkesu_source() {
    identity_file=$APKESU_SOURCE_DIR/dist/manager_identity.mk
    version_file=$APKESU_SOURCE_DIR/version.properties
    kbuild_file=$APKESU_SOURCE_DIR/kernel/Kbuild

    test -f "$identity_file" || fail "Missing $identity_file; refusing to integrate a non-ApkeSU source tree."
    test -f "$version_file" || fail "Missing $version_file; the kernel version would fall back to a Git commit count."
    test -f "$kbuild_file" || fail "Missing $kbuild_file."

    manager_package=$(read_make_value KSU_MANAGER_PACKAGE "$identity_file")
    expected_size=$(read_make_value KSU_EXPECTED_SIZE "$identity_file")
    expected_hash=$(read_make_value KSU_EXPECTED_HASH "$identity_file")
    version_code=$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*//p' "$version_file" | head -n 1)

    test "$manager_package" = "$APKESU_MANAGER_PACKAGE" ||
        fail "Unexpected Manager package '$manager_package' (expected $APKESU_MANAGER_PACKAGE)."
    case "$expected_size" in
        0x[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]) ;;
        *) fail "Invalid ApkeSU Manager certificate size '$expected_size'." ;;
    esac
    case "$expected_hash" in
        *[!0-9a-f]*|'') fail "Invalid ApkeSU Manager certificate SHA-256 '$expected_hash'." ;;
    esac
    test "${#expected_hash}" -eq 64 || fail "ApkeSU Manager certificate SHA-256 must contain 64 hex characters."
    test "$expected_size" = "$APKESU_EXPECTED_SIZE" ||
        fail "Unexpected Manager certificate size '$expected_size' (expected $APKESU_EXPECTED_SIZE)."
    test "$expected_hash" = "$APKESU_EXPECTED_HASH" ||
        fail "Unexpected Manager certificate SHA-256 '$expected_hash'."
    case "$version_code" in
        *[!0-9]*|'') fail "Invalid ApkeSU versionCode '$version_code'." ;;
    esac

    grep -Fq "KSU_MANAGER_PACKAGE := $manager_package" "$kbuild_file" ||
        fail "kernel/Kbuild Manager package fallback does not match manager_identity.mk."
    grep -Fq "KSU_EXPECTED_SIZE := $expected_size" "$kbuild_file" ||
        fail "kernel/Kbuild certificate size fallback does not match manager_identity.mk."
    grep -Fq "KSU_EXPECTED_HASH := $expected_hash" "$kbuild_file" ||
        fail "kernel/Kbuild certificate hash fallback does not match manager_identity.mk."

    echo "[+] Verified ApkeSU source identity"
    echo "    version: $version_code"
    echo "    package: $manager_package"
    echo "    certificate size: $expected_size"
    echo "    certificate sha256: $expected_hash"
}

checkout_apkesu_source() {
    if test ! -d "$APKESU_SOURCE_DIR/.git"; then
        test ! -e "$APKESU_SOURCE_DIR" ||
            fail "$APKESU_SOURCE_DIR exists but is not a Git repository."
        echo "[+] Cloning ApkeSU from $APKESU_REPO"
        git clone "$APKESU_REPO" "$APKESU_SOURCE_DIR"
        : > "$APKESU_SOURCE_DIR/.git/apkesu-setup-owned"
    fi

    cd "$APKESU_SOURCE_DIR"
    if test -n "$(git status --porcelain --untracked-files=no)"; then
        fail "$APKESU_SOURCE_DIR has tracked local changes; commit or restore them before updating."
    fi

    current_repo=$(git remote get-url origin 2>/dev/null || true)
    if test "$current_repo" != "$APKESU_REPO"; then
        echo "[+] Replacing source remote $current_repo with $APKESU_REPO"
        git remote set-url origin "$APKESU_REPO"
    fi
    git fetch --tags --prune origin
    git remote set-head origin --auto >/dev/null
    requested_ref=${1-}
    if test -z "$requested_ref"; then
        remote_head=$(git symbolic-ref -q --short refs/remotes/origin/HEAD || true)
        test -n "$remote_head" || fail "Unable to resolve the ApkeSU default branch."
        requested_ref=${remote_head#origin/}
    fi

    if git show-ref --verify --quiet "refs/remotes/origin/$requested_ref"; then
        if git show-ref --verify --quiet "refs/heads/$requested_ref"; then
            git checkout "$requested_ref"
        else
            git checkout --track -b "$requested_ref" "origin/$requested_ref"
        fi
        git merge --ff-only "origin/$requested_ref"
    elif git rev-parse --verify --quiet "$requested_ref^{commit}" >/dev/null; then
        git checkout --detach "$requested_ref"
    else
        fail "Unknown ApkeSU branch, tag, or commit: $requested_ref"
    fi

    echo "[+] Using ApkeSU revision $(git rev-parse --short=12 HEAD) ($requested_ref)"
    verify_apkesu_source
}

perform_cleanup() {
    echo "[+] Cleaning up ApkeSU integration..."
    if test -L "$DRIVER_DIR/kernelsu"; then
        rm "$DRIVER_DIR/kernelsu"
        echo "[-] Driver symlink removed."
    fi
    if grep -Fq "$KSU_MAKE_ENTRY" "$DRIVER_MAKEFILE"; then
        sed -i "\|$KSU_MAKE_ENTRY_PATTERN|d" "$DRIVER_MAKEFILE"
        echo "[-] Makefile entry removed."
    fi
    if grep -Fq 'source "drivers/kernelsu/Kconfig"' "$DRIVER_KCONFIG"; then
        sed -i '\|source "drivers/kernelsu/Kconfig"|d' "$DRIVER_KCONFIG"
        echo "[-] Kconfig entry removed."
    fi

    if test -f "$APKESU_SOURCE_DIR/.git/apkesu-setup-owned"; then
        source_real=$(realpath "$APKESU_SOURCE_DIR")
        root_real=$(realpath "$GKI_ROOT")
        case "$source_real" in
            "$root_real"/*)
                rm -rf "$source_real"
                echo "[-] Script-managed ApkeSU clone removed."
                ;;
            *)
                fail "Refusing to remove source outside the kernel tree: $source_real"
                ;;
        esac
    fi
}

setup_apkesu() {
    echo "[+] Setting up ApkeSU..."
    checkout_apkesu_source "${1-}"

    cd "$DRIVER_DIR"
    ln -sfn "$(realpath --relative-to="$DRIVER_DIR" "$APKESU_SOURCE_DIR/kernel")" kernelsu
    echo "[+] Driver symlink points to $APKESU_SOURCE_DIR/kernel"

    if ! grep -Fq "$KSU_MAKE_ENTRY" "$DRIVER_MAKEFILE"; then
        printf '\n%s\n' "$KSU_MAKE_ENTRY" >> "$DRIVER_MAKEFILE"
        echo "[+] Makefile entry added."
    fi
    if ! grep -Fq 'source "drivers/kernelsu/Kconfig"' "$DRIVER_KCONFIG"; then
        printf '\nsource "drivers/kernelsu/Kconfig"\n' >> "$DRIVER_KCONFIG"
        echo "[+] Kconfig entry added."
    fi
    echo '[+] ApkeSU integration complete.'
}

case "${1-}" in
    -h|--help)
        display_usage
        ;;
    --cleanup)
        initialize_variables
        perform_cleanup
        ;;
    *)
        test "$#" -le 1 || fail "Only one branch, tag, or commit may be specified."
        initialize_variables
        setup_apkesu "${1-}"
        ;;
esac
