# UX 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 경과 시간 한 줄 표시 + 기록 추가 시 바텀시트 자동 오픈/닫힘

**Architecture:** DAO insert 반환 타입을 Long으로 변경, ViewModel에 lastAddedRecord StateFlow 추가, MainScreen에서 자동 오픈/닫힘 로직 구현. 기존 편집 모드는 변경 없음.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Coroutines + Flow

**Spec:** `docs/superpowers/specs/2026-03-31-ux-improvements-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt` | Modify | `insert` 반환 타입을 `Long`으로 변경 |
| `app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt` | Modify | `addRecord`가 `FeedingRecord`를 반환 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` | Modify | `_lastAddedRecord` StateFlow 추가, `addRecord` 수정 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` | Modify | 경과 시간 한 줄, 바텀시트 자동 오픈/닫힘 |

---

## Task 1: Data Layer — insert 반환 타입 변경

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt:17-18`
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt:9-11`

- [ ] **Step 1: FeedingDao.insert 반환 타입을 Long으로 변경**

`app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt`에서 insert 메서드를 변경:

변경 전:
```kotlin
    @Insert
    suspend fun insert(record: FeedingRecord)
```

변경 후:
```kotlin
    @Insert
    suspend fun insert(record: FeedingRecord): Long
```

- [ ] **Step 2: FeedingRepository.addRecord가 FeedingRecord를 반환하도록 변경**

`app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt`에서 addRecord 메서드를 변경:

변경 전:
```kotlin
    suspend fun addRecord() {
        dao.insert(FeedingRecord(timestamp = System.currentTimeMillis()))
    }
```

변경 후:
```kotlin
    suspend fun addRecord(): FeedingRecord {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        val id = dao.insert(record)
        return record.copy(id = id)
    }
```

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt && git commit -m "feat: return inserted record from addRecord"
```

---

## Task 2: ViewModel — lastAddedRecord StateFlow

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`

- [ ] **Step 1: _lastAddedRecord StateFlow 추가 및 addRecord 수정**

`app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null
)

class MainViewModel(private val repository: FeedingRepository) : ViewModel() {

    // 1분마다 tick을 발행하여 경과 시간 갱신을 트리거
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<MainUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let {
            (now - it.timestamp) / 60_000L
        }
        MainUiState(
            records = records,
            elapsedMinutes = elapsed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    // 새로 추가된 레코드 (바텀시트 자동 오픈용)
    private val _lastAddedRecord = MutableStateFlow<FeedingRecord?>(null)
    val lastAddedRecord: StateFlow<FeedingRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() {
        _lastAddedRecord.value = null
    }

    // 연타 방지
    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            val record = repository.addRecord()
            _refreshTrigger.value = now
            _lastAddedRecord.value = record
        }
    }

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun updateRecordType(recordId: Long, type: String?, amountMl: Int?) {
        viewModelScope.launch {
            repository.updateRecord(recordId, type, amountMl)
        }
    }

    companion object {
        fun factory(repository: FeedingRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
        }
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt && git commit -m "feat: add lastAddedRecord StateFlow for auto-open bottom sheet"
```

---

## Task 3: MainScreen — 경과 시간 한 줄 + 바텀시트 자동 오픈/닫힘

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`

이 태스크는 3개의 독립적인 변경:
1. `formatElapsedTimeDisplay()`에 "전" 포함
2. `ElapsedTimeSection`에서 "전" Text 제거
3. `MainScreen`에서 `lastAddedRecord` 관찰 + 바텀시트 자동 오픈/닫힘

- [ ] **Step 1: formatElapsedTimeDisplay에 "전" 포함**

`app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`의 `formatElapsedTimeDisplay` 함수(line 675)를 변경:

변경 전:
```kotlin
private fun formatElapsedTimeDisplay(elapsedMinutes: Long?): String {
    if (elapsedMinutes == null) return "첫 수유를\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        minutes > 0 -> "${minutes}분"
        else -> "방금"
    }
}
```

변경 후:
```kotlin
private fun formatElapsedTimeDisplay(elapsedMinutes: Long?): String {
    if (elapsedMinutes == null) return "첫 수유를\n기록해보세요"
    val hours = elapsedMinutes / 60
    val minutes = elapsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
        else -> "방금"
    }
}
```

- [ ] **Step 2: ElapsedTimeSection에서 "전" Text 제거**

같은 파일의 `ElapsedTimeSection` 함수(line 142)에서 "전" 텍스트 블록을 제거:

삭제할 부분 (line 169-175):
```kotlin
        if (elapsedMinutes != null) {
            Text(
                text = "전",
                style = MaterialTheme.typography.headlineSmall,
                color = LocalExtendedColors.current.subtleText
            )
        }
```

이 블록을 완전히 삭제합니다.

- [ ] **Step 3: MainScreen에서 lastAddedRecord 관찰 + 바텀시트에 isNewRecord 전달**

`MainScreen` composable 함수 상단(line 57~)을 변경합니다.

변경 전 (line 60-63):
```kotlin
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }
```

변경 후:
```kotlin
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }

    // 새 기록 추가 시 바텀시트 자동 오픈
    LaunchedEffect(lastAddedRecord) {
        lastAddedRecord?.let { record ->
            selectedRecord = record
            isNewRecord = true
            viewModel.clearLastAddedRecord()
        }
    }
```

이를 위해 import도 추가:
```kotlin
import androidx.compose.runtime.LaunchedEffect
```

- [ ] **Step 4: 바텀시트 호출부에 isNewRecord 전달**

`MainScreen`에서 `RecordEditBottomSheet` 호출 부분(line 78~)을 변경:

변경 전:
```kotlin
    // 바텀시트
    selectedRecord?.let { record ->
        RecordEditBottomSheet(
            record = record,
            onUpdateType = { type, amountMl ->
                viewModel.updateRecordType(record.id, type, amountMl)
            },
            onDelete = {
                recordToDelete = record
            },
            onDismiss = { selectedRecord = null }
        )
    }
```

변경 후:
```kotlin
    // 바텀시트
    selectedRecord?.let { record ->
        RecordEditBottomSheet(
            record = record,
            isNewRecord = isNewRecord,
            onUpdateType = { type, amountMl ->
                viewModel.updateRecordType(record.id, type, amountMl)
            },
            onDelete = {
                recordToDelete = record
            },
            onDismiss = {
                selectedRecord = null
                isNewRecord = false
            }
        )
    }
```

리스트 항목 탭 시에는 `isNewRecord`가 `false`인 상태로 유지되므로 기존 편집 동작 그대로입니다.

- [ ] **Step 5: RecordEditBottomSheet에 isNewRecord 파라미터 추가 + 자동 닫힘 로직**

`RecordEditBottomSheet` 함수(line 391~)를 변경합니다.

변경 전:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEditBottomSheet(
    record: FeedingRecord,
    onUpdateType: (type: String?, amountMl: Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedType by remember { mutableStateOf(record.type) }
    var selectedAmount by remember { mutableStateOf(record.amountMl) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val amounts = listOf(60, 80, 100, 120, 140, 160)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 시간 표시
            Text(
                text = "${timeFormat.format(Date(record.timestamp))} 수유 기록",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 모유 / 분유 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleButton(
                    text = "모유",
                    selected = selectedType == "breast",
                    onClick = {
                        val newType = if (selectedType == "breast") null else "breast"
                        selectedType = newType
                        selectedAmount = null
                        onUpdateType(newType, null)
                    },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    text = "분유",
                    selected = selectedType == "formula",
                    onClick = {
                        val newType = if (selectedType == "formula") null else "formula"
                        selectedType = newType
                        if (newType != "formula") selectedAmount = null
                        onUpdateType(newType, if (newType == "formula") selectedAmount else null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 분유 용량 선택 (분유 선택 시만 표시)
            AnimatedVisibility(visible = selectedType == "formula") {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "용량",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amounts.forEach { amount ->
                            AmountButton(
                                amount = amount,
                                selected = selectedAmount == amount,
                                onClick = {
                                    val newAmount = if (selectedAmount == amount) null else amount
                                    selectedAmount = newAmount
                                    onUpdateType(selectedType, newAmount)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 삭제 버튼
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "삭제",
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

변경 후:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordEditBottomSheet(
    record: FeedingRecord,
    isNewRecord: Boolean,
    onUpdateType: (type: String?, amountMl: Int?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedType by remember { mutableStateOf(record.type) }
    var selectedAmount by remember { mutableStateOf(record.amountMl) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val amounts = listOf(60, 80, 100, 120, 140, 160)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 시간 표시
            Text(
                text = "${timeFormat.format(Date(record.timestamp))} 수유 기록",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 모유 / 분유 토글
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleButton(
                    text = "모유",
                    selected = selectedType == "breast",
                    onClick = {
                        val newType = if (selectedType == "breast") null else "breast"
                        selectedType = newType
                        selectedAmount = null
                        onUpdateType(newType, null)
                        if (isNewRecord && newType == "breast") onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    text = "분유",
                    selected = selectedType == "formula",
                    onClick = {
                        val newType = if (selectedType == "formula") null else "formula"
                        selectedType = newType
                        if (newType != "formula") selectedAmount = null
                        onUpdateType(newType, if (newType == "formula") selectedAmount else null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 분유 용량 선택 (분유 선택 시만 표시)
            AnimatedVisibility(visible = selectedType == "formula") {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "용량",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        amounts.forEach { amount ->
                            AmountButton(
                                amount = amount,
                                selected = selectedAmount == amount,
                                onClick = {
                                    val newAmount = if (selectedAmount == amount) null else amount
                                    selectedAmount = newAmount
                                    onUpdateType(selectedType, newAmount)
                                    if (isNewRecord && newAmount != null) onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 삭제 버튼
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "삭제",
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

핵심 변경 2곳:
- "모유" onClick 마지막에: `if (isNewRecord && newType == "breast") onDismiss()`
- 용량 버튼 onClick 마지막에: `if (isNewRecord && newAmount != null) onDismiss()`

- [ ] **Step 6: 빌드 확인**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt && git commit -m "feat: elapsed time one-line display, auto-open/close bottom sheet on new record"
```
