<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="Avatar du projet ApkeSU">
  <h1>ApkeSU</h1>
  <p>Gestionnaire root basé sur le noyau pour les appareils Android GKI</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <strong>Français</strong> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <a href="README.es.md">Español</a>
  </p>
  <p>
    <a href="#présentation-du-projet">Présentation</a> ·
    <a href="#projet-en-amont">Projet en amont</a> ·
    <a href="#respect-des-licences-libres">Licences</a> ·
    <a href="#avertissement">Avertissement</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">Groupe QQ</a>
  </p>
</div>

---

ApkeSU est un projet open source dérivé du dépôt officiel [KernelSU](https://github.com/tiann/KernelSU) et de [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra). Il s'agit d'un gestionnaire root basé sur le noyau pour les appareils Android GKI, axé sur la correspondance KMI, la fiabilité de la configuration SuSFS, la maintenance récupérable, l'expérience du gestionnaire KernelSU, les extensions d'interface, les correctifs LKM et le débogage d'appareils personnels.

## Présentation du projet

Ce projet reprend la structure de licences de KernelSU : le répertoire `kernel/` est publié sous licence **GPL-2.0-only**, conformément à KernelSU en amont et au noyau Linux ; le code dérivé de KernelSU situé hors de `kernel/` est publié sous licence **GPL-3.0-or-later**. Les dépendances tierces restent soumises à leurs licences d'origine. Consultez [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) pour plus de détails.

## Projet en amont

Projet principal en amont : KernelSU

Dépôt en amont : https://github.com/tiann/KernelSU

Ce projet intègre périodiquement les mises à jour du code source, les correctifs de sécurité et les évolutions fonctionnelles du projet en amont.

## Respect des licences libres

1. Toutes les modifications et tout le code dérivé restent open source. Le code source correspondant complet est fourni avec chaque version distribuée, notamment les noyaux, APK et modules.
2. Toute personne peut obtenir, modifier et redistribuer ce projet selon les licences applicables. Toute redistribution doit conserver l'attribution au projet en amont, les mentions de licence et le code source correspondant complet.
3. Si vous distribuez publiquement une version modifiée de ce projet, vous devez également publier l'intégralité du code source modifié et indiquer sa provenance.

> Important : ce projet étend les fonctionnalités du projet en amont tout en conservant le modèle d'autorisations natif de KernelSU.
> Le thème de style MIUI est une réalisation visuelle indépendante qui n'utilise pas le code source officiel de Xiaomi.

## Utilisation prévue

Cet outil est exclusivement destiné à la recherche technique locale sur Android, à l'apprentissage et au débogage d'appareils vous appartenant. Ne l'utilisez pas pour modifier illégalement les autorisations d'applications, contourner des contrôles de risque, obtenir un accès non autorisé ou mener toute autre activité illégale.

## Avertissement

1. Ce projet est uniquement destiné à l'apprentissage des technologies Android de bas niveau et aux échanges techniques open source. Tous les outils et codes sources sont fournis pour des recherches personnelles licites. Les développeurs déclinent toute responsabilité de réparation, d'indemnisation ou d'assistance après-vente en cas d'échec de démarrage, de redémarrages en boucle, d'appareil inutilisable, de dommages matériels ou de toute autre panne provoquée par le flashage d'une ROM ou d'un noyau, ou par l'installation de fichiers liés au projet. Vous assumez l'intégralité de ces risques.<br>

2. Les applications financières, de jeu en ligne, professionnelles et administratives utilisent couramment des contrôles de risque liés à l'environnement root et aux privilèges du noyau. Vous assumez tous les risques de suspension de compte, de blocage d'appareil, de restriction de fonctionnalités ou de perte financière résultant de l'utilisation de cet outil. Les développeurs ne fournissent aucune aide pour les recours de compte ou le contournement des contrôles de risque.<br>

3. Il est interdit d'utiliser le code source ou les outils dérivés de ce projet pour modifier des autorisations sans l'accord du propriétaire de l'appareil, pirater des applications, voler des données, intégrer des logiciels malveillants, développer des outils de triche ou enfreindre toute loi applicable relative à la cybersécurité, au droit d'auteur ou autre. L'auteur de tels actes assume seul toute responsabilité civile, administrative et pénale ; les développeurs du projet ne sauraient être tenus responsables.<br>

4. Ce projet est distribué gratuitement uniquement par l'intermédiaire de communautés open source légitimes et ne propose aucune vente payante ni aucun service officiel de personnalisation. Les paquets payants ou versions modifiées proposés par des plateformes ou personnes tierces sont sans lien avec ce projet ; leur sécurité et leur intégrité ne sont pas garanties. Vous assumez les risques de vol de compte, de fuite de données personnelles ou d'infection par un logiciel malveillant liés à ces versions tierces.<br>

5. En téléchargeant, compilant ou flashant des fichiers de ce projet, vous confirmez avoir lu, compris et accepté l'intégralité de cet avertissement. Si vous ne l'acceptez pas, supprimez immédiatement le code source et les fichiers, puis cessez toute utilisation du projet.<br>

## Remerciements

- [KernelSU](https://github.com/tiann/KernelSU), avec nos remerciements à l'auteur weishu et à tous les contributeurs
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), référence pour la solution SuSFS
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), code du framework d'interface utilisé comme référence
- [kowsu](https://github.com/KOWX712/KernelSU.git), assistance technique
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/), source d'inspiration pour la conception de KernelSU
- [Magisk](https://github.com/topjohnwu/Magisk), solution root open source reconnue
- [genuine](https://github.com/brevent/genuine/), vérification des signatures APK
- [Diamorphine](https://github.com/m0nad/Diamorphine), référence pour les techniques de dissimulation de bas niveau
