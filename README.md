# Idol Project

K-pop 팬들을 위한 Android 커뮤니티 앱입니다.
사용자는 최애 그룹을 설정하고, 친구를 추가하며, 랭킹·미션·일정·팬톡 기능을 통해 팬 활동을 관리할 수 있습니다.

## 주요 기능

* Firebase Auth 기반 로그인 및 회원가입
* 닉네임 중복 검사
* 최애 그룹 설정
* 친구 검색, 친구 요청, 수락/거절, 친구 삭제
* 유저 랭킹 및 그룹 랭킹
* 레벨, EXP, 뱃지 시스템
* 일일/주간 미션
* 컴백 일정 및 그룹 일정 관리
* 최애 그룹 기준 홈 일정 표시
* 최애 그룹 팬톡방 기반 실시간 채팅
* Firebase Cloud Messaging 기반 알림 기능

## 기술 스택

* Kotlin
* Android XML
* Firebase Authentication
* Cloud Firestore
* Firebase Cloud Messaging
* Firebase Functions
* Material Components
* ViewPager2
* RecyclerView
* MaterialCalendarView

## 프로젝트 구조

```text
app/src/main/java/com/example/idolproject
├── Login
├── UI
│   ├── Home
│   ├── Friend
│   ├── Ranking
│   ├── Mission
│   └── MyPage
├── Drawer
│   ├── ComeBack
│   ├── Group
│   └── Community
├── FCM
└── MainActivity.kt
```

## 화면 구성

* 홈: 최애 그룹 기준 오늘 일정과 팬톡 바로가기
* 친구: 친구 목록, 친구 검색, 친구 요청 관리
* 랭킹: 유저 랭킹과 그룹 랭킹
* 미션: 일일/주간 미션과 EXP 보상
* 마이페이지: 프로필, 레벨, 뱃지, 최애 그룹 관리
* 커뮤니티: 최애 그룹 팬톡방
* 일정: 컴백 일정 및 그룹 활동 일정
* 고객센터 설정등 아직 미구현

## Firebase 설정

이 프로젝트는 Firebase를 사용합니다.

Public 저장소에는 보안상 `google-services.json` 파일을 포함하지 않습니다.
프로젝트를 실행하려면 Firebase Console에서 Android 앱을 등록한 뒤, 다운로드한 `google-services.json` 파일을 아래 위치에 추가해야 합니다.

```text
app/google-services.json
```

## 실행 방법

1. 이 저장소를 clone합니다.
2. Firebase Console에서 Android 앱을 등록합니다.
3. `google-services.json` 파일을 `app/` 폴더에 추가합니다.
4. Android Studio에서 프로젝트를 엽니다.
5. Gradle Sync를 실행합니다.
6. 에뮬레이터 또는 실제 기기에서 앱을 실행합니다.

## 보안 관련 안내

아래 파일은 저장소에 포함하지 않습니다.

```text
local.properties
app/google-services.json
*.jks
*.keystore
keystore.properties
serviceAccountKey.json
.env
```

## 개발 목적

이 프로젝트는 Android 앱 개발 포트폴리오 목적으로 제작되었습니다.
Firebase 기반 인증, 데이터베이스, 실시간 채팅, 알림, 랭킹, 미션 시스템 등 실제 앱에서 자주 사용되는 기능을 학습하고 구현하는 것을 목표로 합니다.
