# Reporting Security Issues

Security bugs that affect ApkeSU-specific code should be reported privately through the GitHub Security Advisory [Report a vulnerability](https://github.com/fixz232/ApkeSU/security/advisories/new) form. Do not publish working exploits, signing material, device identifiers, or private logs in a public issue.

Dynamic Manager reports are security-sensitive because the feature grants full root-management authority. Include the ApkeSU source revision, kernel and ksud versions, Android version, whether the secondary Manager was active, and sanitized reproduction steps. Do not attach the selected APK if you do not have permission to redistribute it.

For a vulnerability that reproduces unchanged on the official KernelSU upstream, use the [KernelSU security advisory](https://github.com/tiann/KernelSU/security/advisories/new) process as well. Ordinary crashes and UI defects that do not expose a security boundary may be filed through the public ApkeSU issue templates.
