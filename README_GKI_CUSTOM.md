# ABK-style ApkeSU GKI build

This repository now has ABK-style ApkeSU GKI build workflows:

- `.github/workflows/gki-abk-main.yml`: full matrix entry point.
- `.github/workflows/gki-custom.yml`: custom single-target entry point.
- `.github/workflows/gki-abk-prepare.yml`: matrix expansion layer.
- `.github/workflows/gki-abk-build.yml`: reusable build pipeline.

The custom workflow accepts the same kind of target inputs used by ABK custom
builds:

- `android_version`
- `kernel_version`
- `sublevel`
- `os_patch_level`

Before building, the workflow validates the requested pair against the checked-in
`.github/config/gki-*.json` files. If the pair is not listed there, the workflow
fails before downloading or patching any kernel source. This keeps custom builds
on the same supported target matrix as the rest of this repository.

Before building, the workflows validate the requested pair against the
checked-in `.github/config/gki-*.json` files. If the pair is not listed there,
the workflow fails before downloading or patching any kernel source.

## Usage: Custom Target

1. Push this repository to GitHub.
2. Open `Actions`.
3. Run `Build Custom ABK Style ApkeSU GKI`.
4. Choose the Android version, kernel version, sublevel, and patch level.
5. Choose `ApkeSU` or `ApkeSU+SUSFS`.
6. Download the uploaded AnyKernel3 artifact after the workflow completes.

`os_patch_level` may be a concrete value such as `2025-06`, `lts`, or `latest`.
When `latest` is used, the workflow resolves it to the last non-`lts` entry in
the matching `.github/config/gki-*.json` file and ignores the `sublevel` input.

## Usage: Full Matrix

Run `Build ABK Style ApkeSU GKI` and choose one kernel line or `All`. The
workflow expands `latest`, `all`, or a concrete patch level through the existing
`.github/config/gki-*.json` target matrix.

## ABK Features

The ApkeSU port keeps the ABK-style large pipeline shape:

- build summary
- version pair validation
- disk cleanup and swap setup
- repo/archive GKI source fetch
- ApkeSU injection
- optional SUSFS
- ABK-style ZRAM config bundle
- optional NTsync config
- optional BBR/IPSet networking configs
- config-only virtualization switch
- optional OPlus 6.6 compat/zram profile
- normal and optional bypass kernel builds
- patch reject upload
- AnyKernel3 zip plus ABK-style bundle zip

These ABK switches are exposed but intentionally fail until ApkeSU-native
implementations exist: `BBG`, `DDK LSM`, `KPM`, `Re-Kernel`, `supp_op`, and
custom external modules. They depend on ABK/SukiSU-specific patch sources and
should not silently pretend to work in an ApkeSU build.
