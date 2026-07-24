<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="ApkeSU 프로젝트 아바타">
  <h1>ApkeSU</h1>
  <p>GKI Android 기기를 위한 커널 기반 루트 관리자</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <strong>한국어</strong> ·
    <a href="README.es.md">Español</a>
  </p>
  <p>
    <a href="#프로젝트-개요">프로젝트 개요</a> ·
    <a href="#업스트림-프로젝트">업스트림</a> ·
    <a href="#오픈-소스-라이선스-준수">라이선스</a> ·
    <a href="#면책-조항">면책 조항</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">QQ 그룹</a>
  </p>
</div>

---

ApkeSU는 공식 업스트림 [KernelSU](https://github.com/tiann/KernelSU) 저장소와 [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)를 기반으로 한 파생 오픈 소스 프로젝트입니다. GKI Android 기기를 위한 커널 기반 루트 관리자로서 KMI 일치, 안정적인 SuSFS 구성, 복구 가능한 유지 관리, KernelSU Manager 사용 경험, UI 확장, LKM 패치 및 개인 소유 기기 디버깅에 중점을 둡니다.

## 프로젝트 개요

이 프로젝트는 KernelSU의 라이선스 구조를 따릅니다. `kernel/` 디렉터리는 업스트림 KernelSU 및 Linux 커널과 동일하게 **GPL-2.0-only** 라이선스를 따르며, `kernel/` 외부의 KernelSU 파생 코드는 **GPL-3.0-or-later** 라이선스를 따릅니다. 타사 종속성에는 각 업스트림 라이선스가 적용됩니다. 자세한 내용은 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)를 참조하십시오.

## 업스트림 프로젝트

주요 업스트림 프로젝트: KernelSU

업스트림 저장소: https://github.com/tiann/KernelSU

이 프로젝트는 업스트림 소스 업데이트, 보안 패치 및 기능 개선을 주기적으로 반영합니다.

## 오픈 소스 라이선스 준수

1. 모든 변경 사항과 파생 코드는 계속해서 오픈 소스로 공개됩니다. 커널, APK, 모듈을 포함하여 배포되는 모든 빌드에는 완전한 대응 소스 코드가 함께 제공됩니다.
2. 누구나 해당 라이선스 조건에 따라 이 프로젝트를 취득, 수정 및 재배포할 수 있습니다. 재배포 시 업스트림 출처, 라이선스 고지 및 완전한 대응 소스 코드를 유지해야 합니다.
3. 이 프로젝트의 수정 버전을 공개 배포하는 경우 전체 수정 소스 코드도 공개하고 업스트림 출처를 명시해야 합니다.

> 중요: 이 프로젝트는 업스트림 기능을 확장하지만 기반 권한 모델은 KernelSU의 네이티브 구현을 유지합니다.
> MIUI 스타일 테마는 독립적인 시각 구현이며 Xiaomi의 공식 소스 코드를 사용하지 않습니다.

## 사용 목적

이 도구는 Android 기기에 대한 로컬 기술 연구, 학습 및 본인이 소유한 기기의 디버깅만을 목적으로 합니다. 앱 권한의 불법 변경, 위험 관리 우회, 무단 접근 또는 기타 불법 행위에 사용하지 마십시오.

## 면책 조항

1. 이 프로젝트는 Android 저수준 기술 학습과 오픈 소스 기술 교류만을 목적으로 합니다. 모든 도구와 소스 코드는 합법적인 개인 연구용으로만 제공됩니다. ROM 또는 커널 플래싱이나 프로젝트 관련 파일 설치로 인해 발생하는 부팅 실패, 무한 재부팅, 기기 벽돌화, 하드웨어 손상 또는 기타 기기 고장에 대해 개발자는 수리, 배상 또는 사후 지원 책임을 지지 않습니다. 모든 위험은 사용자가 부담합니다.<br>

2. 금융, 온라인 게임, 기업 업무 및 정부 서비스 앱은 일반적으로 Root 환경과 커널 권한에 대한 위험 관리 기능을 사용합니다. 이 도구 사용으로 발생하는 계정 정지, 기기 차단, 기능 제한 또는 금전적 손실의 위험은 전적으로 사용자가 부담합니다. 프로젝트 개발자는 계정 이의 제기나 위험 관리 우회를 지원하지 않습니다.<br>

3. 기기 소유자의 허가 없는 권한 변경, 앱 크래킹, 데이터 절도, 악성 프로그램 번들링, 치트 개발 또는 적용 가능한 사이버 보안법, 저작권법 및 기타 법률 위반에 이 프로젝트의 소스 코드나 파생 도구를 사용하는 것을 금지합니다. 위반자는 모든 민사, 행정 및 형사 책임을 단독으로 부담하며 프로젝트 개발자는 책임을 지지 않습니다.<br>

4. 이 프로젝트는 정식 오픈 소스 커뮤니티를 통해서만 무료로 배포되며 공식 유료 판매나 맞춤 제작 서비스를 제공하지 않습니다. 타사 플랫폼이나 개인이 제공하는 유료 패키지 또는 수정 버전은 이 프로젝트와 관련이 없으며 안전성과 무결성을 보장할 수 없습니다. 타사 빌드로 인한 계정 도용, 개인정보 유출 또는 악성코드 감염 위험은 사용자가 부담합니다.<br>

5. 이 프로젝트의 파일을 다운로드, 컴파일 또는 플래싱하면 본 면책 조항 전체를 읽고 이해했으며 동의한 것으로 간주됩니다. 동의하지 않는 경우 소스 코드와 파일을 즉시 삭제하고 프로젝트 사용을 중단하십시오.<br>

## 감사의 말

- [KernelSU](https://github.com/tiann/KernelSU): 저자 weishu 및 모든 기여자
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra): SuSFS 솔루션 참고
- [FolkPatch](https://github.com/LyraVoid/FolkPatch): UI 프레임워크 코드 참고
- [kowsu](https://github.com/KOWX712/KernelSU.git): 기술 지원
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/): KernelSU 설계에 영감을 준 프로젝트
- [Magisk](https://github.com/topjohnwu/Magisk): 잘 알려진 오픈 소스 Root 솔루션
- [genuine](https://github.com/brevent/genuine/): APK 서명 검증
- [Diamorphine](https://github.com/m0nad/Diamorphine): 저수준 숨김 기술 참고
