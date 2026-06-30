# ApkeSU OnePlus/Realme GKI Build Repository

This repository can build ApkeSU GKI AnyKernel3 packages for OnePlus, OPPO/OPlus,
and Realme devices that match the GKI families used by these reference
repositories:

- SM8650: <https://github.com/cctv18/oppo_oplus_realme_sm8650>
- SM8750: <https://github.com/cctv18/oppo_oplus_realme_sm8750>
- SM8850: <https://github.com/cctv18/oppo_oplus_realme_sm8850>
- MT6989/MT6897: <https://github.com/cctv18/oppo_oplus_realme_sm8650>
- MT6991: <https://github.com/cctv18/oppo_oplus_realme_sm8750>
- MT6993: <https://github.com/cctv18/oppo_oplus_realme_sm8850>

The dedicated workflow is `.github/workflows/gki-oneplus-realme.yml`.

## Targets

| Target | GKI branch | Reference sublevel | Config |
| --- | --- | --- | --- |
| `sm8650` | `android14-6.1` | `6.1.118` | `.github/config/gki-oneplus-realme-sm8650.json` |
| `sm8750` | `android15-6.6` | `6.6.89` | `.github/config/gki-oneplus-realme-sm8750.json` |
| `sm8850` | `android16-6.12` | `6.12.23` | `.github/config/gki-oneplus-realme-sm8850.json` |
| `mt6989-mt6897` | `android14-6.1` | `6.1.118` | `.github/config/gki-oneplus-realme-mt6989-mt6897.json` |
| `mt6991` | `android15-6.6` | `6.6.89` | `.github/config/gki-oneplus-realme-mt6991.json` |
| `mt6993` | `android16-6.12` | `6.12.23` | `.github/config/gki-oneplus-realme-mt6993.json` |

## Build

1. Push this repository to GitHub.
2. Open `Actions`.
3. Run `Build OnePlus/Realme GKI`.
4. Choose `target_chip`.
5. Choose `ApkeSU` or `ApkeSU+SUSFS`.
6. Download the uploaded `AnyKernel3` artifact after the workflow completes.

The default patch level is `latest`. For the reference versions above, the
per-target configs each contain one known-good patch level:

- `sm8650`: `2025-01`
- `sm8750`: `2025-06`
- `sm8850`: `2025-06`
- `mt6989-mt6897`: `2025-01`
- `mt6991`: `2025-06`
- `mt6993`: `2025-06`

Use `All` only when you want to build all three chip families in one workflow
run. Each output artifact name includes `OnePlus-Realme-SM8650`,
`OnePlus-Realme-SM8750`, `OnePlus-Realme-SM8850`,
`OnePlus-Realme-MT6989-MT6897`, `OnePlus-Realme-MT6991`, or
`OnePlus-Realme-MT6993`.

## Notes

This workflow intentionally reuses the existing ApkeSU GKI pipeline instead of
copying the extra tuning patches from the reference repositories. That keeps the
first ApkeSU build path focused on root integration, SUSFS, kernel branding, and
AnyKernel3 packaging. Add optional device tuning patches later only after one
plain ApkeSU build boots on the target device.

The MTK entries are GKI builds with matching chip-family labels and GKI patch
levels. They do not copy the reference repositories' OKI device-source archive
workflow or extra device tuning patches.
