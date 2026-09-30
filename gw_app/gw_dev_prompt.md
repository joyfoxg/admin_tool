# Galaxy Watch Shizuku Wi-Fi Bridge (Wear OS) - Development Prompt

> **문서 목적**: 본 프롬프트는 AI 어시스턴트(또는 개발자)에게 갤럭시 워치(Wear OS 4/5/6, Android 13/14/15/16 호환)에서 스마트폰의 무선 디버깅(Shizuku)을 활성화할 수 있도록 **원클릭 로컬 Wi-Fi 핫스팟(Local SoftAP) 및 브릿지 도구**를 개발하도록 지시하는 명세서입니다.

---

```markdown
# Role & Project Overview
당신은 Wear OS 및 안드로이드 시스템 네트워크 엔지니어링 전문 시니어 안드로이드 개발자입니다.
갤럭시 워치(Galaxy Watch 4, 5, 6, 7, 8 및 Wear OS 4/5/6 기기)에서 단독으로 작동하는 **워치용 Wi-Fi 핫스팟 브릿지 앱 (Watch Shizuku Bridge)**을 개발해주세요.

---

## 1. 프로젝트 배경 및 핵심 목적

- **문제 상황**: 사용자의 스마트폰이 통신사 제한이나 사내 정책 등으로 모바일 핫스팟을 켤 수 없고, 외부 Wi-Fi 공유기가 없는 환경에서 스마트폰 단독으로 Shizuku(무선 ADB)를 실행할 수 없음.
- **해결 방안**:
  - 갤럭시 워치에서 원클릭으로 **로컬 Wi-Fi 핫스팟(SoftAP)**을 생성.
  - 스마트폰이 워치의 Wi-Fi 신호에 연결되면, 스마트폰 내부 Wi-Fi IP 인터페이스가 활성화되어 스마트폰의 **[무선 디버깅]**을 켜고 **Shizuku**를 정상 실행할 수 있도록 함.
  - 인터넷 데이터 소모 없이 순수 로컬 Wi-Fi 인터페이스만으로 작동.

---

## 2. 핵심 요구사항 (Core Requirements)

### 2.1 워치 로컬 핫스팟(SoftAP) 제어 엔진
1. **Local-only Hotspot 생성 (`WifiManager.startLocalOnlyHotspot`)**:
   - 워치 하드웨어의 Wi-Fi 칩을 SoftAP 모드로 전환하여 자체 Wi-Fi SSID 및 비밀번호를 생성.
   - 워치 화면에 생성된 **Wi-Fi 이름(SSID)** 및 **비밀번호(Password)**를 명확하게 표시.
2. **원클릭 핫스팟 ON / OFF 토글**:
   - 메인 화면의 큰 스위치 버튼으로 1초 만에 핫스팟 시작 및 중지.
3. **연결 상태 모니터링**:
   - 핫스팟 활성화 상태 (`STOPPED`, `STARTING`, `RUNNING`, `FAILED`).
   - 할당된 워치 로컬 IP (`192.168.x.x` 등) 및 연결 포트 상태 표시.
4. **워치 배터리 보호 (Auto Timeout)**:
   - 스마트폰의 Shizuku 시작이 완료된 후 워치 배터리를 보존하기 위해, 5분/10분 후 자동 종료 타이머 옵션 제공.

### 2.2 Wear OS 최적화 UI / UX
1. **원형 디스플레이(Round Screen) 최적화**:
   - Compose for Wear OS (`androidx.wear.compose:compose-material3`) 기반 UI.
   - 워치 작은 화면에 맞춘 직관적인 원형 레이아웃 및 스크롤 뷰.
2. **화면 켜짐 유지(Keep Screen On) 옵션**:
   - 스마트폰에서 비밀번호를 보고 입력하는 동안 워치 화면이 바로 꺼지지 않도록 화면 유지 처리.

---

## 3. 기술 스택 & 아키텍처

- **Language**: Kotlin 2.x
- **Platform**: Wear OS (Min SDK: 30 / Target SDK: 35 / Compile SDK: 35)
- **UI Framework**: Compose for Wear OS + Wear Material 3 + Horologist
- **Architecture**: Modern Clean Architecture + MVVM + StateFlow
- **Dependency Injection**: Dagger Hilt
- **Network API**: Android `WifiManager.LocalOnlyHotspotReservation`, `ConnectivityManager`

---

## 4. 산출물 구조
1. `gw_app/settings.gradle.kts` & `gw_app/build.gradle.kts`
2. `gw_app/app/build.gradle.kts` (Wear OS 및 Compose for Wear 의존성 포함)
3. `gw_app/app/src/main/AndroidManifest.xml` (WIFI, ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES 권한)
4. `gw_app/app/src/main/java/com/admintool/watchbridge/`
   - `data/hotspot/WatchHotspotManager.kt` (LocalOnlyHotspot 생성 및 라이프사이클 관리)
   - `ui/screen/WatchMainScreen.kt` (원형 워치 UI 및 핫스팟 컨트롤러)
   - `ui/viewmodel/WatchBridgeViewModel.kt`
5. 설치 및 워치 무선 디버깅 가이드가 포함된 `README.md`
```
