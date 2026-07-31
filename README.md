# IdolProject - Modern Android Refactor

![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%2F%20Auth-FFCA28?logo=firebase)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-blue)
![Hilt](https://img.shields.io/badge/DI-Hilt-34A853)
![Flow](https://img.shields.io/badge/Async-Coroutine%20%2F%20Flow-0095D5)
![DataStore](https://img.shields.io/badge/Storage-DataStore-4285F4)
![Room](https://img.shields.io/badge/LocalDB-Room-6DB33F)
![Retrofit](https://img.shields.io/badge/Network-Retrofit-3E4348)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Test](https://img.shields.io/badge/Test-JUnit-red)

이 문서는 `refactor/modern-android` 브랜치에서 진행한 현대 Android 구조 리팩토링 내용을 정리한 README입니다.

기존 앱의 전체 기능 소개는 `main` 브랜치 기준 README에서 확인할 수 있으며, 이 브랜치에서는 기존 Firebase 기반 앱을 유지한 상태에서 구조 개선과 최신 Android 기술 적용에 집중했습니다.

---

## 📌 브랜치 목적

`main` 브랜치는 기존 안정 버전으로 유지하고, `refactor/modern-android` 브랜치에서 다음 목표를 중심으로 리팩토링을 진행했습니다.

| 목표 | 내용 |
|---|---|
| 기존 기능 유지 | Firebase 기반 인증, 친구, 랭킹, 미션, 커뮤니티 기능 유지 |
| 기존 UI 유지 | 기존 XML 기반 UI를 유지하면서 점진적으로 구조 개선 |
| 구조 개선 | 주요 화면을 MVVM + Repository 구조로 분리 |
| 비동기 개선 | Firebase callback 기반 코드를 Coroutine / Flow 기반으로 정리 |
| 의존성 주입 | Hilt를 통한 Repository / ViewModel 의존성 주입 |
| 로컬 저장소 개선 | DataStore를 통한 사용자 세션 저장 구조 개선 |
| 테스트 추가 | Unit Test를 통한 핵심 도메인 로직 검증 |
| 데이터 계층 확장 | Retrofit + Room 기반 데이터 계층 예제 추가 |
| UI 현대화 | Jetpack Compose를 기존 XML 앱에 점진 도입 |

---

## 🧩 리팩토링 전 문제점

초기 구조에서는 여러 화면의 `Fragment` 또는 `Activity` 내부에 다음 로직이 함께 섞여 있었습니다.

```text
Fragment / Activity
├── FirebaseAuth 접근
├── Firestore 쿼리
├── SnapshotListener 처리
├── UI 갱신
├── 상태 관리
├── Toast / Event 처리
└── 비즈니스 로직
```

이 구조는 기능이 커질수록 다음 문제가 발생할 수 있었습니다.

- Fragment 코드가 길어짐
- 화면과 데이터 처리 책임이 섞임
- 테스트하기 어려움
- Firebase callback 중첩으로 흐름 파악이 어려움
- 리팩토링 시 영향 범위 예측이 어려움

---

## 🏗️ 리팩토링 후 구조

리팩토링 후 주요 화면은 아래 구조를 기준으로 정리했습니다.

```text
Fragment / Activity
        ↓
ViewModel
        ↓
Repository
        ↓
Firebase / DataStore / Room / Retrofit
```

각 계층의 역할은 다음과 같이 분리했습니다.

| Layer | Responsibility |
|---|---|
| Fragment / Activity | UI 바인딩, 사용자 입력 처리, UiState 관찰 |
| ViewModel | 화면 상태 관리, Repository 호출, StateFlow 제공, 일회성 이벤트 처리 |
| Repository | Firebase / Room / Retrofit / DataStore 접근, 데이터 변환, 데이터 흐름 제공 |
| Policy / Manager | 순수 Kotlin 비즈니스 로직, Unit Test 대상 |

---

## 🛠️ 적용 기술 요약

| Category | Stack |
|---|---|
| Architecture | MVVM, Repository Pattern, UiState / Event |
| Dependency Injection | Hilt |
| Async | Coroutine, Flow, StateFlow, SharedFlow |
| Firebase | Firebase Auth, Cloud Firestore, FCM |
| Local Storage | DataStore, Room |
| Network | Retrofit, OkHttp Logging Interceptor |
| Test | JUnit Unit Test |
| UI | XML Layout, Material3, Jetpack Compose 일부 적용 |

---

## 1. Hilt DI 적용

Hilt를 도입해 Repository와 ViewModel 의존성 생성을 관리하도록 개선했습니다.

### 적용 내용

- `@HiltAndroidApp`
- `@AndroidEntryPoint`
- `@HiltViewModel`
- Repository 생성자 주입
- `DataStoreModule`
- `DatabaseModule`
- `NetworkModule`

### 개선 효과

- 객체 생성 책임 분리
- ViewModel과 Repository 연결 구조 명확화
- 테스트와 확장에 유리한 구조 마련

---

## 2. MVVM + Repository 구조 분리

기존 Fragment 중심 구조를 ViewModel + Repository 중심으로 분리했습니다.

### 적용한 주요 영역

| Area | Status |
|---|---|
| Ranking | 완료 |
| Mission | 완료 |
| MyPage | 완료 |
| Friend | 완료 |
| FriendProfile | 완료 |
| Group Schedule | 완료 |
| Comeback Schedule | 완료 |
| Home | 완료 |
| Community | 완료 |
| Login / Register / Splash | 완료 |
| Event | 완료 |

### 개선 효과

- Fragment는 UI 처리에 집중
- ViewModel은 상태 관리 담당
- Repository는 데이터 접근 담당
- 화면별 책임 분리

---

## 3. Firebase Callback → Coroutine / Flow 정리

Firebase callback 기반 코드를 Coroutine과 Flow 중심으로 정리했습니다.

### 적용 내용

- Firebase Task는 `await()` 사용
- Firestore `SnapshotListener`는 `callbackFlow`로 변환
- ViewModel에서는 `StateFlow`로 UI 상태 관리
- 일회성 메시지 / 이벤트는 Event 또는 SharedFlow 성격으로 분리

### 개선 효과

- 비동기 흐름 명확화
- ViewModel에서 collect 기반 상태 관리 가능
- Firebase 실시간 데이터와 MVVM 구조 연결 개선

---

## 4. DataStore 적용

기존 사용자 세션 저장 구조를 DataStore 기반으로 전환했습니다.

### 적용 파일

| File | Role |
|---|---|
| `UserSessionData` | 세션 데이터 모델 |
| `UserSessionRepository` | DataStore 기반 세션 저장소 |
| `DataStoreModule` | Hilt DataStore 제공 |
| `SplashActivity` | 앱 시작 시 세션 동기화 |
| `LogOutFragment` | 로그아웃 시 세션 삭제 |

### 개선 내용

- 기존 SharedPreferences 기반 UserSession 제거
- 로그인 세션 정보를 DataStore에 저장
- SplashActivity에서 FirebaseAuth / Firestore 기준으로 세션 동기화
- Logout 시 DataStore 세션 삭제

### 개선 효과

- 사용자 세션 관리 구조 명확화
- Coroutine / Flow 기반 로컬 저장소 적용
- 최신 Android 저장소 방식 경험 추가

---

## 5. Unit Test 기초 적용

핵심 순수 로직을 분리하고 JUnit 기반 Unit Test를 추가했습니다.

### 추가한 테스트

| Test | 검증 내용 |
|---|---|
| `BadgePolicyTest` | 레벨별 뱃지 계산, default / blank badgeId 보정 |
| `NicknamePolicyTest` | 닉네임 정규화, 길이 / 공백 검증 |
| `MissionRewardManagerTest` | EXP 지급, 레벨업, 잔여 EXP 이월 |
| `RankingPolicyTest` | Top3 / others 분리, 내 순위 계산 |
| `FriendRequestIdUtilTest` | 친구 요청 ID 생성 규칙 |

### 검증 내용

- 레벨별 뱃지 계산
- default / blank badgeId 보정
- 닉네임 정규화
- 닉네임 길이 / 공백 검증
- 미션 EXP 지급
- 레벨업 계산
- 잔여 EXP 이월
- 랭킹 Top3 / others 분리
- 내 순위 계산
- 친구 요청 ID 생성 규칙

### 개선 효과

- 리팩토링 후에도 핵심 계산 결과가 유지되는지 검증 가능
- 비즈니스 로직을 Fragment / Repository에서 분리
- 테스트 가능한 코드 구조 확보

### Unit Test 실행

```bash
./gradlew testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew testDebugUnitTest
```

---

## 6. Retrofit + Room 적용

이벤트 화면에 Retrofit + Room 기반 데이터 구조를 추가했습니다.

### 적용 목적

기존 Firebase 중심 앱에 REST API + Local DB 캐시 구조를 추가해 일반적인 실무 Android 데이터 계층을 경험하기 위함입니다.

### 구조

```text
EventFragment
        ↓
EventViewModel
        ↓
EventRepository
 ├── EventApiService  // Retrofit
 └── EventDao         // Room
        ↓
Room Flow
        ↓
UI
```

### 적용 파일

| File | Role |
|---|---|
| `EventApiService` | Retrofit API 인터페이스 |
| `EventDto` | 원격 응답 DTO |
| `NetworkModule` | Retrofit / OkHttp Hilt Module |
| `EventEntity` | Room Entity |
| `EventDao` | Room DAO |
| `AppDatabase` | Room Database |
| `DatabaseModule` | Room Hilt Module |
| `EventMapper` | DTO / Entity / UI Model 변환 |
| `EventRepository` | Remote + Local 데이터 통합 |
| `EventViewModel` | 이벤트 화면 상태 관리 |

### 동작 흐름

```text
1. 이벤트 화면 진입
2. Room에 저장된 이벤트 목록을 Flow로 관찰
3. Retrofit으로 원격 이벤트 목록 요청
4. 성공 시 Room에 저장
5. 실패 시 fallback 이벤트를 Room에 저장
6. UI는 Room Flow를 통해 자동 갱신
```

### 개선 효과

- RemoteDataSource와 LocalDataSource 역할 분리
- Room 기반 캐시 구조 적용
- Retrofit 기반 REST API 호출 구조 적용
- 네트워크 실패 시 fallback 데이터 표시 가능

---

## 7. Jetpack Compose 일부 전환

기존 XML 기반 앱 전체를 한 번에 Compose로 전환하지 않고, 독립적인 설정 화면부터 Compose를 점진 도입했습니다.

### 적용 구조

```text
SettingFragment
        ↓
fragment_setting.xml
        ↓
ComposeView
        ↓
SettingScreen()
```

### 적용 내용

- `ComposeView` 사용
- Material3 Compose UI 구성
- 앱 설정 섹션
- 정보 섹션
- `Switch`, `Card`, `TextButton` 구성

### 개선 효과

- 기존 Fragment / XML 구조와 Compose 공존
- 점진적 Compose 도입 방식 적용
- Material3 기반 현대적인 설정 화면 구현

---

## 📁 주요 패키지 구조

```text
com.example.idolproject
├── data
│   ├── local
│   │   ├── dao
│   │   ├── entity
│   │   └── AppDatabase
│   ├── mapper
│   ├── remote
│   │   ├── api
│   │   └── dto
│   └── repository
│
├── di
│   ├── DataStoreModule
│   ├── DatabaseModule
│   └── NetworkModule
│
├── domain
│   └── policy
│
├── Drawer
│   ├── Event
│   └── Setting
│
├── UI
│   ├── Friend
│   ├── Ranking
│   ├── Mission
│   ├── MyPage
│   ├── Home
│   └── Community
│
└── Login
```

---

## 🚀 빌드 및 테스트

### Debug 빌드

```bash
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew assembleDebug
```

### Unit Test

```bash
./gradlew testDebugUnitTest
```

Windows PowerShell:

```powershell
.\gradlew testDebugUnitTest
```

---

## 🌿 브랜치 전략

| Branch | Role |
|---|---|
| `main` | 기존 안정 버전 |
| `refactor/modern-android` | 현대 Android 구조 적용 브랜치 |

기존 앱을 바로 `main`에 합치지 않고 별도 브랜치에서 리팩토링을 진행해, 안정 버전과 구조 개선 버전을 분리해서 관리했습니다.

---

## 💼 포트폴리오 설명 요약

기존 Firebase 기반 Android 앱을 유지하면서 현대 Android 구조를 점진적으로 적용했습니다.

Fragment와 Activity에 집중되어 있던 Firebase 접근, UI 갱신, 상태 관리 로직을 ViewModel과 Repository로 분리하고, Firestore 실시간 데이터는 `callbackFlow`를 통해 Flow 기반 데이터 흐름으로 개선했습니다.

또한 DataStore를 통한 사용자 세션 관리, JUnit 기반 Unit Test, Retrofit과 Room을 활용한 이벤트 캐시 구조, Jetpack Compose 기반 설정 화면을 적용해 실무형 Android 데이터 계층과 점진적 Compose 도입 사례를 구현했습니다.

---

## 🔜 향후 개선 방향

- Compose 적용 범위 확대
- ViewModel 테스트 추가
- Room migration 처리
- Retrofit 실제 운영 API 연동
- WorkManager 기반 백그라운드 동기화
- UI 테스트 추가
- 리팩토링 브랜치 스크린샷 보강