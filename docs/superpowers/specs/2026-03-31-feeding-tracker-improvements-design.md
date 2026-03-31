# Baby Feeding Tracker - 기능 개선 설계

## 1. Overview

기존 Baby Feeding Tracker 앱에 대한 세 가지 개선:
1. 삭제 기능 버그 수정
2. 수유 종류(모유/분유) 및 분유 용량 기록 기능 추가
3. 테마 컬러 변경 (코랄 → 민트/연두)
4. 리스트 타임라인 UI 적용

**설계 원칙**: 기존 단일 화면 MVVM 구조를 유지하며 최소 변경으로 구현한다.

---

## 2. 데이터 모델 변경

### 2.1 FeedingRecord Entity 수정

```kotlin
@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String? = null,     // "breast" | "formula" | null(미입력)
    val amountMl: Int? = null      // 분유일 때만: 60, 80, 100, 120, 140, 160
)
```

- `type`과 `amountMl`은 nullable. 기록 버튼을 누르면 `null`로 저장되고, 이후 탭하여 입력.
- `amountMl`은 `type == "formula"`일 때만 유효.

### 2.2 Room Migration (version 1 → 2)

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE feeding_records ADD COLUMN type TEXT")
        db.execSQL("ALTER TABLE feeding_records ADD COLUMN amountMl INTEGER")
    }
}
```

### 2.3 DAO 변경

기존 메서드에 추가:

```kotlin
@Query("UPDATE feeding_records SET type = :type, amountMl = :amountMl WHERE id = :id")
suspend fun updateTypeAndAmount(id: Long, type: String?, amountMl: Int?)
```

### 2.4 Repository 변경

```kotlin
suspend fun updateRecord(id: Long, type: String?, amountMl: Int?) {
    dao.updateTypeAndAmount(id, type, amountMl)
}
```

---

## 3. 삭제 기능 수정

### 현재 문제

`SwipeableRecordRow`의 `confirmValueChange`가 항상 `false`를 반환하여 스와이프가 `DismissedToStart` 상태에 도달하지 못함.

### 해결 방안

스와이프 삭제(`SwipeToDismiss`)를 제거하고, 항목 탭 시 열리는 바텀시트 내에 삭제 버튼을 배치한다.

- 기존 `SwipeableRecordRow` → 단순 `RecordRow`로 변경 (클릭 가능)
- 클릭 시 바텀시트 오픈
- 바텀시트 하단에 삭제 버튼 배치
- 삭제 탭 시 기존 `DeleteConfirmDialog` 표시

이유: 항목 탭으로 바텀시트(수유 종류/용량 편집)를 열기로 했으므로, 삭제도 바텀시트 안에서 처리하는 것이 UX상 일관적.

---

## 4. 바텀시트 UI

리스트 항목을 탭하면 `ModalBottomSheet`가 열린다.

### 레이아웃

```
┌──────────────────────────────┐
│        14:30 수유 기록         │  ← 시간 표시
│                              │
│   [ 모유 ]     [ 분유 ]       │  ← 토글 버튼 2개 (단일 선택)
│                              │
│   (분유 선택 시 아래 노출)      │
│   [60] [80] [100] [120] [140] [160] ml  │  ← Row 버튼
│                              │
│   ┌─────────────────────┐    │
│   │       삭제           │    │  ← 빨간 텍스트 버튼
│   └─────────────────────┘    │
└──────────────────────────────┘
```

### 동작

- **모유/분유 토글**: 하나를 선택하면 다른 하나는 해제. 이미 선택된 것을 다시 누르면 해제(null).
- **용량 버튼**: `type == "formula"`일 때만 표시 (AnimatedVisibility). 6개 버튼(60, 80, 100, 120, 140, 160ml)을 단순 Row로 나열. 선택된 버튼은 primary 컬러로 하이라이트.
- **자동 저장**: 모유/분유 선택 또는 용량 선택 시 즉시 DB에 반영. 별도 저장 버튼 없음.
- **삭제**: 하단 빨간 텍스트 버튼. 탭 시 `DeleteConfirmDialog` 표시. 확인 시 삭제 후 바텀시트 닫힘.

---

## 5. 리스트 타임라인 UI

기존 단순 리스트를 타임라인 스타일로 변경한다.

### 레이아웃

```
── 오늘 ──────────────────
   ●  14:30  모유
   │
   ●  12:15  분유 · 120ml
   │
   ●  09:00
── 어제 ──────────────────
   ●  23:45  분유 · 80ml
   │
   ●  21:00  모유
```

### 구현

- 각 항목 왼쪽에 동그라미(●) 표시. 색상은 민트 액센트 컬러.
- 같은 날짜 내 항목 사이에 세로선(│)으로 연결. 세로선은 동그라미 중심에서 시작/종료.
- 날짜가 바뀌면 세로선 끊김.
- 마지막 항목 아래에는 세로선 없음.
- 수유 종류 표시: 모유 → "모유", 분유 → "분유 · 120ml", 미입력 → 시간만 표시.
- 간격 표시(2h 30m 간격)는 기존대로 유지.

---

## 6. 테마 변경

### 컬러 팔레트 변경

기존 코랄(Coral) 계열 액센트를 민트/연두 계열로 교체:

| 역할 | 기존 | 변경 |
|------|------|------|
| Primary (액센트) | `#FF8A76` (SoftCoral) | `#8CC9B0` (MintGreen) |
| Primary Light | `#FFB4A2` (SoftCoralLight) | `#B5DFCC` (MintGreenLight) |
| Primary Dark | `#E8735F` (SoftCoralDark) | `#6BAF96` (MintGreenDark) |

### 유지 항목

- 배경색(CreamWhite, WarmWhite, SoftBeige): 유지. 그라데이션에 살짝 민트 톤 가미.
- 텍스트 컬러(DarkCharcoal, WarmGray): 유지.
- 삭제 컬러(SoftRed): 유지.
- Typography: 변경 없음.

---

## 7. ViewModel 변경

### MainUiState 수정

변경 없음. 기존 `MainUiState`의 `records: List<FeedingRecord>`에 이미 새 필드가 포함됨.

### 새 메서드 추가

```kotlin
fun updateRecordType(recordId: Long, type: String?, amountMl: Int?) {
    viewModelScope.launch {
        repository.updateRecord(recordId, type, amountMl)
    }
}
```

### 기존 메서드

- `addRecord()`: 변경 없음 (timestamp만으로 기록 생성)
- `deleteRecord()`: 변경 없음

---

## 8. 파일 변경 목록

| 파일 | 변경 내용 |
|------|----------|
| `data/FeedingRecord.kt` | `type`, `amountMl` 필드 추가 |
| `data/AppDatabase.kt` | version 2, MIGRATION_1_2 추가 |
| `data/FeedingDao.kt` | `updateTypeAndAmount` 메서드 추가 |
| `data/FeedingRepository.kt` | `updateRecord` 메서드 추가 |
| `ui/MainViewModel.kt` | `updateRecordType` 메서드 추가 |
| `ui/MainScreen.kt` | 스와이프 삭제 제거, 항목 탭 → 바텀시트, 타임라인 UI, 수유 종류 표시 |
| `ui/theme/Theme.kt` | 코랄 → 민트 컬러 교체 |

### 새 파일 없음

바텀시트는 `MainScreen.kt` 내 private composable로 구현. 단일 화면 앱이므로 별도 파일 불필요.

---

## 9. 범위 외 (Out of Scope)

- 다크 모드
- Navigation 도입
- 알림/리마인더
- 통계/차트
- 멀티 아기 지원
