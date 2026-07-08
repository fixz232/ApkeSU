# ApkeSU release APK

Use `.github/workflows/release-apk.yml` to build an ABK-style release APK for
this repository.

The workflow builds the release manager APK, injects fresh `ksud` binaries for
`arm64-v8a` and `x86_64`, zipaligns the APK with 16 KB page alignment, signs it,
and uploads:

- `ApkeSU_<versionName>_<versionCode>-release.apk`
- `ApkeSU_<versionName>_<versionCode>-release.apk.sha256`
- `manager_identity.mk`
- `RELEASE_APK_MANIFEST.json`

## Signing

For a real release, configure these repository secrets:

- `KEYSTORE`
- `KEY_ALIAS`
- `KEYSTORE_PASSWORD`
- `KEY_PASSWORD`

`KEYSTORE` must be the base64-encoded keystore file. The resulting
`manager_identity.mk` records the package name, certificate size, and
certificate hash that kernel builds must trust.

The workflow has an `allow_ephemeral_key` input for test builds. APKs signed
with that temporary key are installable, but kernels built for the normal release
certificate will not trust them as Manager.

## Usage

1. Push a tag matching `v*`, or open `Actions`.
2. Run `Release ApkeSU APK`.
3. Keep `create_github_release` enabled to publish the APK to GitHub Releases.
4. Download the `apkesu-release-apk` artifact or the created release assets.
