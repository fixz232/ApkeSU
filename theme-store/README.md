# ApkeSU Cloud Theme Store

The cloud store is a signed-by-hash catalog hosted in this repository. Theme packages are kept in GitHub Releases so large binary files do not inflate Git history.

## Trust model

- The Manager downloads only HTTPS resources hosted by GitHub or GitHub's content CDN.
- Every catalog entry must declare the exact package byte count and SHA-256.
- The Manager checks the final redirect host, byte count, SHA-256, ZIP paths, package schema, package version, embedded assets, and Manager compatibility before import.
- Applying a cloud theme first saves the current theme to the local library. A failed apply automatically restores that backup.
- No GitHub token or publishing credential is embedded in the APK.
- Creator approval is read from `creators/v1/creators.json`; local Manager settings never grant publishing rights.
- GitHub Actions rechecks the Issue author, creator registry, manifest, package hash, byte count, ZIP structure, compatibility range, and media after approval.

SHA-256 proves that the downloaded package matches the reviewed catalog. It does not replace catalog review. Only maintainers may merge catalog changes or run the publishing workflow.

## Creator access

1. Open **Theme center > My > Creator Center** and enter the GitHub account that will submit themes.
2. Create a creator application Issue from the **Access** page.
3. The repository owner `fixz232` reviews the application and adds the `creator-approved` label.
4. The moderation workflow verifies that the label was applied by `fixz232`, writes the Issue author to `creators/v1/creators.json`, and closes the application.
5. Refresh Creator Center. Submission tools unlock only when the downloaded registry contains that GitHub account.

Run **Moderate cloud theme creators and submissions** once with `workflow_dispatch` to create the moderation labels in a new repository. A creator approval grants no repository write permission and does not bypass per-theme review.

## Publishing a theme as an approved creator

1. In Creator Center, use **Create cloud-safe package from current theme**, or select any local file up to 500 MiB. The picker does not restrict the original filename, extension, or MIME type, but the bytes must contain a valid cloud-safe ApkeSU theme package. The Manager copies and validates one immutable snapshot, then computes its exact byte count and SHA-256. Device storage and GitHub Release limits still apply.
2. Export the validated snapshot as a standard `.kstheme` file and upload those exact bytes to a public GitHub Release under the approved creator's GitHub account. Paste the `.kstheme` Release URL into Creator Center. The app and moderation workflow reject package URLs owned by another account.
3. Add screenshots when available, plus the license, compatibility range, version, and an existing or custom category. The cover is optional: the first screenshot is used when present, otherwise ApkeSU supplies its default cover.
4. Run remote verification. The Manager downloads the Release asset and requires its byte count and SHA-256 to match the selected local package.
5. Submit the generated GitHub Issue. The Issue author must be the approved creator recorded in the manifest.
6. `fixz232` reviews the visible metadata and adds `theme-approved` only when publication is allowed.
7. GitHub Actions repeats all security checks, creates the canonical Release, adds a custom category when needed, updates the catalog, marks the Issue `theme-published`, and closes it.

The app intentionally does not upload directly to the central repository: doing so would require embedding a GitHub write token or an OAuth service. Creator-owned Releases keep publishing credentials outside the APK while still providing a complete reviewed upload path.

The ordinary local export remains suitable for backup and device-to-device transfer, but it may preserve local `content://` references and private author fields for that purpose. Do not upload that file directly. The publishing workflow rejects device-specific URIs, a non-empty author real name, a non-`unspecified` gender, or any configured media that was not embedded in the cloud-safe package.

## Maintainer publishing

1. Export a `.kstheme` package from **Theme center > Import and export**.
2. Prepare a 16:9 cover and up to eight screenshots. Each image or video asset may be up to 500 MiB.
3. Upload the package to a temporary GitHub Release or another GitHub-hosted URL.
4. Ask a maintainer to run **Publish cloud theme** with the temporary URL, exact size, SHA-256, and package metadata. The workflow verifies the bytes and creates the canonical release asset.
5. Add or update the entry in `catalog/v1/catalog.json` using that canonical URL. Increase `versionCode` whenever the package bytes, URL, hash, size, or version name changes.
6. Open a pull request. The catalog workflow validates the schema, URLs, compatibility range, remote media, package size, SHA-256, ZIP paths, and embedded `theme.json`.

The canonical release tag is `theme-<theme-id>-v<versionCode>`. The catalog `downloadUrl` must point to an asset under that tag.

## Public author data

Cloud entries contain only an explicit public author ID, display name, optional profile/avatar URLs, and an optional bio. Do not publish a real name, gender, private avatar, API token, device identifier, or other personal data. Local author profile fields inside the Manager are not uploaded automatically. The publishing validator rejects packages whose embedded `realName` is non-empty or whose embedded gender is not `unspecified`.

## Catalog fields

- `id`: stable lowercase theme identifier.
- `versionCode`: monotonically increasing package revision.
- `packageSchema` / `packageVersion`: must match the exported `.kstheme` package.
- `minManagerVersionCode` / `maxManagerVersionCode`: supported Manager range; use `null` for no upper limit.
- `sha256` / `sizeBytes`: exact release asset identity.
- `license`: SPDX license identifier for the theme assets.
- `status`: `published` or `deprecated`.
- `featured`: curated placement flag.
- `downloadCount`: maintained by the scheduled statistics workflow.

Run the local validator before submitting:

```bash
python theme-store/scripts/validate_catalog.py \
  theme-store/catalog/v1/catalog.json \
  --schema theme-store/schemas/catalog-v1.schema.json
```

Add `--check-remote` after the package and image URLs are publicly reachable.

Validate creator moderation changes with:

```bash
python -m unittest discover -s theme-store/tests -p "test_*.py" -v
```
