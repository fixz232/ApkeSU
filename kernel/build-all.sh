#!/bin/bash
set -e

if ! command -v ddk >/dev/null 2>&1 && [ -x /opt/ddk/scripts/ddk ]; then
    export PATH="/opt/ddk/scripts:$PATH"
fi
if ! command -v ddk >/dev/null 2>&1; then
    echo "ddk command not found" >&2
    exit 127
fi

if [ -z "$1" ]; then
    KMIS="android12-5.10 android13-5.10 android13-5.15 android14-5.15 android14-6.1 android15-6.6 android16-6.12 android17-6.18"
else
    KMIS=$1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ASSET_DIR="$ROOT_DIR/userspace/ksud/bin/aarch64"
if [ -z "${KSU_VERSION_OVERRIDE:-}" ] && [ -f "$ROOT_DIR/version.properties" ]; then
    KSU_VERSION_OVERRIDE="$(sed -n 's/^versionCode=//p' "$ROOT_DIR/version.properties" | head -n 1)"
fi
export KSU_VERSION_OVERRIDE
echo "Using KSU_VERSION_OVERRIDE=${KSU_VERSION_OVERRIDE:-unset}"

# Some patch is required to use separate build dir when building for android16-6.12, see:
# https://github.com/5ec1cff/ddk#local-%E6%A8%A1%E5%BC%8F%E6%9E%84%E5%BB%BA%E9%80%82%E7%94%A8%E4%BA%8E%E5%A4%9A%E4%B8%AA-target-%E7%89%88%E6%9C%AC%E7%9A%84%E5%86%85%E6%A0%B8%E6%A8%A1%E5%9D%97

mv .ddk-version .ddk-version.bak 2> /dev/null || true
failures=0

for kmi in $KMIS; do
    echo "========== Building $kmi =========="
    ODIR="$(realpath .)/out/$kmi"
    DDK_ODIR="$ODIR"
    # Docker-mode DDK must mount the repository root so kernel sources can
    # include the shared root-level uapi headers.
    if [ "$(cat "${HOME}/.ddk/mode" 2>/dev/null || true)" = "docker" ]; then
        DDK_ODIR="/build/kernel/out/$kmi"
    fi
    if [ "$kmi" = "android16-6.12" ]; then
        # Linux 6.12's external-module Kbuild resolves source prerequisites
        # below M=, so stage source files into its isolated output tree.
        rm -rf "$ODIR"
        mkdir -p "$ODIR"
        while IFS= read -r -d '' source_file; do
            target="$ODIR/$source_file"
            mkdir -p "$(dirname "$target")"
            cp "$source_file" "$target"
        done < <(find . -type f \( -name '*.c' -o -name '*.h' -o -name '*.S' -o -name '*.rs' \) -not -path './out/*' -print0)
    fi
    if [ "$(cat "${HOME}/.ddk/mode" 2>/dev/null || true)" = "docker" ]; then
        if (cd "$ROOT_DIR" && ddk build "$kmi" -e CONFIG_KSU=m -- -C kernel "ODIR=$DDK_ODIR"); then
            build_result=0
        else
            build_result=$?
        fi
    else
        if ddk build "$kmi" "ODIR=$DDK_ODIR" -e CONFIG_KSU=m; then
            build_result=0
        else
            build_result=$?
        fi
    fi
    if [ "$build_result" -eq 0 ]; then
        if [ -f "$ODIR/kernelsu.ko" ]; then
            cp "$ODIR/kernelsu.ko" "kernelsu-${kmi}.ko"
            llvm-strip -d "kernelsu-${kmi}.ko"
            mkdir -p "$ASSET_DIR"
            cp "kernelsu-${kmi}.ko" "$ASSET_DIR/${kmi}_kernelsu.ko"
            echo "✓ Built kernelsu-${kmi}.ko"
        fi
    else
        echo "✗ Build failed for $kmi"
        failures=$((failures + 1))
    fi
    echo ""
done

mv .ddk-version.bak .ddk-version 2> /dev/null || true

echo "========== Final output =========="
ls -l kernelsu-*.ko

if [ "$failures" -ne 0 ]; then
    echo "$failures KMI build(s) failed" >&2
    exit 1
fi
