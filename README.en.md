<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="ApkeSU project avatar">
  <h1>ApkeSU</h1>
  <p>Kernel-based root manager for GKI Android devices</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <strong>English</strong> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p>
    <a href="#project-overview">Project overview</a> ·
    <a href="#upstream-project">Upstream</a> ·
    <a href="#open-source-compliance">Licensing</a> ·
    <a href="#disclaimer">Disclaimer</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">QQ group</a>
  </p>
</div>

---

ApkeSU is a derivative open-source project based on the official [KernelSU](https://github.com/tiann/KernelSU) upstream repository and [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra). It is a kernel-based root manager for GKI Android devices, with a focus on KMI matching, reliable SuSFS configuration, recoverable maintenance, the KernelSU Manager experience, UI extensions, LKM patching, and personal device debugging.

## Project overview

This project inherits KernelSU's licensing structure: the `kernel/` directory is licensed under **GPL-2.0-only**, in line with upstream KernelSU and the Linux kernel; KernelSU-derived code outside `kernel/` is licensed under **GPL-3.0-or-later**. Third-party dependencies remain under their respective upstream licenses. See [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) for details.

## Upstream project

Primary upstream project: KernelSU

Upstream repository: https://github.com/tiann/KernelSU

This project periodically incorporates upstream source updates, security patches, and feature improvements.

## Open-source compliance

1. All modifications and derived source code remain open source. Complete corresponding source code is provided alongside every distributed build, including kernels, APKs, and modules.
2. Anyone may obtain, modify, and redistribute this project under the applicable license terms. Redistributions must preserve upstream attribution, license notices, and complete corresponding source code.
3. If you publicly distribute modifications to this project, you must also publish the complete modified source code and identify the upstream source.

> Important: This project extends upstream functionality while retaining KernelSU's native underlying permission model.
> The MIUI-style theme is an independent visual implementation and does not use Xiaomi's official source code.

## Intended use

This tool is intended only for local technical research on Android devices, learning, and debugging devices you own. Do not use it to unlawfully alter application permissions, bypass risk controls, gain unauthorized access, or conduct other illegal activity.

## Disclaimer

1. This project is intended solely for Android low-level technology education and open-source technical exchange. All tools and source code are provided only for lawful personal research. The developers accept no responsibility for repair, compensation, or after-sales support for boot failures, reboot loops, bricked devices, hardware damage, or any other device failure caused by flashing a ROM or kernel or installing project-related files. You assume all such risks.<br>

2. Financial, online gaming, enterprise, and government applications commonly employ root-environment and kernel-privilege risk controls. You assume all risks of account suspension, device blocking, feature restrictions, or financial loss arising from use of this tool. The project developers do not assist with account appeals or bypassing risk controls.<br>

3. You must not use this project's source code or derivative tools for unauthorized permission changes, application cracking, data theft, malicious bundling, cheat development, or any other violation of applicable cybersecurity, copyright, or other laws. The person committing such misuse bears all civil, administrative, and criminal liability; the project developers are not responsible.<br>

4. This project is distributed free of charge only through legitimate open-source communities and offers no official paid sales or customization services. Paid packages or modified builds offered by third-party platforms or individuals are unrelated to this project, and their security and integrity cannot be guaranteed. You are responsible for risks such as account theft, privacy leakage, or malware caused by third-party builds.<br>

5. By downloading, compiling, or flashing files from this project, you confirm that you have read, understood, and accepted this disclaimer in full. If you do not accept it, delete the source code and files immediately and stop using the project.<br>

## Acknowledgements

- [KernelSU](https://github.com/tiann/KernelSU), with thanks to author weishu and all contributors
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), reference for the SuSFS solution
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), referenced UI framework code
- [kowsu](https://github.com/KOWX712/KernelSU.git), technical support
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/), inspiration for KernelSU's design
- [Magisk](https://github.com/topjohnwu/Magisk), a well-known open-source root solution
- [genuine](https://github.com/brevent/genuine/), APK signature verification
- [Diamorphine](https://github.com/m0nad/Diamorphine), reference for low-level hiding techniques
