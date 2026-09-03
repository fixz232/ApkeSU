ApkeSU third-party notices
==========================

This file records direct third-party dependencies and build-time source inputs
used by this repository. It complements LICENSE, NOTICE, and
GPL-COMPLIANCE.md.

ApkeSU is an open-source derivative of
[KernelSU](https://github.com/tiann/KernelSU), with changes focused on the
Manager experience, interface extensions, LKM patching, and personal-device
debugging. The project periodically incorporates upstream source updates,
security fixes, and feature work from KernelSU.

Keep the upstream license text and notices with any redistributed binary
artifact. For release audits, also verify transitive dependency licenses from
the committed lock files and published package metadata.

1. Project license split
------------------------

ApkeSU inherits KernelSU's license split:

  - kernel/ is GPL-2.0-only unless an individual file says otherwise.
  - non-kernel KernelSU-derived code is GPL-3.0-or-later unless an individual
    file says otherwise.
  - third-party dependencies remain under their own licenses.

Redistributions must preserve the upstream origin, applicable license texts,
and third-party notices. Modified public distributions, including released
kernels, APKs, and modules, must provide the complete corresponding source as
required by the applicable license.

2. NPM packages
---------------

Source files:

  - js/package.json

Project/package declarations:

| Area | Package | Version/range | Declared license | Notes |
| --- | --- | --- | --- | --- |
| Module WebUI library | kernelsu | 3.0.2 | Apache-2.0 | No runtime npm dependencies are declared in js/package.json. |

3. Gradle and Maven dependencies
--------------------------------

Source files:

  - manager/settings.gradle.kts
  - manager/gradle/libs.versions.toml
  - manager/app/build.gradle.kts

Repositories used by Gradle:

  - google()
  - mavenCentral()
  - https://jitpack.io

Direct Gradle plugins:

| Coordinate/plugin | Version | License | Source/project |
| --- | --- | --- | --- |
| com.android.application | 9.3.0 | Apache-2.0 | Android Gradle Plugin |
| org.jetbrains.kotlin.android | 2.4.10 | Apache-2.0 | Kotlin |
| org.jetbrains.kotlin.plugin.compose | 2.4.10 | Apache-2.0 | Kotlin Compose compiler plugin |
| org.jetbrains.kotlin.plugin.serialization | 2.4.10 | Apache-2.0 | Kotlin serialization compiler plugin |
| org.lsposed.lsplugin.apksign | 1.4 | Apache-2.0 | https://github.com/LSPosed/LSPlugin |

Direct Manager dependencies:

| Coordinate | Version | License | Source/project |
| --- | --- | --- | --- |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0 | AndroidX |
| androidx.compose:compose-bom | 2026.06.01 | Apache-2.0 | AndroidX Compose |
| androidx.compose.material:material-icons-extended | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.material3:material3 | 1.5.0-alpha24 | Apache-2.0 | AndroidX Compose Material3 |
| androidx.compose.ui:ui | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-tooling-preview | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-test-manifest | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.compose.ui:ui-tooling | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.lifecycle:lifecycle-runtime-compose | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-navigation3 | 2.11.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.navigation3:navigation3-runtime | 1.1.4 | Apache-2.0 | AndroidX Navigation 3 |
| androidx.navigationevent:navigationevent-compose | 1.1.2 | Apache-2.0 | AndroidX Navigation Event |
| androidx.webkit:webkit | 1.16.0 | Apache-2.0 | AndroidX WebKit |
| com.github.topjohnwu.libsu:core | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:service | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:io | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| dev.rikka.rikkax.parcelablelist:parcelablelist | 2.0.1 | MIT | https://github.com/RikkaApps/RikkaX |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.11.0 | Apache-2.0 | kotlinx.coroutines |
| org.commonmark:commonmark | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-tables | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-strikethrough | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-autolink | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-task-list-items | 0.29.0 | BSD-2-Clause | commonmark-java |
| org.lsposed.libcxx:libcxx | 29.0.14206865 | Apache-2.0 / LLVM notices | https://github.com/LSPosed/prefab-libcxx |
| org.lsposed.hiddenapibypass:hiddenapibypass | 6.1 | Apache-2.0 | https://github.com/LSPosed/AndroidHiddenApiBypass |
| top.yukonga.miuix.kmp:miuix-ui-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-icons-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-navigation3-ui-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-preference-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-blur-android | 0.9.3 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| com.squareup.okhttp3:okhttp-bom | 5.4.0 | Apache-2.0 | https://github.com/square/okhttp |
| com.squareup.okhttp3:okhttp | 5.4.0 | Apache-2.0 | https://github.com/square/okhttp |
| com.materialkolor:material-kolor | 5.0.0 | MIT | https://github.com/jordond/materialkolor |
| me.zhanghai.android.appiconloader:appiconloader | 1.5.0 | Apache-2.0 | https://github.com/zhanghai/AppIconLoader |

Gradle resolves additional transitive dependencies from the repositories above.
For binary releases, generate a dependency report from Gradle and preserve all
licenses/notices from resolved artifacts.

Suggested release check:

  cd manager
  ./gradlew :app:dependencies --configuration releaseRuntimeClasspath

4. LKM build inputs
-------------------

Source files:

  - .github/workflows/build-lkm.yml
  - .github/workflows/ddk-lkm.yml

Direct external build inputs:

| Input | Version/ref used by repo | License/source note |
| --- | --- | --- |
| ghcr.io/ylarod/ddk-min | `${kmi}-20260313` | Android DDK container used only to compile kernelsu.ko. Kernel headers and generated kernel build files follow the Android common kernel/Linux GPL-2.0-only model; LLVM/Clang and Android build tools keep their own upstream notices. Keep the container/upstream notices with published KO build logs where applicable. |
| `userspace/ksud/bin/aarch64/*_kernelsu.ko` | [KOWX712/KernelSU](https://github.com/KOWX712/KernelSU), `master` at `60e7841b5ce1d0dc5aafd4d4898cd5dccacb270f` (2026-07-21) | Precompiled KernelSU LKM source reference. The exact build commit is not embedded in the `.ko` metadata and must be replaced with the actual build ref when known. |
| GitHub Actions official actions | checkout, upload-artifact | Build-service actions used at CI time; not vendored into release artifacts. |

The repository does not vendor the DDK container. It is fetched by CI at build
time and remains governed by its upstream licenses.

The KOWX712 KernelSU source uses GPL-2.0-only for `kernel/` (see
`kernel/LICENSE`) and GPL-3.0-or-later for the remaining project code (see
`LICENSE`). Redistribution of the referenced `.ko` files is permitted under
those applicable GPL terms, provided that the corresponding license texts,
copyright notices, and complete corresponding source (or the GPL-required
source offer) are preserved with the redistributed artifact.

The `.ko` ELF metadata reports `license=GPL`, but does not contain a Git
commit. The commit above is the audited upstream `master` reference, not a
claim that it is the exact build commit; record the exact build ref before
publishing a release when it is available.

When publishing a KO package, include:

  - the exact ApkeSU source revision;
  - the exact KMI and DDK release;
  - the generated binary artifact and enough build instructions to reproduce it.

5. ReSukiSU Dynamic Manager reference
-------------------------------------

The ApkeSU Dynamic Manager implementation was designed with reference to the
Dynamic Manager feature, IOCTL command allocation, and loading approach in
[ReSukiSU/ReSukiSU](https://github.com/ReSukiSU/ReSukiSU), audited at commit
[`0b5efe9e0102c43ca5c41174d500f5a7080cd0c7`](https://github.com/ReSukiSU/ReSukiSU/commit/0b5efe9e0102c43ca5c41174d500f5a7080cd0c7)
(2026-08-31).

Relevant upstream areas include:

  - `kernel/feature/dynamic_manager.c` and `.h`;
  - `userspace/ksud/src/android/dynamic_manager.rs`;
  - the Manager Dynamic Manager repository, model, use cases, ViewModel, and UI.

ReSukiSU applies GPL-2.0-only to its `kernel/` directory and GPL-3.0-or-later
to the remaining project code. ApkeSU retains the same applicable license split
for this adaptation. The ApkeSU implementation is not a verbatim import: it
keeps a fixed built-in primary Manager, exposes one secondary slot, binds the
slot to package name and normalized App ID in addition to the APK v2
certificate, rejects shared App IDs, persists a versioned root-only
configuration atomically, and omits manual certificate entry from the Manager
UI. Its command structure and semantics are ApkeSU-specific and do not claim
binary compatibility with ReSukiSU. See `docs/DYNAMIC_MANAGER.md` for the
security contract and limitations.
