# Baby Feeding Tracker - Architecture Document

## 1. Overview

신생아 수유 시각을 기록하고, 배우자와 실시간 공유하는 Android 앱.
버튼 하나로 현재 시각을 저장하고, 수유 종류(모유/분유)와 용량을 기록하며,
기록 목록, 경과 시간, 일일 통계를 표시한다.

**설계 원칙**: 극한의 단순함 + 실사용자 피드백 기반 UX.

---

## 2. Tech Stack

| 영역 | 선택 | 비고 |
|------|------|------|
| Language | Kotlin | |
| UI | Jetpack Compose + Material 3 | |
| Backend | Firebase (Firestore + Auth) | 실시간 동기화, 오프라인 지원 |
| Auth | Firebase Auth (Anonymous + Google) | 익명 시작, 공유 시 Google 업그레이드 |
| Async | Kotlin Coroutines + Flow | |
| DI | 수동 DI (AppContainer) | |
| Architecture | MVVM (ViewModel + Repository) | |
| minSdk / targetSdk | 26 / 34 | |

---

## 3. Architecture

```
┌─────────────────────────────────┐
│           UI Layer              │
│  MainScreen (Compose)           │
│  ShareBottomSheet (Compose)     │
│         │                       │
│  MainViewModel                  │
│         │                       │
├─────────────────────────────────┤
│         Data Layer              │
│  FeedingRepository              │
│  UserRepository                 │
│  GoogleAuthHelper               │
│         │                       │
│  FirestoreDataSource            │
│         │                       │
│  Firebase Firestore / Auth      │
└─────────────────────────────────┘
```

---

## 4. Project Folder Structure

```
app/src/main/java/com/baby/feedingtracker/
├── BabyFeedingApp.kt              # Application class
├── MainActivity.kt                # Single Activity + Google Sign-In result handling
├── di/
│   └── AppContainer.kt            # 수동 DI container (Firebase Auth, Firestore, Repositories)
├── data/
│   ├── FeedingRecord.kt           # 수유 기록 데이터 클래스
│   ├── FirestoreDataSource.kt     # Firestore CRUD + Flow
│   ├── FeedingRepository.kt       # 수유 기록 Repository
│   ├── UserRepository.kt          # 프로필, 초대 코드, 공유 상태 관리
│   └── GoogleAuthHelper.kt        # Google Sign-In + 익명 계정 연결
└── ui/
    ├── MainScreen.kt              # 메인 화면 (타임라인, 바텀시트, 일일 통계)
    ├── MainViewModel.kt           # ViewModel (수유 CRUD + 공유 상태)
    ├── ShareBottomSheet.kt        # 공유 바텀시트 (4개 상태)
    └── theme/
        └── Theme.kt               # Material 3 Theme (민트/연두 액센트)
```

---

## 5. Firestore Data Model

```
users/{uid}/
  profile/linked
    - linkedTo: String?           # 파트너 uid
    - dataOwnerUid: String        # 데이터 소유자 uid (공유 시 호스트)
    - email: String?              # Google 이메일

  feeding_records/{recordId}
    - timestamp: Long
    - type: String?               # "breast" | "formula" | null
    - amountMl: Int?              # 60~160 (10ml 단위)
    - createdAt: Timestamp

invite_codes/{code}
    - hostUid: String
    - createdAt: Timestamp
    - expiresAt: Timestamp        # 10분 후 만료
```

---

## 6. Key Features

### 6.1 수유 기록
- 버튼 탭 → 기록 추가 → 바텀시트 자동 오픈
- 모유 선택 → 즉시 닫힘 / 분유 선택 → 용량 선택 후 닫힘 (신규 기록)
- 기존 기록 탭 → 편집 모드 (자동 닫힘 없음)
- 타임라인 UI (동그라미 + 세로선, 날짜별 그룹)

### 6.2 일일 통계
- 날짜 헤더 아래에 총 X회 / 모유 X회 / 분유 X회·Xml 표시

### 6.3 배우자 공유
- 익명 로그인 → Google 로그인 업그레이드 (linkWithCredential)
- 6자리 초대 코드 생성/입력으로 연결
- 연결 후 양쪽이 같은 feeding_records를 바라봄
- 공유 아이콘 + 상태 dot (초록=연결, 빨간=미연결)

### 6.4 앱 재설치 대응
- linkWithCredential 실패 시 signInWithCredential로 폴백
- Google 로그인 후 dataOwnerUid 재확인 + Repository 재초기화
- ViewModel key로 repository 변경 시 자동 재생성

---

## 7. Firestore Security Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/profile/{doc} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /users/{userId}/feeding_records/{recordId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == userId ||
        get(/databases/$(database)/documents/users/$(userId)/profile/linked).data.linkedTo == request.auth.uid
      );
    }
    match /invite_codes/{code} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 8. Dependencies

- Compose BOM 2024.06.00
- Firebase BOM 32.7.0 (Auth, Firestore)
- Google Play Services Auth 21.0.0
- Lifecycle ViewModel Compose 2.7.0
- Core KTX 1.12.0

---

## 9. ADR (Architecture Decision Records)

### ADR-001: Hilt 대신 수동 DI
**상태**: Accepted. AppContainer에서 모든 의존성 관리. 규모가 작아 Hilt 비용이 이점을 초과.

### ADR-002: UseCase 레이어 생략
**상태**: Accepted. ViewModel → Repository 직접 호출. 비즈니스 로직이 단순.

### ADR-003: 단일 화면 + Navigation 미사용
**상태**: Accepted. 모든 기능이 바텀시트로 처리됨.

### ADR-004: Room → Firestore 전환
**상태**: Accepted (2026-03-31). 배우자 공유 기능을 위해 클라우드 DB 필요. Firestore 오프라인 퍼시스턴스로 Room 대체. Repository 인터페이스 유지, 내부만 교체.

### ADR-005: 익명 → Google 계정 업그레이드 방식
**상태**: Accepted (2026-03-31). 진입 장벽 최소화를 위해 익명 시작, 공유 시에만 Google 로그인. linkWithCredential로 uid 유지.
