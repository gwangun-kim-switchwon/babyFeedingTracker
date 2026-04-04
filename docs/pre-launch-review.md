# Play Store 출시 전 최종 코드 품질 점검 보고서

**점검일**: 2026-03-31
**대상 프로젝트**: baby-feeding-tracker (맘마미아)
**소스 파일 수**: 24개 Kotlin 파일 (테스트 파일 0개)
**점검자**: Code Reviewer (Automated)

---

## 1. 미사용 코드 / Import

### [Critical] 미사용 Import

| 파일 | 미사용 Import | 비고 |
|------|-------------|------|
| `FeedingScreen.kt` | `AnimatedVisibility` | 실제 사용 중이므로 문제 없음 |
| `GoogleAuthHelper.kt` | `IntentSender` | **미사용** - 제거 필요 |
| `UserRepository.kt` | `import com.google.firebase.firestore.snapshots` | `snapshots()` 사용 중이므로 유효 |
| `MainActivity.kt` | `googleSignInLauncher` (line 40-52) | 첫 번째 launcher는 `googleSignInLauncherWithRefresh`에 의해 완전히 대체됨. **데드 코드** |

### [Important] 데드 코드

- **`MainActivity.kt` line 40-52**: `googleSignInLauncher`가 선언되지만 실제 사용되지 않음. `googleSignInLauncherWithRefresh`가 동일한 역할을 하면서 추가로 `refreshLoginState()`를 호출함. 첫 번째 launcher는 제거해야 함.

---

## 2. 하드코딩된 문자열 (i18n 미대응)

### [Critical] strings.xml에 `app_name` 하나만 정의

`strings.xml`에는 앱 이름만 등록되어 있고, UI에 노출되는 **모든 문자열이 Kotlin 코드에 하드코딩**되어 있음. 한국어 단일 언어 앱이라면 당장 문제는 아니지만, Play Store 글로벌 출시 또는 향후 다국어 지원 시 대규모 리팩터링이 필요함.

### 하드코딩된 문자열 목록 (주요 항목)

**FeedingScreen.kt**:
- `"마지막 수유"`, `"첫 수유를\n기록해보세요"`, `"아직 기록이 없어요"`
- `"+ 수유 기록"`, `"기록 삭제"`, `"삭제"`, `"취소"`
- `"모유"`, `"분유"`, `"유축"`, `"용량"`, `"왼쪽"`, `"오른쪽"`
- `"수유 기록"`, `"확인"`, `"오늘"`, `"어제"`

**CleaningScreen.kt**:
- `"마지막 세척"`, `"첫 세척을\n기록해보세요"`, `"+ 세척 기록"`
- `"젖병"`, `"분유포트"`, `"유축기"`, `"기타"`
- `"총"`, `"회"`, `"간격"` 등

**DiaperScreen.kt**:
- `"마지막 기저귀"`, `"첫 기저귀를\n기록해보세요"`, `"+ 기저귀 기록"`
- `"기저귀 교체"`, `"소변"`, `"대변"`

**ShareBottomSheet.kt**:
- `"공유하기"`, `"초대 코드 만들기"`, `"초대 코드 입력"`, `"연결하기"`
- `"G  Google로 로그인"`, `"10분 후 만료"`, `"코드 복사하기"`
- `"공유 상태"`, `"연결됨"`, `"과 공유 중"`

**BottomNavItem.kt**:
- `"수유"`, `"기저귀"`, `"세척"`

### [Important] 하드코딩된 색상 값

- `FeedingScreen.kt` line 203-205: `Color(0xFF4CAF50)`, `Color(0xFFFF6B6B)` - 연결 상태 색상이 Theme에 정의된 `SoftRed`와 별도로 하드코딩됨
- `ShareBottomSheet.kt` line 437: `Color(0xFF4CAF50)` - 동일한 녹색 하드코딩 중복
- `Theme.kt`에 이미 `SoftRed = Color(0xFFFF6B6B)`가 정의되어 있으므로 재사용해야 함

---

## 3. 코드 중복 (3개 탭 간 중복 패턴)

### [Critical] 심각한 구조적 중복

3개 Screen (Feeding, Cleaning, Diaper)에 걸쳐 거의 동일한 구조가 반복됨. 아래는 중복 패턴별 분석:

#### 3-1. 경과 시간 포맷 함수 (3중 중복)

**완전히 동일한 로직**이 세 파일에 각각 존재:

- `FeedingScreen.kt` → `formatElapsedTimeDisplay()`
- `CleaningScreen.kt` → `formatCleaningElapsedTime()`
- `DiaperScreen.kt` → `formatDiaperElapsedTime()`

모두 `elapsedMinutes`를 받아 `"N시간 M분 전"` 형태로 반환. null일 때 문구만 다름.

#### 3-2. 간격 포맷 함수 (3중 중복)

- `FeedingScreen.kt` → `formatIntervalText()`
- `CleaningScreen.kt` → `formatCleaningIntervalText()`
- `DiaperScreen.kt` → `formatDiaperIntervalText()`

**100% 동일한 로직**: `"Nh Mm 간격"` 형태 반환.

#### 3-3. 날짜별 그룹핑 함수 (3중 중복)

- `FeedingScreen.kt` → `groupRecordsByDate()`
- `CleaningScreen.kt` → `groupCleaningRecordsByDate()`
- `DiaperScreen.kt` → `groupDiaperRecordsByDate()`

Record 타입만 다르고 로직은 동일 (오늘/어제/날짜 그룹핑).

#### 3-4. UI 컴포넌트 중복

| 컴포넌트 | Feeding | Cleaning | Diaper |
|---------|---------|----------|--------|
| DateSectionHeader | `DateSectionHeader` | `CleaningDateSectionHeader` | `DiaperDateSectionHeader` |
| StatChip | `StatChip` | `CleaningStatChip` | `DiaperStatChip` |
| ToggleButton | `ToggleButton` | `CleaningToggleButton` | `DiaperToggleButton` |
| BottomActionButton | `BottomActionButton` | `CleaningBottomActionButton` | `DiaperBottomActionButton` |
| DeleteConfirmDialog | `DeleteConfirmDialog` | `CleaningDeleteConfirmDialog` | `DiaperDeleteConfirmDialog` |
| EmptyState | `EmptyState` | `CleaningEmptyState` | `DiaperEmptyState` |
| TimelineRecordRow | `TimelineRecordRow` | `CleaningTimelineRecordRow` | `DiaperTimelineRecordRow` |

위 7개 컴포넌트가 3개 Screen에서 이름만 다르고 **거의 동일한 구현**으로 반복됨.

#### 3-5. ViewModel 중복

3개 ViewModel이 동일한 패턴 반복:
- `ticker` (60초 interval Flow)
- `_refreshTrigger`
- `_lastAddedRecord` / `clearLastAddedRecord()`
- `addRecord()` 디바운싱 로직
- `deleteRecord()` 패턴
- ViewModelProvider.Factory companion object

#### 3-6. DataSource 중복

`FirestoreDataSource`, `CleaningDataSource`, `DiaperDataSource` 3개가 동일한 구조:
- `getAll()`, `getLatest()`, `insert()`, `delete()` 패턴
- Firestore collection 접근 패턴
- DocumentSnapshot -> Record 변환 패턴

### 권장 리팩터링

1. **공통 유틸리티 추출**: `formatElapsedTime()`, `formatIntervalText()`, `groupRecordsByDate()` 등을 `util/TimeUtils.kt`로 추출
2. **공통 UI 컴포넌트 추출**: `DateSectionHeader`, `StatChip`, `ToggleButton`, `BottomActionButton`, `DeleteConfirmDialog`, `EmptyState` 등을 `ui/common/` 패키지로 추출
3. **제네릭 Base DataSource / Repository** 도입 검토
4. **Base ViewModel** 도입으로 ticker, debounce, lastAddedRecord 패턴 통합

---

## 4. 네이밍 일관성

### [Important] DataSource 네이밍 불일치

- Feeding: `FirestoreDataSource` (일반적인 이름)
- Cleaning: `CleaningDataSource` (도메인 접두사)
- Diaper: `DiaperDataSource` (도메인 접두사)

`FirestoreDataSource`만 도메인명이 빠져 있음. `FeedingDataSource`로 변경하거나, 모두 통일해야 함.

### [Suggestion] 함수명 접두사 불일치

- Feeding: `formatElapsedTimeDisplay()`, `formatIntervalText()`, `formatRecordType()`
- Cleaning: `formatCleaningElapsedTime()`, `formatCleaningIntervalText()`, `formatCleaningItemType()`
- Diaper: `formatDiaperElapsedTime()`, `formatDiaperIntervalText()`, `formatDiaperType()`

Feeding 쪽만 도메인 접두사가 없음. private 함수이므로 당장 충돌 위험은 없지만, 공통 모듈로 추출 시 문제가 됨.

### [Suggestion] Record 필드 네이밍 불일치

- `CleaningRecord.itemType` vs `DiaperRecord.type` vs `FeedingRecord.type`

Cleaning만 `itemType`이라는 다른 필드명을 사용. 의미상 모두 "종류"를 나타내므로 통일 권장.

### [Suggestion] 타입 값 매직 스트링

`"breast"`, `"formula"`, `"pumped"`, `"bottle"`, `"pot"`, `"pump"`, `"diaper"`, `"urine"`, `"stool"` 등 문자열 상수가 코드 전반에 하드코딩됨. enum class 또는 sealed class로 정의하면 타입 안전성을 확보할 수 있음.

---

## 5. 에러 핸들링 누락

### [Critical] ViewModel에서 Firestore 예외 미처리

모든 ViewModel의 `addRecord()`, `deleteRecord()`, `updateType()` 등에서 `viewModelScope.launch` 내부에 **try-catch가 전혀 없음**. Firestore 네트워크 오류, 권한 오류 등 발생 시 앱이 크래시되거나 사용자에게 피드백 없이 실패함.

```kotlin
// 현재 코드 (모든 ViewModel 동일)
fun addRecord() {
    viewModelScope.launch {
        val record = repository.addRecord()  // 예외 발생 시 크래시
        ...
    }
}
```

**권장**: 최소한 CoroutineExceptionHandler 또는 try-catch로 감싸고, UI에 에러 상태를 노출해야 함.

### [Critical] AppContainer 익명 로그인 실패 미처리

```kotlin
auth.signInAnonymously().addOnSuccessListener { result ->
    resolveAndInitRepository(result.user!!.uid)
}
```

`addOnFailureListener`가 없어서 익명 로그인 실패 시 앱이 영원히 로딩 상태에 머무름 (`repository`가 null인 채로).

### [Important] GoogleAuthHelper null 강제 언래핑

```kotlin
// GoogleAuthHelper.kt line 61, 66, 72
Result.success(result.user!!.uid)
```

`user`가 null일 가능성에 대한 방어 코드 없음. Firebase 문서에서는 `user`가 null일 수 있다고 명시. NPE 크래시 위험.

### [Important] UserRepository.sharingState() 내부 suspend 호출

```kotlin
fun sharingState(uid: String): Flow<SharingState> {
    return getProfile(uid).map { profile ->
        // ...
        val partnerSnapshot = profileDocRef(partnerUid).get().await()  // Flow 내부 suspend
    }
}
```

`Flow.map` 내부에서 `await()` (suspend 함수)를 호출하고 있음. 기술적으로 동작하지만, Flow 수집이 블로킹될 수 있으며 구조적으로 부적절함. `flatMapLatest` 또는 별도 suspend 함수로 분리 권장.

### [Suggestion] Repository 계층 에러 전파 부재

모든 Repository 함수가 예외를 그대로 throw함. `Result<T>` 래핑이나 sealed class 기반 응답 타입으로 에러를 명시적으로 전파하면 호출부에서 안전하게 처리 가능.

---

## 6. 테스트 부재로 인한 리스크

### [Critical] 테스트 파일 0개

프로젝트에 단위 테스트, UI 테스트, 통합 테스트가 **전혀 존재하지 않음**.

### 리스크 등급별 분류

#### 최고 위험 (버그 발생 시 데이터 손실/손상 가능)

| 영역 | 리스크 내용 |
|------|-----------|
| `UserRepository.redeemInviteCode()` | batch 연산 실패 시 부분 업데이트 가능성. 한쪽만 linkedTo 설정되면 비대칭 상태 발생 |
| `GoogleAuthHelper.handleSignInResult()` | 익명→Google 링크 실패 후 signInWithCredential 경로에서 기존 익명 데이터 유실 |
| `AppContainer.reinitializeWithDataOwner()` | 레이스 컨디션 - 동시 호출 시 StateFlow가 꼬일 수 있음 |

#### 높은 위험 (사용자 경험 저하)

| 영역 | 리스크 내용 |
|------|-----------|
| 3개 ViewModel의 `addRecord()` 디바운싱 | 2초 간격이 하드코딩됨. 빠른 탭 시 기록 누락을 사용자가 인지 못할 수 있음 |
| 시간 포맷 함수들 | 경계값 (0분, 24시간 초과 등) 테스트 없음 |
| 날짜 그룹핑 함수 | 자정 전후, 시간대 변경 등 엣지 케이스 미검증 |
| 초대 코드 생성 (`generateInviteCode`) | 충돌 시 최대 5회 재시도 후 예외. 사용자 경험 고려 필요 |

#### 중간 위험 (기능적 결함 가능)

| 영역 | 리스크 내용 |
|------|-----------|
| TimePicker 시간 수정 | 날짜가 변경되지 않고 시간만 바뀜 → 자정 경계에서 잘못된 timestamp 가능 |
| 초대 코드 유효성 | 6자리 영숫자 입력 검증만 존재. 특수문자 필터링은 `uppercase()`에 의존 |

### 최소 권장 테스트 목록

1. **단위 테스트**: 시간 포맷 함수 (경계값), 날짜 그룹핑, 초대 코드 생성/검증 로직
2. **ViewModel 테스트**: 디바운싱, 상태 전이, 에러 핸들링
3. **Repository 테스트**: Firestore mock 기반 CRUD, batch 연산
4. **통합 테스트**: 공유 플로우 (초대 코드 생성 → 입력 → 연결)
5. **UI 테스트**: 각 Screen의 빈 상태, 기록 추가, 삭제 플로우

---

## 7. 기타 발견 사항

### [Important] 다크 모드 미지원

`Theme.kt`에 `darkTheme: Boolean = false` 파라미터가 있지만, 항상 `LightColorScheme`만 사용. Play Store 심사 시 문제는 아니지만, 사용자 경험 관점에서 다크 모드 미지원은 낮은 평점의 원인이 될 수 있음.

### [Suggestion] ProGuard / R8 난독화 설정 확인 필요

출시 빌드에서 Firebase 관련 클래스가 난독화되면 런타임 크래시 발생 가능. `proguard-rules.pro`에 Firebase 관련 keep 규칙이 있는지 확인 필요.

### [Suggestion] Firestore 오프라인 캐시 전략

현재 `snapshots()` (실시간 리스너)를 사용하므로 오프라인에서도 캐시 데이터를 보여줌. 그러나 오프라인 상태에서 `insert()`, `delete()` 호출 시 Firestore가 로컬 캐시에 기록하고 온라인 복귀 시 동기화하는데, 사용자에게 오프라인 상태를 알리는 UI가 없음.

---

## 요약

| 심각도 | 항목 수 | 대표 이슈 |
|--------|---------|----------|
| **Critical** | 5 | 에러 핸들링 전면 부재, 테스트 0개, 구조적 중복, 익명 로그인 실패 미처리 |
| **Important** | 6 | 데드 코드, 하드코딩 색상, DataSource 네이밍, null 강제 언래핑, Flow 내 suspend |
| **Suggestion** | 6 | i18n, 매직 스트링 enum화, 다크 모드, ProGuard, 오프라인 UI, 함수명 통일 |

### 출시 전 필수 조치 (Critical)

1. **모든 ViewModel의 Firestore 호출에 try-catch 추가** - 네트워크 오류 시 크래시 방지
2. **AppContainer 익명 로그인 실패 처리** - 무한 로딩 방지
3. **GoogleAuthHelper의 `!!` 연산자를 안전한 처리로 교체** - NPE 크래시 방지
4. **MainActivity의 미사용 googleSignInLauncher 제거** - 데드 코드 정리
5. **최소한의 핵심 경로 테스트 작성** - 데이터 손실 시나리오 검증

### 출시 후 우선 리팩터링 권장

1. 3개 탭 간 중복 코드를 공통 모듈로 추출
2. 매직 스트링을 enum/sealed class로 전환
3. UI 문자열을 strings.xml로 이전 (다국어 지원 대비)
