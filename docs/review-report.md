# Baby Feeding Tracker -- 종합 리뷰 보고서

**리뷰 일자:** 2026-03-31
**리뷰어:** Senior Code Reviewer (AI-assisted)
**대상 범위:** 전체 프로젝트 코드베이스 + 3개 사이클의 개발 프로세스

---

## 1. 프로세스 리뷰

### 잘된 점

**1.1 Brainstorming -> Spec -> Plan -> Execute 워크플로우의 일관적 적용**

총 3개 사이클 모두 동일한 패턴을 따랐다:
- 사이클 1: 기능 개선 (삭제 버그, 수유 종류, 테마, 타임라인)
- 사이클 2: UX 개선 (경과 시간 한 줄, 바텀시트 자동 오픈/닫힘)
- 사이클 3: Firebase + 공유 기능

각 사이클마다 설계 문서(spec)와 구현 계획(plan)이 명확히 분리되어 있고, 구현 계획에는 파일별 변경 사항, 체크박스 단위 태스크, 예상 코드까지 포함되어 있다. 이는 서브에이전트 기반 개발에서 구현 품질의 일관성을 유지하는 데 핵심적인 역할을 했다.

**1.2 사용자 피드백의 즉시 반영과 문서화**

`docs/lessons-learned.md`에 실제 사용 후 발견한 3가지 문제점(경과 시간 분리 표시, 바텀시트 수동 오픈, 수동 닫기)을 원인-교훈-수정 형식으로 기록한 것은 매우 좋은 관행이다. 피드백 -> 설계 수정 -> 구현의 흐름이 사이클 2로 자연스럽게 이어졌다.

**1.3 점진적 아키텍처 전환**

Room -> Firestore 전환 시 "Repository 인터페이스 유지, 내부 구현만 교체"라는 원칙을 세우고 실행한 것이 좋다. 실제로 `FeedingRepository`의 메서드 시그니처가 거의 그대로 유지되었고, UI 코드는 `id: Long -> String` 타입 변경 정도의 최소 변경만 발생했다.

**1.4 커밋 이력의 논리적 구성**

27개 커밋이 기능 단위로 잘 분리되어 있고, 메시지가 변경의 의도를 명확히 전달한다. `feat:`, `fix:`, `build:`, `docs:` 접두사의 일관된 사용도 좋다.

### 개선할 점

**1.5 아키텍처 문서가 현재 상태를 반영하지 않는다 [Important]**

`docs/architecture.md`는 최초 Room 기반 아키텍처를 기술하고 있으며, Firebase/Firestore 전환, Google Sign-In, UserRepository, 공유 기능 등 사이클 2-3의 변경이 반영되지 않았다. 프로젝트 폴더 구조(섹션 4), Room Entity & DAO(섹션 5), 수동 DI 설계(섹션 7) 등이 현재 코드와 다르다.

**권장:** 각 사이클 완료 후 아키텍처 문서를 업데이트하는 단계를 프로세스에 포함시킨다.

**1.6 Firestore 전환 시 데이터 마이그레이션 전략 부재**

설계 문서에 "미출시 앱이므로 불필요"라고 명시했지만, 실제로 이전 Room 데이터가 있는 개발 단말에서의 동작은 고려되지 않았다. Room DB 파일이 남아있는 상태에서 앱이 정상 동작하는지(기존 데이터가 단순히 무시되는 것인지) 검증이 필요하다.

**1.7 빌드 오류 디버깅 기록이 부족하다**

Compose BOM 충돌, google-services.json 누락 등의 빌드 오류를 해결한 과정이 lessons-learned에 기록되지 않았다. `aeef486 fix: update Compose BOM to 2024.06.00 to fix Firebase version conflict` 커밋에서 Compose BOM 버전을 `2024.01.00` -> `2024.06.00`으로 올린 것은 Firebase BOM과의 충돌 때문인데, 이런 의존성 충돌 패턴은 반복되기 쉬우므로 기록해 두는 것이 좋다.

**1.8 테스트가 전혀 없다 [Critical]**

12개의 Kotlin 소스 파일이 있지만 테스트 파일은 0개이다. MVP 앱이라 이해할 수 있지만, 특히 다음 로직은 버그 발생 시 사용자 데이터에 직접 영향을 미치므로 테스트가 필요하다:
- `UserRepository.redeemInviteCode()` -- 양방향 프로필 업데이트의 원자성
- `AppContainer.resolveAndInitRepository()` -- dataOwnerUid 기반 경로 결정
- `formatElapsedTimeDisplay()` -- 엣지 케이스(0분, 정확히 1시간 등)

---

## 2. 코드 리뷰

### 아키텍처

**2.1 MVVM + Repository 패턴의 일관된 적용 [Good]**

전체 코드가 `MainScreen(Compose) -> MainViewModel -> FeedingRepository/UserRepository -> FirestoreDataSource/Firestore` 구조를 충실히 따르고 있다. ViewModel이 data 레이어의 구현 세부사항(Firestore)에 의존하지 않는다.

**2.2 AppContainer의 비동기 초기화 패턴 [Good]**

`repository: StateFlow<FeedingRepository?>` 패턴으로 인증 완료 전 null, 완료 후 non-null을 표현한 것이 깔끔하다. MainActivity에서 이를 `collectAsState`로 관찰하여 로딩 화면을 보여주는 흐름이 자연스럽다.

**2.3 MainViewModel의 책임 범위가 넓다 [Important]**

`MainViewModel`이 수유 기록 CRUD, 공유 상태, 로그인 상태, 초대 코드 생성/검증까지 모두 담당하고 있다(188줄). 현재 규모에서는 관리 가능하지만, 기능이 추가되면 분리를 고려해야 한다.

**권장:** 공유 관련 로직(`isGoogleLoggedIn`, `sharingState`, `inviteCode`, `sharingError` 및 관련 메서드)을 `SharingViewModel` 또는 `SharingUseCase`로 분리하는 것을 고려한다.

**2.4 AppContainer에서 CoroutineScope 직접 생성 [Suggestion]**

`AppContainer`(line 31)에서 `CoroutineScope(Dispatchers.Main)`을 직접 생성하는데, 이 스코프의 생명주기가 Application과 동일하므로 cancel이 불필요하긴 하다. 다만 명시적으로 `SupervisorJob()`을 포함시키면 자식 코루틴 실패 시 다른 코루틴이 영향받지 않는다:

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
```

### 보안

**2.5 초대 코드 충돌 가능성 [Critical]**

`UserRepository.generateInviteCode()`에서 6자리 영숫자 코드를 `chars.random()`으로 생성한다. 36^6 = 약 21억 개의 조합이 가능하지만:
- 코드 생성 시 기존 코드와의 중복 여부를 확인하지 않는다.
- 동시에 여러 사용자가 코드를 생성하면 이론적으로 충돌이 발생할 수 있다.

**권장:** `Firestore transaction` 또는 `set with merge`를 사용하여 기존 코드가 존재하면 재생성하도록 한다. 또는 UUID 기반으로 변경하되 사용자에게는 짧은 형식으로 표시한다.

**2.6 초대 코드 redeem의 비원자적 업데이트 [Critical]**

`redeemInviteCode()`에서 호스트 프로필 업데이트와 게스트 프로필 업데이트가 두 개의 독립적인 `.update().await()` 호출로 이루어진다(line 137-148). 첫 번째 업데이트 후 두 번째 업데이트가 실패하면 데이터가 비일관 상태에 빠진다(호스트는 게스트와 연결되었다고 생각하지만, 게스트는 아직 미연결 상태).

**권장:** Firestore의 `WriteBatch` 또는 `Transaction`을 사용하여 세 가지 작업(호스트 프로필 업데이트, 게스트 프로필 업데이트, 초대 코드 삭제)을 원자적으로 수행한다:

```kotlin
firestore.runBatch { batch ->
    batch.update(profileDocRef(hostUid), mapOf("linkedTo" to currentUid))
    batch.update(profileDocRef(currentUid), mapOf("linkedTo" to hostUid, "dataOwnerUid" to hostUid))
    batch.delete(inviteCodeDocRef(code))
}.await()
```

**2.7 프로필 읽기 권한이 과도하다 [Important]**

공유 기능 설계 문서(섹션 7)에서 Firestore 보안 규칙으로 `allow read: if request.auth != null`을 프로필에 적용했다. 이는 인증된 사용자라면 누구나 다른 사용자의 프로필(이메일 포함)을 읽을 수 있다는 의미이다.

**권장:** 프로필 읽기를 본인 + linkedTo에 등록된 파트너로 제한한다. 초대 코드 검증 시에는 Cloud Function 또는 제한적 필드 접근을 사용한다.

**2.8 Firestore 보안 규칙이 프로젝트에 포함되지 않았다 [Important]**

설계 문서에 보안 규칙이 기술되어 있지만, 실제 `firestore.rules` 파일이 프로젝트에 없다. Firebase Console에서 수동으로 설정한 것으로 보이는데, 코드로 관리하지 않으면 규칙이 의도치 않게 변경될 수 있다.

**권장:** 프로젝트 루트에 `firestore.rules` 파일을 추가하고, Firebase CLI로 배포할 수 있게 한다.

**2.9 invite_codes 컬렉션의 전체 읽기/쓰기 허용 [Important]**

설계 문서의 보안 규칙에서 `invite_codes`에 대해 `allow read, write: if request.auth != null`으로 되어 있다. 이는 인증된 사용자라면 다른 사용자의 초대 코드를 삭제하거나, 임의의 코드를 생성할 수 있다는 의미이다.

**권장:** 쓰기 규칙을 `hostUid == request.auth.uid`인 경우만 허용하고, 코드 삭제는 hostUid 또는 코드를 사용한 사용자만 가능하도록 제한한다.

### UI/UX 코드

**2.10 MainScreen.kt의 크기와 구조 [Suggestion]**

`MainScreen.kt`는 819줄로, 15개의 composable 함수와 유틸리티 함수를 포함한다. 현재 구조가 잘 섹션화되어 있고(주석 구분선 사용) 모든 함수가 `private`으로 선언되어 있어 캡슐화는 잘 되어 있다. 다만 파일이 커지면 다음과 같이 분리를 고려할 수 있다:
- `TimelineComponents.kt` -- `TimelineRecordRow`, `DateSectionHeader` 등
- `RecordEditBottomSheet.kt` -- 바텀시트 관련 composable
- `FeedingFormatters.kt` -- `formatElapsedTimeDisplay`, `formatRecordType` 등 유틸리티

**2.11 MainActivity의 중복 GoogleSignIn 런처 [Important]**

`MainActivity.kt`에서 `googleSignInLauncher`(line 36)와 `googleSignInLauncherWithRefresh`(line 61) 두 개의 런처가 등록되어 있다. 첫 번째 `googleSignInLauncher`는 실제로 사용되지 않는다(`googleSignInLauncherWithRefresh`만 `MainScreen`에 전달됨). 불필요한 코드가 남아 있다.

**권장:** 사용되지 않는 `googleSignInLauncher`(line 36-48)를 제거한다.

**2.12 ShareBottomSheet의 GoogleAuthHelper 직접 참조 [Suggestion]**

`ShareBottomSheet`가 `GoogleAuthHelper` 인스턴스를 직접 받아서 `getSignInIntent()`를 호출한다. Composable이 데이터 레이어 객체를 직접 참조하는 것은 MVVM 패턴에서 권장되지 않는다.

**권장:** `onGoogleSignIn: () -> Unit` 콜백으로 변경하고, Intent 생성 로직은 ViewModel 또는 Activity 레벨에서 처리한다.

**2.13 RecordEditBottomSheet의 remember 초기값 문제 [Suggestion]**

`RecordEditBottomSheet`에서 `selectedType`과 `selectedAmount`를 `remember { mutableStateOf(record.type) }`로 초기화한다. `record`가 변경되어도(예: Firestore 실시간 업데이트로 같은 ID의 레코드가 다른 값으로 올 경우) `remember`의 초기값은 갱신되지 않는다. 현재 사용 패턴에서는 문제가 발생하지 않지만, `key`를 사용하면 더 안전하다:

```kotlin
var selectedType by remember(record.id) { mutableStateOf(record.type) }
```

### 잠재적 이슈

**2.14 UserRepository.sharingState()에서 Flow 내 suspend 호출 [Critical]**

`UserRepository.sharingState()` (line 160-172)에서 `getProfile().map { }` 블록 안에서 `profileDocRef(partnerUid).get().await()`를 호출한다. `Flow.map`은 suspend 연산자이므로 기술적으로는 동작하지만:
- 프로필이 변경될 때마다 파트너의 프로필을 매번 네트워크로 가져온다 (캐시되지 않을 수 있음).
- 파트너 프로필 조회가 실패하면 전체 Flow가 에러로 종료된다.

**권장:** 파트너 이메일은 연결 시점에 호스트/게스트 양쪽 프로필에 저장하거나, 에러 처리를 추가한다:

```kotlin
fun sharingState(uid: String): Flow<SharingState> {
    return getProfile(uid).map { profile ->
        if (profile?.linkedTo == null) {
            SharingState.NotConnected
        } else {
            try {
                val partnerSnapshot = profileDocRef(profile.linkedTo).get().await()
                SharingState.Connected(partnerEmail = partnerSnapshot.getString("email"))
            } catch (e: Exception) {
                SharingState.Connected(partnerEmail = null)
            }
        }
    }
}
```

**2.15 익명 인증 실패 시 처리 부재 [Important]**

`AppContainer.init` (line 37)에서 `auth.signInAnonymously()`의 `addOnSuccessListener`만 등록하고 `addOnFailureListener`가 없다. 네트워크 오류 시 `_repository`는 영원히 null로 남아 앱이 로딩 화면에 멈춘다.

**권장:** 실패 리스너를 추가하고, 사용자에게 재시도 옵션을 제공한다:

```kotlin
auth.signInAnonymously()
    .addOnSuccessListener { result -> resolveAndInitRepository(result.user!!.uid) }
    .addOnFailureListener { /* 에러 상태 emit 또는 재시도 로직 */ }
```

**2.16 linkWithCredential 실패 시 대응 부재 [Important]**

`GoogleAuthHelper.handleSignInResult()`에서 `linkWithCredential`이 실패할 수 있는 케이스:
- 이미 다른 익명 계정에서 같은 Google 계정으로 링크한 경우 (`ERROR_CREDENTIAL_ALREADY_IN_USE`)
- 이 경우 `signInWithCredential`로 폴백해야 하지만, 현재 코드는 단순히 에러를 반환한다.

**권장:** `linkWithCredential` 실패 시 `AuthCredential`을 사용한 `signInWithCredential` 폴백을 구현한다.

**2.17 redeemInviteCode 후 Repository 경로 전환이 자동으로 되지 않을 수 있다 [Important]**

`MainViewModel.redeemInviteCode()`에서 `onSuccess` 콜백으로 `hostUid`를 전달하지만, 이를 받는 곳에서 `AppContainer.reinitializeWithDataOwner()`를 호출하는 코드가 보이지 않는다. `MainScreen`의 `onRedeemCode` 콜백(line 137):

```kotlin
onRedeemCode = { code ->
    viewModel.redeemInviteCode(code) { /* onSuccess handled by reinit */ }
},
```

주석에 "reinit"이 언급되어 있지만 실제로 `reinitializeWithDataOwner()`를 호출하지 않는다. 게스트가 초대 코드를 입력해도 앱을 재시작하기 전까지는 자신의 데이터를 계속 보게 된다.

**권장:** `onSuccess` 콜백에서 `AppContainer.reinitializeWithDataOwner(hostUid)`를 호출한다.

**2.18 다크 모드 미지원 시 시스템 다크 모드에서의 동작 [Suggestion]**

`BabyFeedingTrackerTheme`에서 `darkTheme` 파라미터를 받지만 항상 라이트 컬러 스킴을 사용한다. 시스템이 다크 모드일 때 상태바/네비게이션바의 아이콘 색상이 예상과 다를 수 있다.

---

## 3. 다음을 위한 제안

### 프로세스

**3.1 아키텍처 문서를 각 사이클의 필수 산출물로 포함**

현재 spec과 plan은 잘 작성되지만, 기존 아키텍처 문서의 업데이트는 빠져 있다. 각 사이클의 마지막 태스크로 "아키텍처 문서 업데이트"를 추가한다.

**3.2 빌드 의존성 충돌 해결 과정을 기록**

Compose BOM과 Firebase BOM의 버전 충돌은 Android 프로젝트에서 흔한 문제이다. 해결 과정을 lessons-learned에 기록하면 다음에 같은 문제를 만났을 때 시간을 절약할 수 있다.

**3.3 보안 리뷰를 설계 단계에서 수행**

공유 기능의 설계 문서에 Firestore 보안 규칙이 포함되어 있었지만, 구현 단계에서 실제로 적용/검증되었는지 확인할 수 없다. 보안 규칙의 구현과 테스트를 Plan의 태스크로 명시적으로 포함시킨다.

**3.4 수동 테스트 체크리스트를 더 구체적으로**

Firebase 전환 Plan의 Task 7에 수동 테스트 체크리스트가 있지만, 공유 기능 Plan에는 없다. 모든 사이클의 마지막에 수동 테스트 체크리스트를 포함시키되, 특히 엣지 케이스(네트워크 끊김, 만료된 코드, 자기 코드 입력 등)를 명시한다.

### 코드

**3.5 Firestore WriteBatch를 사용한 원자적 업데이트**

`redeemInviteCode()`의 세 가지 Firestore 작업(호스트 업데이트, 게스트 업데이트, 코드 삭제)을 `WriteBatch`로 묶는다. 이는 데이터 일관성을 보장하는 가장 중요한 개선이다.

**3.6 에러 상태의 UI 표현 강화**

현재 에러 처리가 로그 없이 Result.failure로만 전달되거나, UI에 단순 문자열로만 표시된다. 다음을 고려한다:
- 익명 인증 실패: 재시도 버튼이 있는 에러 화면
- Firestore 네트워크 에러: 오프라인 상태 표시
- 초대 코드 검증 실패: 더 구체적인 에러 메시지 (만료, 잘못된 코드, 이미 사용된 코드 등)

**3.7 Firebase Crashlytics 도입**

프로덕션에서 발생하는 비정상 종료와 에러를 추적하기 위해 Firebase Crashlytics를 추가한다. 이미 Firebase를 사용하고 있으므로 추가 비용이 거의 없다.

**3.8 연결 해제 기능**

설계 문서에서 "범위 외"로 명시했지만, 연결된 상태에서 연결을 해제할 수 있는 기능이 없으면 잘못 연결한 경우 복구할 수 없다. 다음 사이클에서 우선적으로 고려한다.

### 테스트

**3.9 단위 테스트 우선 대상**

테스트를 처음 도입한다면 다음 순서를 권장한다:

1. **UserRepository.redeemInviteCode()** -- 비즈니스 로직이 가장 복잡하고 데이터 일관성에 직접 영향
2. **AppContainer의 경로 결정 로직** -- dataOwnerUid 기반 FirestoreDataSource 초기화
3. **formatElapsedTimeDisplay()** 등 순수 함수 -- 가장 테스트하기 쉬움
4. **MainViewModel의 addRecord() 디바운스 로직** -- 시간 기반 로직은 버그가 발생하기 쉬움

**3.10 Firestore 에뮬레이터 기반 통합 테스트**

Firebase Emulator Suite를 사용하면 로컬에서 Firestore, Auth를 에뮬레이션하여 통합 테스트를 작성할 수 있다. 특히 초대 코드의 생성-검증-연결-만료 흐름은 통합 테스트가 적합하다.

**3.11 Compose UI 테스트**

바텀시트의 자동 오픈/닫힘 로직, 공유 상태별 UI 분기 등은 Compose UI 테스트로 검증한다. `createComposeRule()`을 사용하여:
- 새 기록 추가 시 바텀시트가 열리는지
- 모유 선택 시 바텀시트가 닫히는지 (isNewRecord=true)
- 편집 모드에서는 닫히지 않는지 (isNewRecord=false)

---

## 요약: 이슈 심각도별 분류

### Critical (반드시 수정)

| # | 이슈 | 파일 |
|---|------|------|
| 2.5 | 초대 코드 충돌 미검증 | `UserRepository.kt:77-98` |
| 2.6 | redeemInviteCode 비원자적 업데이트 | `UserRepository.kt:101-156` |
| 2.14 | sharingState Flow 내 suspend 호출 에러 미처리 | `UserRepository.kt:160-172` |
| 2.17 | redeemInviteCode 후 Repository 경로 미전환 | `MainScreen.kt:137` |

### Important (수정 권장)

| # | 이슈 | 파일/위치 |
|---|------|-----------|
| 1.5 | 아키텍처 문서 미갱신 | `docs/architecture.md` |
| 1.8 | 테스트 부재 | 프로젝트 전체 |
| 2.3 | MainViewModel 책임 과다 | `MainViewModel.kt` |
| 2.7 | 프로필 읽기 권한 과도 | Firestore 보안 규칙 |
| 2.8 | 보안 규칙 코드 미관리 | 프로젝트 루트 |
| 2.9 | invite_codes 권한 과도 | Firestore 보안 규칙 |
| 2.11 | 미사용 googleSignInLauncher | `MainActivity.kt:36-48` |
| 2.15 | 익명 인증 실패 미처리 | `AppContainer.kt:37` |
| 2.16 | linkWithCredential 실패 폴백 부재 | `GoogleAuthHelper.kt:57-60` |

### Suggestion (개선 시 좋음)

| # | 이슈 | 파일/위치 |
|---|------|-----------|
| 2.4 | SupervisorJob 추가 | `AppContainer.kt:31` |
| 2.10 | MainScreen.kt 파일 분리 | `MainScreen.kt` |
| 2.12 | ShareBottomSheet의 GoogleAuthHelper 참조 | `ShareBottomSheet.kt` |
| 2.13 | remember key 추가 | `MainScreen.kt:481-482` |
| 2.18 | 다크 모드 시스템 설정 대응 | `Theme.kt` |

---

*이 리뷰 보고서는 코드베이스의 모든 소스 파일, 4개의 설계 문서, 4개의 구현 계획, 그리고 27개의 git 커밋 이력을 분석하여 작성되었다.*
