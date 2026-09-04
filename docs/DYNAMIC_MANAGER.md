# ApkeSU Dynamic Manager

## 中文说明

动态管理器在保留 ApkeSU 内置主管理器的同时，让一个 APK v2 签名证书成为动态管理器身份。匹配该证书的兼容应用拥有完整 Root 管理能力，因此只应配置你信任并审计过的证书。

### 安全模型

- 内置 ApkeSU 主管理器不会被动态配置替换，也不会因撤销副管理器而失效。
- 同一时间只保存一个动态证书槽位，签名索引固定为 `255`。
- 身份由 APK v2 签名证书大小和证书 SHA-256 决定，不绑定包名或 App ID；使用同一证书签名的兼容 APK 都属于同一信任边界。
- 配置后以及 Android 软件包列表变化后，内核都会重新扫描 `/data/app/.../base.apk`，并把匹配证书的应用登记到 RCU 管理器注册表。
- 内置 ApkeSU 仅接受包名 `io.github.fixz.apkesu` 及发行证书；动态签名不导入 Vivo 或其他固定包名变体。
- 只接受唯一 APK v2 signer；v1、v3/v3.1、ZIP64、多 signer 和损坏的 ZIP 中央目录均会被拒绝。
- 配置按 ReSukiSU 的 `{size, hash}` 格式原子写入 `/data/adb/ksu/.dynamic_manager`，权限为 `0600`。旧 schema-v1 `.dynamic_manager.json` 会迁移证书字段，但不迁移旧包名绑定。
- 软件包卸载、App ID 改变、签名改变或校验失败时，对应应用会从运行时注册表移除；目录扫描不完整时保留上一次完整结果，避免瞬间清空主管理器。

动态管理器不是应用沙箱。获得管理器权限的应用可以请求 Root，并能以 Root 身份修改系统和动态管理器配置。授权确认是信任边界，不能把未知或来源不明的 APK 加入其中。

### 使用条件

1. 当前运行的 ApkeSU 内核和 ksud 必须包含动态管理器 UAPI；旧内核会显示“不支持”。
2. 候选管理器必须作为普通用户应用安装在 `/data/app`，并包含可用的 `libksud.so`。
3. APK 必须包含唯一的 v2 signer，且不能同时包含 v1、v3/v3.1 签名。
4. 候选应用不能是内置 ApkeSU 包；同证书签名的所有兼容应用都会获得动态管理器身份。

在 ApkeSU 中打开 `设置 -> 主页与管理器 -> 动态管理器`，选择候选应用并确认完整权限警告。也可手动输入证书大小和小写 SHA-256。页面显示“已启用并通过验证”后，再启动副管理器验证状态。

### 撤销与恢复

在动态管理器页面点击“撤销”会清除内核运行时槽位，并保存禁用状态。即使副管理器无法启动，内置 ApkeSU 主管理器仍然可用，可由它完成撤销。高级诊断可使用：

```sh
ksud kernel dynamic-manager status
ksud kernel dynamic-manager set-apk /data/app/.../base.apk
ksud kernel dynamic-manager set 744 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
ksud kernel dynamic-manager clear
```

优先使用 `set-apk` 从 APK 解析证书；手动配置只适用于已独立核对证书指纹的高级场景。内核会再次扫描和验证已安装 APK。

## English

Dynamic Manager keeps the built-in ApkeSU primary Manager available and recognizes one configured APK v2 signing certificate as a secondary Manager identity. Every compatible installed APK signed by that certificate receives full root-management capability.

The identity consists of the certificate size and SHA-256 and deliberately does not bind a package name or App ID, matching the ReSukiSU ABI and loading model. The kernel rescans `/data/app/.../base.apk` after configuration and package-list changes and publishes matching App IDs through an RCU registry. The built-in Manager remains restricted to `io.github.fixz.apkesu`; no Vivo package variant is accepted. Multiple v2 signers, v1/v2 mixtures, v3/v3.1, ZIP64, and malformed central directories are rejected. State is atomically stored at `/data/adb/ksu/.dynamic_manager` with mode `0600`; the old ApkeSU schema-v1 file is migrated to certificate-only state.

Open `Settings -> Home & Manager -> Dynamic Manager`, choose a compatible application or enter a verified certificate manually, and accept the full-authority warning. Revoking the slot does not affect the built-in ApkeSU Manager. Source and license attribution is recorded in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).
