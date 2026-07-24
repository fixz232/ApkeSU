<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="ApkeSU 项目头像">
  <h1>ApkeSU</h1>
  <p>面向 GKI Android 设备的内核 Root 管理器</p>
  <p>
    <a href="#项目说明">项目说明</a> ·
    <a href="#上游项目信息">上游信息</a> ·
    <a href="#开源协议遵守说明">开源协议</a> ·
    <a href="#免责声明">免责声明</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">QQ 群</a>
  </p>
</div>

---

ApkeSU 是基于 [KernelSU](https://github.com/tiann/KernelSU) 官方上游仓库进行二次修改的衍生开源项目和 [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) 面向 GKI Android 设备的内核 Root 管理器，关注 KMI 匹配、SuSFS 配置可靠性和可恢复维护进行二次修改的衍生开源项目。主要面向 KernelSU 管理器体验、界面扩展、LKM 修补与个人设备调试场景。

## 项目说明

本项目继承 KernelSU 的开源授权结构：`kernel/` 目录遵循上游 KernelSU/Linux kernel 的 **GPL-2.0-only** 授权；除 `kernel/` 之外的 KernelSU 衍生代码遵循 **GPL-3.0-or-later** 授权；第三方依赖遵循各自上游许可证，清单见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。

## 上游项目信息

上游主项目：KernelSU

上游仓库地址：https://github.com/tiann/KernelSU

本项目会不定期同步上游官方源码更新、安全补丁与功能迭代。

## 开源协议遵守说明

1. 本项目所有修改、编译后的衍生代码全程开源，所有对外发布的编译成品（内核、APK、模块），同步提供完整修改源码；
2. 任何人可以在对应许可证条款下自由获取、修改、分发本项目代码；二次分发时必须保留上游来源、许可证和完整对应源码；
3. 若你对本项目代码进行二次修改并公开发布，同样需要公开你的全部修改源码，并标注上游来源。
> 重要声明：本项目仅在上游基础上扩展功能，底层权限模型保持KernelSU原生实现；
> MIUI风格主题为独立视觉实现，未使用小米官方源码。
## 项目用途

本工具仅用于安卓设备本地技术研究、个人设备调试学习，请勿用于违规篡改应用权限、绕过风控、恶意入侵等非法场景。

## 免责声明

1. 本项目仅面向安卓底层技术学习、开源技术交流用途，所有工具、源码仅供个人合法研究参考。因自行刷机、刷入内核、安装本项目相关文件所引发的设备卡开机、无限重启、系统变砖、硬件损坏等一切设备故障，开发者不承担任何维修、赔偿、售后相关责任，相关风险由使用者自行全部承担。<br>
​
2. 金融、网络游戏、企业办公、政务类应用普遍搭载Root环境、内核权限风控检测机制。若使用本工具后出现账号封禁、设备拉黑、功能限制、资产损失等后果，全部使用风险由使用者自行承担，项目开发者不提供任何账号申诉、风控解除相关协助。<br>
​
3. 严禁将本项目源码、衍生工具用于未经设备所有者授权的权限篡改、APP逆向破解、数据窃取、恶意程序捆绑、作弊外挂开发等违反《网络安全法》《著作权法》及其他现行法律法规的行为。一旦发生违规使用，全部民事、行政、刑事责任由实际使用者独立承担，与项目开发者无关。<br>
​
4. 本项目仅在正规开源社区免费公开分发，全程无任何官方付费售卖、定制服务。任何第三方平台、个人有偿售卖的安装包、修改版工具均与本项目无关，其安全性、完整性无法保障，下载使用第三方修改包产生的盗号、隐私泄露、设备中毒等风险请使用者自行甄别承担。<br>
​
5. 使用者下载、编译、刷入本项目相关文件，即代表完整阅读、理解并同意以上全部免责条款，若不认可本声明内容，请立即删除相关源码与文件，停止一切使用行为。<br>

## 致谢

- [KernelSU](https://github.com/tiann/KernelSU)，感谢作者weishu与全部贡献者
- [Sukisu-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) SuSFS方案参考
- [FolkPatch](https://github.com/LyraVoid/FolkPatch) UI框架代码引用
- [kowsu](https://github.com/KOWX712/KernelSU.git) 技术支持
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): KernelSU设计灵感来源
- [Magisk](https://github.com/topjohnwu/Magisk): 知名Root开源项目
- [genuine](https://github.com/brevent/genuine/): APK签名校验方案
- [Diamorphine](https://github.com/m0nad/Diamorphine): 底层隐藏技术参考
