ApkeSU GPL compliance notes
===========================

This file documents the source-distribution decisions for this repository.

1. KernelSU attribution
-----------------------

ApkeSU is based on KernelSU:

  https://github.com/tiann/KernelSU

The upstream copyright notice is preserved in NOTICE. Upstream KernelSU
history and source files remain available in this repository.

2. KernelSU-related source availability
---------------------------------------

All KernelSU-derived manager, userspace, module-management, boot-patching,
LKM-patching, rescue-protection, and UI changes present in ApkeSU are stored
as source code in this repository.

License scope:

  - kernel/ keeps the upstream KernelSU/Linux-kernel GPL-2.0-only licensing
    model unless an individual file states otherwise.
  - non-kernel KernelSU-derived code is distributed under GPL-3.0-or-later
    unless an individual file states otherwise.

3. service-6.0.0-patched.aar
----------------------------

The previously checked-in binary file:

  manager/local-libs/service-6.0.0-patched.aar

has been removed from the repository.

Reason:

  The repository did not contain the complete corresponding source code for
  this patched AAR. To avoid distributing an opaque patched binary, the manager
  now depends on the public libsu service artifact instead:

  com.github.topjohnwu.libsu:service:6.0.0

libsu source project:

  https://github.com/topjohnwu/libsu

4. Magica implementation
------------------------

The Magica feature is implemented from source in this repository. The relevant
source files are:

  userspace/ksud/src/magica.rs
  manager/app/src/main/java/me/weishu/kernelsu/magica/AppZygotePreload.java
  manager/app/src/main/java/me/weishu/kernelsu/magica/BootCompletedReceiver.java
  manager/app/src/main/java/me/weishu/kernelsu/magica/MagicaService.java
  manager/app/src/main/cpp/ksu.cc

Implementation summary:

  - The manager starts an isolated app zygote service.
  - AppZygotePreload loads the manager native library and asks it to exec the
    bundled ksud binary.
  - ksud's magica path temporarily enables local adb root, connects to local
    adbd, and executes late-load with --post-magica.
  - late-load continues inside ksud source code and restores adb properties
    after the post-magica flow.

No separate closed-source Magica binary is distributed by this repository.

5. Third-party dependencies and build inputs
--------------------------------------------

Direct npm packages, Gradle/Maven artifacts, and LKM build-time source
inputs are documented in:

  THIRD_PARTY_NOTICES.md

That file is the release-audit entry point for dependencies that are not part
of KernelSU/ApkeSU source itself.
