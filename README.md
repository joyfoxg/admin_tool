# 🛡️ Android Device Admin & Permission Manager

안드로이드 14, 15, 16(API 34~36+) 환경에서 일반적인 방법으로 해제되지 않는 악성·고집 기기 관리자(Device Administrator) 앱을 포함한 모든 기기 관리자 앱을 안전하게 탐색, 관리, 강제 해제하고 세부 앱 권한을 제어하는 종합 시스템 관리 도구입니다.

---

## 🌟 주요 기능 (Key Features)

### 1. 기기 관리자(Device Admin) 완전 제어
- **전체 기기 관리자 탐색**: 활성화된 앱뿐만 아니라 비활성화된 기기 관리자 수신자(Receiver)까지 모두 탐색하여 표시.
- **표준 모드(Standard)**: 시스템 `DevicePolicyManager` API를 통한 기본 활성화/해제 인텐트 호출.
- **방해/고집 앱 강제 해제(Anti-Deactivation Bypass)**:
  - 오버레이 창(`SYSTEM_ALERT_WINDOW`)이나 접근성(`AccessibilityService`)으로 해제 화면 접근을 방해하는 앱을 **Shizuku(ADB) / Root 백그라운드 셸**을 통해 원클릭으로 강제 해제 (`dpm remove-active-admin`).
  - 프로세스 즉시 종료 (`am force-stop`) 및 방해 권한 사전 박탈.
- **안전 모드(Safe Mode) 가이드**: ADB나 루팅이 없는 일반 기기 사용자를 위한 단계별 안전 모드 부팅 가이드 제공.

### 2. 세부 권한(Permissions) 실시간 조회 및 수정
- **위험/특수 권한 실시간 인벤토리**: 위치, 카메라, 마이크, 알림, 오버레이, 접근성, 전체 파일 접근 권한 목록화.
- **원클릭 권한 토글**:
  - **Shizuku / Root 모드**: 앱 내에서 즉시 `pm grant/revoke` 및 `appops set` 실행.
  - **Standard 모드**: 해당 앱의 시스템 설정 페이지로 다이렉트 딥링크.
- **신속 방어 퀵 액션**:
  - `프로세스 강제 종료(Force Stop)`
  - `오버레이 권한 즉시 박탈`
  - `접근성 서비스 즉시 해제`

### 3. 보안 안전장치 (Safety Guardrails)
- `Google 내 기기 찾기`, `Samsung SmartThings Find` 등 시스템 필수 관리자 해제 시도 시 붉은색 경고 모달 및 2단계 확인 요구.

---

## 🏗️ 기술 스택 & 아키텍처

- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose + Material 3 (Material You Dynamic Theme, Edge-to-Edge)
- **Architecture**: Modern Clean Architecture + MVVM/MVI + StateFlow
- **Dependency Injection**: Dagger Hilt
- **Privilege Engines**:
  - [Shizuku API](https://github.com/RikkaApps/Shizuku) (무루팅 무선 ADB 권한 제어)
  - [LibSU](https://github.com/topjohnwu/libsu) (Root 슈퍼유저 셸 제어)
- **Compatibility**: Android 14 (API 34), Android 15 (API 35), Android 16 (API 36 호환)

---

## 🚀 빌드 및 실행 방법

### 1. 프로젝트 빌드 (Android Studio)
1. Android Studio (Ladybug / Iguana 이상 권장)에서 `e:\Project_python\admin_tool` 폴더 열기.
2. Gradle Sync 완료 후 타겟 디바이스(Android 14+ 실기기 또는 에뮬레이터)를 선택하고 실행 (`Run 'app'`).

### 2. Shizuku 무선 디버깅(Wireless ADB) 페어링 (PC 없이 가능)
1. 구글 플레이스토어에서 **Shizuku** 앱을 설치합니다.
2. 기기의 **[설정 > 개발자 옵션 > 무선 디버깅]**을 켭니다.
3. Shizuku 앱에서 **[페어링]**을 누르고 알림창에 6자리 페어링 코드를 입력합니다.
4. Shizuku 앱에서 **[시작]**을 눌러 서비스를 실행합니다.
5. 본 앱(Device Admin Tool)을 실행한 후 상단의 **[Shizuku 연동]** 버튼을 눌러 권한을 승인합니다.

---

## 📂 프로젝트 구조

```
admin_tool/
 ├── app/
 │    ├── src/main/
 │    │    ├── AndroidManifest.xml
 │    │    ├── java/com/admintool/deviceadmin/
 │    │    │    ├── MainActivity.kt
 │    │    │    ├── AdminToolApplication.kt
 │    │    │    ├── data/
 │    │    │    │    ├── model/         (DeviceAdminApp, AppPermission, PrivilegeMode)
 │    │    │    │    ├── repository/    (DeviceAdminRepository, PermissionRepository)
 │    │    │    │    └── shell/         (ShellExecutor, ShizukuShellExecutor, RootShellExecutor)
 │    │    │    ├── domain/usecase/     (GetDeviceAdmins, ForceDeactivateAdmin, TogglePermission)
 │    │    │    ├── di/                 (AppModule for Hilt)
 │    │    │    └── ui/
 │    │    │         ├── screen/        (AdminListScreen, AdminItemCard, AdminDetailBottomSheet, SafeModeGuideDialog)
 │    │    │         ├── theme/         (Color, Theme, Type)
 │    │    │         └── viewmodel/     (DeviceAdminViewModel)
 │    │    └── res/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 ├── gradle/libs.versions.toml
 ├── dev_prompt.md
 ├── dev_masterplan.md
 └── README.md
```
