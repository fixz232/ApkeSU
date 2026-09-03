# ApkeSU Dynamic Manager

## 中文说明

动态管理器在保留 ApkeSU 内置主管理器的同时，为一个已安装的兼容应用提供副管理器权限。副管理器拥有完整的 Root 管理能力，因此该功能只适合你信任并审计过的管理器 APK。

### 安全模型

- 内置 ApkeSU 主管理器不会被动态配置替换，也不会因撤销副管理器而失效。
- 同一时间只保存一个副管理器槽位。
- 身份同时绑定精确包名、标准化 App ID、APK v2 签名证书大小和证书 SHA-256。
- 配置后以及 Android 软件包列表变化后，内核都会重新查找 `/data/app/.../base.apk` 并校验身份与证书。
- 更新期间若 `/data/app` 同时存在多个同包名 APK，所有匹配 APK 都必须通过证书校验；目录扫描不完整时会保持未激活。
- 共享 App ID 会被拒绝，避免同 UID 的其他软件包继承管理器权限。
- 配置以 schema v1 原子写入 `/data/adb/ksu/.dynamic_manager.json`，权限为 `0600`。
- 软件包卸载、App ID 改变、签名改变或校验失败时，副管理器立即处于未激活状态。

动态管理器不是应用沙箱。获得管理器权限的应用可以请求 Root，并能以 Root 身份修改系统和动态管理器配置。授权确认是信任边界，不能把未知或来源不明的 APK 加入其中。

### 使用条件

1. 当前运行的 ApkeSU 内核和 ksud 必须包含动态管理器 UAPI；旧内核会显示“不支持”。
2. 候选管理器必须作为普通用户应用安装在 `/data/app`，并包含可用的 `libksud.so`。
3. APK 必须包含唯一的 v2 signer，且不能包含 v3/v3.1 签名块，这与当前内核管理器签名校验约束一致。
4. 候选包不能使用共享 App ID，也不能是 ApkeSU 内置包或其已知变体。

在 ApkeSU 中打开 `设置 -> 主页与管理器 -> 动态管理器`，选择候选应用并确认完整权限警告。页面显示“已启用并通过验证”后，启动副管理器验证状态。更新 APK 后若签名或身份发生变化，需返回该页面重新确认绑定。

### 撤销与恢复

在动态管理器页面点击“撤销”会清除内核运行时槽位，并保存禁用状态。即使副管理器无法启动，内置 ApkeSU 主管理器仍然可用，可由它完成撤销。高级诊断可使用：

```sh
ksud kernel dynamic-manager status
ksud kernel dynamic-manager set-apk /data/app/.../base.apk --package com.example.manager --appid 10123
ksud kernel dynamic-manager clear
```

不要手工填写证书哈希。`set-apk` 会读取已安装 APK、核对 `packages.list`、解析证书，并让内核再次独立扫描和验证。

## English

Dynamic Manager keeps the built-in ApkeSU primary Manager available and grants one installed compatible application secondary Manager authority. The secondary application receives full root-management capability, so only an APK you trust and have audited should be selected.

The binding includes the exact package name, normalized App ID, APK v2 signer certificate size, and certificate SHA-256. The kernel revalidates the installed `/data/app/.../base.apk` after configuration and Android package-list changes. If several matching APK directories coexist during an update, every match must pass certificate verification; incomplete directory scans remain inactive. Shared App IDs, multiple v2 signers, v3/v3.1 signing blocks, identity changes, and certificate changes are rejected. Persistent schema-v1 state is atomically stored at `/data/adb/ksu/.dynamic_manager.json` with mode `0600`.

Open `Settings -> Home & Manager -> Dynamic Manager`, choose a compatible application, and accept the full-authority warning. Revoking the slot does not affect the built-in ApkeSU Manager. This design was adapted from the ReSukiSU Dynamic Manager approach; exact source and license attribution is recorded in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).
