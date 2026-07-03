ApkeSU third-party notices
==========================

This file records direct third-party dependencies and build-time source inputs
used by this repository. It complements LICENSE, NOTICE, and
GPL-COMPLIANCE.md.

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

2. NPM packages
---------------

Source files:

  - js/package.json
  - website/package.json
  - website/yarn.lock

Project/package declarations:

| Area | Package | Version/range | Declared license | Notes |
| --- | --- | --- | --- | --- |
| Module WebUI library | kernelsu | 3.0.2 | Apache-2.0 | No runtime npm dependencies are declared in js/package.json. |
| Website | KernelSU_website | 1.0.0 | MIT | Website package itself. |
| Website dev dependency | vitepress | ^1.6.4 | MIT | Used to build the documentation site. |
| Website dev dependency | vue | ^3.5.38 | MIT | Used by VitePress. |

The committed website/yarn.lock pins transitive npm packages for the
documentation build. Those packages are not vendored in this repository; their
licenses must be taken from the npm package metadata at install/release time.

Suggested release check:

  cd website
  yarn install --frozen-lockfile
  yarn licenses list

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
| com.android.application | 9.2.1 | Apache-2.0 | Android Gradle Plugin |
| org.jetbrains.kotlin.android | 2.4.0 | Apache-2.0 | Kotlin |
| org.jetbrains.kotlin.plugin.compose | 2.4.0 | Apache-2.0 | Kotlin Compose compiler plugin |
| org.jetbrains.kotlin.plugin.serialization | 2.4.0 | Apache-2.0 | Kotlin serialization compiler plugin |
| org.lsposed.lsplugin.apksign | 1.4 | Apache-2.0 | https://github.com/LSPosed/LSPlugin |

Direct Manager dependencies:

| Coordinate | Version | License | Source/project |
| --- | --- | --- | --- |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0 | AndroidX |
| androidx.compose:compose-bom | 2026.05.01 | Apache-2.0 | AndroidX Compose |
| androidx.compose.material:material-icons-extended | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.material3:material3 | 1.5.0-alpha21 | Apache-2.0 | AndroidX Compose Material3 |
| androidx.compose.ui:ui | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-tooling-preview | BOM managed | Apache-2.0 | AndroidX Compose |
| androidx.compose.ui:ui-test-manifest | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.compose.ui:ui-tooling | BOM managed | Apache-2.0 | AndroidX Compose, debug only |
| androidx.lifecycle:lifecycle-runtime-compose | 2.10.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.10.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.10.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.lifecycle:lifecycle-viewmodel-navigation3 | 2.10.0 | Apache-2.0 | AndroidX Lifecycle |
| androidx.navigation3:navigation3-runtime | 1.1.2 | Apache-2.0 | AndroidX Navigation 3 |
| androidx.navigationevent:navigationevent-compose | 1.1.1 | Apache-2.0 | AndroidX Navigation Event |
| androidx.webkit:webkit | 1.16.0 | Apache-2.0 | AndroidX WebKit |
| com.github.topjohnwu.libsu:core | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:service | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| com.github.topjohnwu.libsu:io | 6.0.0 | Apache-2.0 | https://github.com/topjohnwu/libsu |
| dev.rikka.rikkax.parcelablelist:parcelablelist | 2.0.1 | MIT | https://github.com/RikkaApps/RikkaX |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.11.0 | Apache-2.0 | kotlinx.coroutines |
| org.commonmark:commonmark | 0.28.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-tables | 0.28.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-gfm-strikethrough | 0.28.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-autolink | 0.28.0 | BSD-2-Clause | commonmark-java |
| org.commonmark:commonmark-ext-task-list-items | 0.28.0 | BSD-2-Clause | commonmark-java |
| org.lsposed.libcxx:libcxx | 29.0.14206865 | Apache-2.0 / LLVM notices | https://github.com/LSPosed/prefab-libcxx |
| org.lsposed.hiddenapibypass:hiddenapibypass | 6.1 | Apache-2.0 | https://github.com/LSPosed/AndroidHiddenApiBypass |
| top.yukonga.miuix.kmp:miuix-ui-android | 0.9.2 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-icons-android | 0.9.2 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-navigation3-ui-android | 0.9.2 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-preference-android | 0.9.2 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| top.yukonga.miuix.kmp:miuix-blur-android | 0.9.2 | Apache-2.0 | https://github.com/compose-miuix-ui/miuix |
| com.squareup.okhttp3:okhttp-bom | 5.3.2 | Apache-2.0 | https://github.com/square/okhttp |
| com.squareup.okhttp3:okhttp | 5.3.2 | Apache-2.0 | https://github.com/square/okhttp |
| com.materialkolor:material-kolor | 4.1.1 | MIT | https://github.com/jordond/materialkolor |
| me.zhanghai.android.appiconloader:appiconloader | 1.5.0 | Apache-2.0 | https://github.com/zhanghai/AppIconLoader |

Gradle resolves additional transitive dependencies from the repositories above.
For binary releases, generate a dependency report from Gradle and preserve all
licenses/notices from resolved artifacts.

Suggested release check:

  cd manager
  ./gradlew :app:dependencies --configuration releaseRuntimeClasspath

4. LKM and GKI build inputs
---------------------------

Source files:

  - .github/workflows/build-lkm.yml
  - .github/workflows/ddk-lkm.yml
  - .github/workflows/gki-build.yml
  - .github/actions/gki-download-kernel/action.yml
  - .github/actions/gki-setup-build-environment/action.yml
  - .github/actions/gki-susfs/action.yml

Direct external build inputs:

| Input | Version/ref used by repo | License/source note |
| --- | --- | --- |
| ghcr.io/ylarod/ddk-min | `${kmi}-20260313` | Android GKI DDK container used only to compile kernelsu.ko. Kernel headers and generated kernel build files follow the Android common kernel/Linux GPL-2.0-only model; LLVM/Clang and Android build tools keep their own upstream notices. Keep the container/upstream notices with published KO build logs where applicable. |
| Android GKI kernel manifest/tree | `common-${android}-${kernel}-${os_patch_level}` or deprecated fallback | Android common kernel source from android.googlesource.com. The kernel tree follows Linux kernel GPL-2.0-only plus per-file SPDX notices. |
| Google repo tool | downloaded from storage.googleapis.com/git-repo-downloads/repo | Android repo client; keep upstream notices when redistributing tooling. |
| WildKernels/kernel_patches | default branch at workflow runtime | External GKI patch helper repository cloned during workflow. |
| WildKernels/AnyKernel3 | branch `gki-2.0` | AnyKernel3 packaging tree cloned during workflow. |
| simonpunk/susfs4ksu | branch `gki-${version}`, optionally pinned by .github/config/gki-commits.json | SUSFS source and patches cloned during workflow. Preserve its upstream license/notices with SUSFS-enabled kernel artifacts. |
| GitHub Actions official actions | checkout, upload-artifact | Build-service actions used at CI time; not vendored into release artifacts. |

The repository does not vendor the full Android GKI kernel tree, DDK container,
AnyKernel3 tree, kernel_patches tree, or SUSFS repository. They are fetched by
CI at build time and remain governed by their own upstream licenses.

When publishing a KO, boot image, or AnyKernel3 package, include:

  - the exact ApkeSU source revision;
  - the exact KMI and DDK release;
  - the Android GKI branch or manifest used;
  - any external patch repository commit used;
  - the generated binary artifact and enough build instructions to reproduce it.
