# Baby Feeding Tracker - UX 개선 설계

## 1. Overview

사용자 피드백 기반 두 가지 UX 개선:
1. 경과 시간 표시를 한 줄로 통합
2. 기록 추가 시 바텀시트 자동 오픈 + 선택 후 자동 닫힘

---

## 2. 경과 시간 한 줄 표시

### 현재 동작

```
마지막 수유
2시간 15분    ← displayLarge
전            ← headlineSmall (별도 줄)
```

### 변경 후

```
마지막 수유
2시간 15분 전  ← displayLarge, 한 줄
```

### 구현

- `formatElapsedTimeDisplay()` 반환값에 "전"을 포함: "2시간 15분 전", "방금"
- `ElapsedTimeSection`에서 "전" Text composable 제거

---

## 3. 기록 추가 시 바텀시트 자동 오픈

### 흐름

1. "+ 수유 기록" 버튼 탭 → DB에 기록 추가 → 새 레코드로 바텀시트 자동 오픈
2. 바텀시트에서:
   - "모유" 탭 → 저장 → 바텀시트 즉시 닫힘
   - "분유" 탭 → 용량 버튼 표시 → 용량 선택 → 저장 → 바텀시트 즉시 닫힘

### 기존 동작 유지

- 리스트 항목 탭 → 바텀시트 오픈 (편집 모드) → 자동 닫힘 없음 (여러 번 수정 가능)

### 구현

**ViewModel 변경:**
- `addRecord()`가 새 레코드를 반환하도록 변경. 내부에서 insert 후 해당 레코드를 `_lastAddedRecord: MutableStateFlow<FeedingRecord?>` 에 emit.
- UI에서 `lastAddedRecord`를 collect하여 바텀시트를 자동으로 엶.
- 바텀시트가 열린 후 `clearLastAddedRecord()`로 상태 초기화.

**DAO 변경:**
- `insert()`의 반환 타입을 `Long`으로 변경하여 새 레코드의 ID를 받음.

**바텀시트 변경:**
- `RecordEditBottomSheet`에 `isNewRecord: Boolean` 파라미터 추가.
- `isNewRecord == true`일 때:
  - "모유" 선택 → `onUpdateType` 호출 → `onDismiss` 호출
  - "분유" 선택 → 용량 버튼 표시. 용량 선택 → `onUpdateType` 호출 → `onDismiss` 호출
- `isNewRecord == false`일 때: 기존 동작 유지 (자동 닫힘 없음)

---

## 4. 파일 변경 목록

| 파일 | 변경 내용 |
|------|----------|
| `data/FeedingDao.kt` | `insert` 반환 타입을 `Long`으로 변경 |
| `data/FeedingRepository.kt` | `addRecord`가 `FeedingRecord`를 반환하도록 변경 |
| `ui/MainViewModel.kt` | `_lastAddedRecord` StateFlow 추가, `addRecord` 수정 |
| `ui/MainScreen.kt` | 경과 시간 한 줄 통합, 바텀시트 자동 오픈/닫힘 로직 |

---

## 5. 범위 외

- 리스트 항목 탭 시 편집 모드의 자동 닫힘 변경 없음
- 테마/타임라인 UI 변경 없음
