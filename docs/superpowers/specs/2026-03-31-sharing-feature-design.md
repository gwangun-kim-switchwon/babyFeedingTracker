# Baby Feeding Tracker - 공유 기능 설계 (Sub-project 2)

## 1. Overview

배우자/파트너와 수유 기록을 공유하는 기능. 익명 계정을 Google 로그인으로 업그레이드하고, 6자리 초대 코드로 두 사용자를 연결하여 같은 Firestore 데이터를 바라보게 한다.

---

## 2. Firestore 데이터 구조 (추가)

### 초대 코드

```
invite_codes/{code}
  - hostUid: String         // 코드를 생성한 사용자의 uid
  - createdAt: Timestamp    // 생성 시각
  - expiresAt: Timestamp    // 생성 후 10분
```

- 코드는 6자리 영숫자 대문자 (예: "3A8K72").
- 유효 시간 10분. 만료 후 사용 불가.

### 사용자 프로필

```
users/{uid}/
  profile/linked
    - linkedTo: String?       // 파트너의 uid (null이면 미연결)
    - dataOwnerUid: String    // 실제 데이터가 있는 uid
    - email: String?          // Google 로그인 이메일 (표시용)
  feeding_records/{recordId}
    - (기존과 동일)
```

- `dataOwnerUid`: 공유 시 한쪽(호스트)의 uid로 통일. 양쪽 모두 이 uid의 `feeding_records`를 바라봄.
- 초기값: 자기 자신의 uid.

---

## 3. 공유 흐름

### 3.1 호스트 (코드 생성 측)

1. 공유 아이콘 탭 → 바텀시트
2. 익명 상태면 Google 로그인 버튼 표시 → 로그인 (익명 계정 `linkWithCredential`로 업그레이드, uid 유지)
3. 로그인 완료 → 프로필 문서 생성 (`dataOwnerUid = 자기 uid`, `linkedTo = null`)
4. "초대 코드 만들기" 탭 → 6자리 코드 생성 → `invite_codes/{code}`에 저장
5. 코드 표시 + 복사 버튼 (카톡으로 전달)

### 3.2 게스트 (코드 입력 측)

1. 공유 아이콘 탭 → 바텀시트
2. 익명 상태면 Google 로그인 → 프로필 문서 생성
3. 초대 코드 6자리 입력 → "연결하기" 탭
4. `invite_codes/{code}` 조회 → 만료 확인 → 호스트 uid 획득
5. 양쪽 프로필 업데이트:
   - 호스트: `linkedTo = 게스트uid`
   - 게스트: `linkedTo = 호스트uid`, `dataOwnerUid = 호스트uid`
6. 게스트의 FirestoreDataSource 경로가 호스트의 uid로 전환 → 호스트의 데이터를 바라봄
7. 초대 코드 문서 삭제 (일회용)

### 3.3 연결 후

- 양쪽 모두 같은 `feeding_records` 컬렉션을 읽고 씀.
- Firestore 실시간 동기화로 한쪽이 기록하면 상대에게 즉시 반영.

---

## 4. Google 로그인 업그레이드

- `linkWithCredential(googleCredential)`로 익명 계정에 Google 자격 증명 연결.
- uid가 변경되지 않으므로 기존 Firestore 데이터 유지.
- 이미 Google 로그인된 상태면 바로 초대 코드 화면으로 진행.

---

## 5. 공유 바텀시트 UI

메인 화면 상단에 공유 아이콘 + 상태 dot 배치. 탭하면 바텀시트.

### 상태 dot
- 연결됨: 초록색 dot
- 미연결: 빨간색 dot

### 바텀시트 상태별 내용

**상태 A: 미로그인 (익명)**

```
┌──────────────────────────────┐
│  공유하기                     │
│                              │
│  배우자와 수유 기록을 함께     │
│  보려면 로그인이 필요합니다     │
│                              │
│  ┌─────────────────────┐     │
│  │  G  Google로 로그인   │     │
│  └─────────────────────┘     │
└──────────────────────────────┘
```

**상태 B: 로그인됨, 미연결**

```
┌──────────────────────────────┐
│  공유하기                     │
│                              │
│  [초대 코드 만들기]            │
│                              │
│  ── 또는 ──────────────────  │
│                              │
│  초대 코드 입력               │
│  [ _ _ _ _ _ _ ]             │
│  [연결하기]                   │
└──────────────────────────────┘
```

**상태 C: 코드 생성됨**

```
┌──────────────────────────────┐
│  공유하기                     │
│                              │
│  초대 코드                    │
│  [ 3 8 A 7 K 2 ]            │
│  10분 후 만료                 │
│                              │
│  [코드 복사하기]              │
└──────────────────────────────┘
```

**상태 D: 연결됨**

```
┌──────────────────────────────┐
│  공유 상태                    │
│                              │
│  🟢 연결됨                    │
│  user@gmail.com 과 공유 중    │
└──────────────────────────────┘
```

---

## 6. AppContainer / DI 변경

- `AppContainer`에서 인증 완료 후 프로필 문서(`users/{uid}/profile/linked`)를 확인.
- `dataOwnerUid`가 자기 uid와 다르면(게스트) → FirestoreDataSource의 경로를 `dataOwnerUid`로 설정.
- `repository`는 프로필 확인 후 초기화.

---

## 7. Firestore 보안 규칙

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 사용자 프로필: 본인만 읽기/쓰기
    match /users/{userId}/profile/{doc} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      // 파트너도 프로필 읽기 허용 (연결 상태 확인용)
      allow read: if request.auth != null;
    }

    // 수유 기록: 본인 또는 linkedTo에 등록된 파트너
    match /users/{userId}/feeding_records/{recordId} {
      allow read, write: if request.auth != null && (
        request.auth.uid == userId ||
        get(/databases/$(database)/documents/users/$(userId)/profile/linked).data.linkedTo == request.auth.uid
      );
    }

    // 초대 코드: 인증된 사용자 누구나 읽기/쓰기 (코드 검증용)
    match /invite_codes/{code} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 8. 파일 변경 목록

| 파일 | Action | 내용 |
|------|--------|------|
| `app/build.gradle.kts` | Modify | Google Sign-In (`play-services-auth`) 의존성 추가 |
| `data/FirestoreDataSource.kt` | Modify | uid를 동적으로 변경 가능하게 (dataOwnerUid 반영) |
| `data/UserRepository.kt` | Create | 프로필 CRUD, 초대 코드 생성/검증/연결, 공유 상태 Flow |
| `data/GoogleAuthHelper.kt` | Create | Google Sign-In + 익명 계정 linkWithCredential |
| `di/AppContainer.kt` | Modify | UserRepository, GoogleAuthHelper 주입, 프로필 기반 경로 결정 |
| `ui/MainViewModel.kt` | Modify | 공유 상태, 로그인 상태, 초대 코드 관련 메서드 |
| `ui/MainScreen.kt` | Modify | 상단 공유 아이콘 + 상태 dot |
| `ui/ShareBottomSheet.kt` | Create | 공유 바텀시트 (4개 상태 분기) |

---

## 9. Prerequisites (사용자 수동 설정)

1. Firebase Console → Authentication → Sign-in method → **Google 사용 설정**
2. Firebase Console → 프로젝트 설정 → 앱 → **SHA-1 지문 추가** (debug keystore)
3. SHA-1 등록 후 **`google-services.json` 재다운로드** → `app/`에 덮어쓰기
4. Firestore 보안 규칙을 섹션 7의 내용으로 업데이트

---

## 10. 범위 외

- 연결 해제 기능
- 푸시 알림 (상대가 기록했을 때)
- 멀티 아기 지원
- 다크 모드
