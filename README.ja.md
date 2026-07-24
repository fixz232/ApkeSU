<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="ApkeSU プロジェクトアバター">
  <h1>ApkeSU</h1>
  <p>GKI Android デバイス向けのカーネルベース Root マネージャー</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <strong>日本語</strong> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p>
    <a href="#プロジェクト概要">プロジェクト概要</a> ·
    <a href="#アップストリームプロジェクト">アップストリーム</a> ·
    <a href="#オープンソースライセンスの遵守">ライセンス</a> ·
    <a href="#免責事項">免責事項</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">QQ グループ</a>
  </p>
</div>

---

ApkeSU は、公式アップストリームの [KernelSU](https://github.com/tiann/KernelSU) リポジトリと [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra) を基にした派生オープンソースプロジェクトです。GKI Android デバイス向けのカーネルベース Root マネージャーとして、KMI の適合、SuSFS 設定の信頼性、復旧可能なメンテナンス、KernelSU Manager の操作性、UI 拡張、LKM パッチ、および個人所有デバイスのデバッグに重点を置いています。

## プロジェクト概要

本プロジェクトは KernelSU のライセンス構成を継承しています。`kernel/` ディレクトリは、アップストリームの KernelSU および Linux カーネルに従い **GPL-2.0-only** で提供されます。`kernel/` 以外の KernelSU 派生コードは **GPL-3.0-or-later** で提供されます。サードパーティ依存関係には、それぞれのアップストリームライセンスが適用されます。詳細は [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) を参照してください。

## アップストリームプロジェクト

主要アップストリームプロジェクト：KernelSU

アップストリームリポジトリ：https://github.com/tiann/KernelSU

本プロジェクトは、アップストリームのソース更新、セキュリティパッチ、機能改善を随時取り込みます。

## オープンソースライセンスの遵守

1. すべての変更および派生コードはオープンソースとして公開されます。カーネル、APK、モジュールを含むすべての配布ビルドに、完全な対応ソースコードを添付します。
2. 適用されるライセンス条項に従い、誰でも本プロジェクトを取得、変更、再配布できます。再配布時には、アップストリームの出典、ライセンス表示、完全な対応ソースコードを保持する必要があります。
3. 本プロジェクトの変更版を公開配布する場合は、変更後の完全なソースコードも公開し、アップストリームの出典を明記する必要があります。

> 重要：本プロジェクトはアップストリームの機能を拡張しますが、基盤となる権限モデルは KernelSU のネイティブ実装を維持します。
> MIUI スタイルのテーマは独立した視覚実装であり、Xiaomi の公式ソースコードは使用していません。

## 利用目的

本ツールは、Android デバイスに関するローカルな技術研究、学習、および本人が所有するデバイスのデバッグのみを目的としています。アプリ権限の違法な変更、リスク管理の回避、不正アクセス、その他の違法行為には使用しないでください。

## 免責事項

1. 本プロジェクトは、Android の低レベル技術の学習およびオープンソース技術交流のみを目的としています。すべてのツールとソースコードは、合法的な個人研究のためだけに提供されます。ROM やカーネルの書き込み、または本プロジェクト関連ファイルのインストールによって発生した起動不能、再起動ループ、端末の文鎮化、ハードウェア損傷、その他の故障について、開発者は修理、補償、アフターサポートの責任を負いません。すべてのリスクは利用者が負担します。<br>

2. 金融、オンラインゲーム、企業、行政機関向けアプリでは、Root 環境やカーネル権限に関するリスク管理が一般的に使用されています。本ツールの使用によるアカウント停止、デバイスのブロック、機能制限、資産損失などのリスクは、すべて利用者が負担します。開発者は、アカウントの異議申し立てやリスク管理の回避を支援しません。<br>

3. デバイス所有者の許可なく権限を変更する行為、アプリの不正解析、データ窃取、マルウェアの同梱、チート開発、またはサイバーセキュリティ法、著作権法、その他の適用法令に違反する行為に、本プロジェクトのソースコードや派生ツールを使用することを禁じます。違反者は、民事上、行政上、刑事上のすべての責任を単独で負い、プロジェクト開発者は責任を負いません。<br>

4. 本プロジェクトは、正規のオープンソースコミュニティを通じてのみ無料で配布され、公式の有料販売やカスタマイズサービスは提供していません。第三者のプラットフォームや個人が提供する有料パッケージまたは変更版は本プロジェクトと無関係であり、安全性と完全性は保証されません。第三者版によるアカウント盗難、プライバシー漏えい、マルウェア感染などのリスクは利用者が負担します。<br>

5. 本プロジェクトのファイルをダウンロード、ビルド、または書き込むことにより、本免責事項をすべて読み、理解し、同意したものとみなされます。同意しない場合は、ソースコードとファイルを直ちに削除し、使用を中止してください。<br>

## 謝辞

- [KernelSU](https://github.com/tiann/KernelSU)：作者 weishu およびすべてのコントリビューター
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)：SuSFS ソリューションの参考
- [FolkPatch](https://github.com/LyraVoid/FolkPatch)：UI フレームワークコードの参考
- [kowsu](https://github.com/KOWX712/KernelSU.git)：技術サポート
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)：KernelSU 設計の着想元
- [Magisk](https://github.com/topjohnwu/Magisk)：著名なオープンソース Root ソリューション
- [genuine](https://github.com/brevent/genuine/)：APK 署名検証
- [Diamorphine](https://github.com/m0nad/Diamorphine)：低レベル隠蔽技術の参考
