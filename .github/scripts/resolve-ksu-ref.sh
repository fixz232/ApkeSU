#!/usr/bin/env bash
set -euo pipefail

# Resolve the KernelSU/ApkeSU source ref used by older GKI workflows.
# The old workflow could accidentally pass README text or a full GitHub URL as
# the branch name, which produced broken URLs such as
# /branches/#Apkhttps://github.com/....

raw_ref="${1:-${KSU_REF:-${KERNELSU_REF:-${GITHUB_REF_NAME:-}}}}"
repo="${KSU_REPO:-${KERNELSU_REPO:-${GITHUB_REPOSITORY:-fixz232/ApkeSU}}}"

raw_ref="$(printf '%s' "$raw_ref" | tr -d '\r' | head -n 1 | xargs || true)"

github_repo_from_text() {
  printf '%s' "$1" | sed -nE 's#.*github\.com/([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+).*#\1#p' | head -n 1
}

if extracted_repo="$(github_repo_from_text "$raw_ref")"; [ -n "$extracted_repo" ]; then
  repo="$extracted_repo"
fi

repo="${repo%%https*}"
repo="${repo%%http*}"
repo="${repo%%#*}"
repo="${repo%%\?*}"

if [[ "$raw_ref" == refs/heads/* ]]; then
  raw_ref="${raw_ref#refs/heads/}"
fi

if [[ -z "$raw_ref" || "$raw_ref" == \#* || "$raw_ref" == *"://"* || "$raw_ref" == *"["* || "$raw_ref" == *"]"* || "$raw_ref" =~ [[:space:]] ]]; then
  raw_ref="${GITHUB_REF_NAME:-}"
fi

if [[ -z "$raw_ref" || "$raw_ref" == \#* || "$raw_ref" == *"://"* || "$raw_ref" =~ [[:space:]] ]]; then
  raw_ref="ApkeSU"
fi

repo="${repo#https://github.com/}"
repo="${repo#http://github.com/}"
repo="${repo%.git}"

if [[ ! "$repo" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]; then
  echo "Invalid KernelSU repository: $repo" >&2
  exit 2
fi

ref="$raw_ref"
repo_url="https://github.com/${repo}.git"

if ! git ls-remote --exit-code --heads "$repo_url" "$ref" >/dev/null 2>&1; then
  fallback_ref=""
  for candidate in "${GITHUB_REF_NAME:-}" ApkeSU main master; do
    [ -n "$candidate" ] || continue
    if git ls-remote --exit-code --heads "$repo_url" "$candidate" >/dev/null 2>&1; then
      fallback_ref="$candidate"
      break
    fi
  done

  if [ -z "$fallback_ref" ]; then
    echo "KernelSU branch '$ref' was not found in $repo" >&2
    exit 3
  fi

  echo "KernelSU branch '$ref' was not found in $repo; using '$fallback_ref' instead." >&2
  ref="$fallback_ref"
fi

echo "Resolved KernelSU repository: $repo"
echo "Resolved KernelSU ref: $ref"

{
  echo "ksu_repo=$repo"
  echo "ksu_ref=$ref"
  echo "ksu_source_url=https://github.com/${repo}/tree/${ref}"
} >> "${GITHUB_OUTPUT:-/dev/null}"

{
  echo "KSU_REPO=$repo"
  echo "KSU_REF=$ref"
  echo "KSU_SOURCE_URL=https://github.com/${repo}/tree/${ref}"
} >> "${GITHUB_ENV:-/dev/null}"
