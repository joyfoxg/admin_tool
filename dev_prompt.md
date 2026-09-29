# Android Device Admin & Permission Manager Development Prompt

> **문서 목적**: 본 프롬프트는 AI 어시스턴트(또는 개발자)에게 안드로이드 14, 15, 16 환경에서 **루팅 없이(Non-Root)** 기기 관리자(Device Administrator) 앱 조회/활성화, **설정에서 비활성화(회색/잠김)된 앱 및 접근 차단 앱의 강제 해제**, 그리고 앱 권한 통합 관리 도구를 개발하도록 지시하는 명세서입니다.

---

```markdown
# Role & Project Overview
당신은 안드로이드 시스템 엔지니어링 및 최신 Jetpack Compose 전문 시니어 안드로이드 개발자입니다.
Android 14 (API 34), Android 15 (API 35), Android 16 (API 36 호환) 환경에서 작동하는 **기기 관리자(Device Admin) 및 앱 권한 통합 관리 툴(Device Admin & Permission Manager)** 앱을 개발해주세요.

> **중요 전제 조건**:
> - 본 앱은 **비루팅(Non-Root) 순정 안드로이드 폰**을 대상으로 합니다. (루트 의존성 제거)
> - 고급 권한 제어 및 강제 해제는 사용자가 PC 없이 폰 자체에서 실행할 수 있는 **Shizuku (무선 ADB API)**를 사용합니다.

---

## 1. 핵심 요구사항 (Core Requirements)

### 1.1 기기 관리자(Device Admin) 앱 탐색 및 활성화/해제
1. **기기 관리자 목록 검색 및 식별**:
   - 기기에 등록된 모든 기기 관리자 앱(활성/비활성 상태)을 `DevicePolicyManager` 및 `PackageManager`를 통해 탐색하고 목록화.
   - 시스템 필수 기기 관리자(예: 내 기기 찾기, Google Play 서비스 등)와 타사(3rd-party) 앱을 명확히 구분하여 배지(Badge) 표시.
2. **설정에서 비활성화(회색으로 잠겨서 해제 불가)된 기기 관리자 해제**:
   - **문제 상황**: 스마트폰 [설정 > 기기 관리자] 화면에 들어가면 일부 앱(프로필 소유자, 기업 관리/MDM 정책 앱, 자녀 보호 앱, 잠금 정책 앱 등)이 체크된 상태로 비활성화(Greyed-out/Dimmed)되어 사용자가 직접 체크를 해제할 수 없음.
   - **해결 솔루션**:
     - Shizuku(무선 ADB) 연동을 통해 시스템 설정 UI의 잠금을 우회.
     - `dpm clear-profile-owner`, `dpm clear-device-owner` 상태 해제 시도.
     - `dpm remove-active-admin <Component>` 및 `cmd device_policy remove-active-admin <Component>` 명령으로 설정 잠김 앱을 백그라운드에서 직접 강제 해제.
3. **접근 차단/강제 방해(Anti-Deactivation) 악성·고집 앱 해제**:
   - **문제 상황**: 해제 화면 접근 시 오버레이(Overlay)를 띄우거나 접근성(Accessibility) 서비스로 뒤로가기(Back)를 강제하여 해제를 방해하는 경우.
   - **해결 솔루션**:
     - 1단계: 타겟 앱 프로세스 강제 종료 (`am force-stop <package>`)
     - 2단계: 화면 오버레이 권한 박탈 (`appops set <package> SYSTEM_ALERT_WINDOW ignore`)
     - 3단계: 접근성 서비스 권한 차단 (`pm revoke <package> android.permission.BIND_ACCESSIBILITY_SERVICE`)
     - 4단계: 기기 관리자 강제 해제 (`dpm remove-active-admin <Component>`)
     - 5단계 (일반 사용자용): ADB 연결이 어려운 사용자를 위한 '안전 모드(Safe Mode)' 단계별 가이드 다이얼로그 제공.

### 1.2 기기 관리자 앱의 권한(Permissions) 조회 및 수정
1. **권한 인벤토리 조회**:
   - 각 기기 관리자 앱이 보유한 런타임 권한(위치, 카메라, 마이크, 알림 등) 및 특수 권한(오버레이, 접근성, 배터리 최적화 예외, 모든 파일 접근) 목록 표시.
2. **권한 수정 기능**:
   - **Shizuku (무선 ADB) 모드**: `pm grant/revoke <package> <permission>` 및 `appops set <package> <op> allow/ignore`로 앱 내에서 즉시 온/오프 토글.
   - **Standard (기본) 모드**: 해당 앱의 시스템 앱 정보/권한 설정 페이지로 이동.

### 1.3 안드로이드 14, 15, 16 호환성 및 정책 준수
- **Android 14 (API 34)**: 세부 사진/미디어 권한 대응, 포그라운드 서비스 타입 준수.
- **Android 15 (API 35)**: Edge-to-Edge 기본 적용, 16KB 페이지 크기 메모리 호환, 프라이빗 스페이스 앱 탐색 대응.
- **Android 16 (API 36 호환)**: 최신 Target SDK 설정 및 최신 Jetpack Compose 아키텍처.

---

## 2. 기술 스택 & 아키텍처

- **언어**: Kotlin 2.x
- **UI 프레임워크**: Jetpack Compose + Material 3 (Material You 테마)
- **아키텍처**: Modern Android Clean Architecture + MVVM/MVI
- **의존성 주입**: Dagger Hilt
- **ADB 연동 라이브러리**: `rikka.shizuku:api:13.+` & `rikka.shizuku:provider:13.+` (루트 라이브러리 제외)

---

## 3. UI/UX 디자인 요구사항

1. **대시보드 화면**:
   - 상단: 실행 모드 카드 (Standard / Shizuku 무선 ADB) 및 Shizuku 바인딩 버튼.
   - 검색창 & 필터 탭 (`전체`, `활성화됨`, `비활성화됨`, `사용자 앱`, `시스템 앱`).
   - 앱 카드: '🔒 설정에서 비활성화(잠김)' 배지, 위험 권한 태그, '잠김 강제 해제' 버튼.
2. **앱 상세 & 권한 수정 바텀시트**:
   - '프로세스 종료', '오버레이 권한 박탈', '접근성 해제' 퀵 액션 버튼.
   - 권한별 실시간 원클릭 스위치.
3. **안전장치 (Safety Modal)**:
   - 중요 시스템 기기 관리자 해제 시도시 경고 모달 (`Google 내 기기 찾기` 등).
4. **안전 모드(Safe Mode) 안내**:
   - Shizuku 미사용자를 위한 단계별 안전 모드 부팅 가이드.
```
