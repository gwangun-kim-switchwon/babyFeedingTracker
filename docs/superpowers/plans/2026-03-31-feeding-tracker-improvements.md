# Baby Feeding Tracker 기능 개선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 삭제 버그 수정, 수유 종류/용량 기록 기능 추가, 민트 테마 적용, 타임라인 UI 구현

**Architecture:** 기존 단일 화면 MVVM 2레이어 구조 유지. FeedingRecord에 type/amountMl 필드 추가, Room migration으로 기존 데이터 보존. UI는 스와이프 삭제를 제거하고 바텀시트 기반으로 전환.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Coroutines + Flow, 수동 DI

**Spec:** `docs/superpowers/specs/2026-03-31-feeding-tracker-improvements-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `app/src/main/java/com/baby/feedingtracker/data/FeedingRecord.kt` | Modify | `type`, `amountMl` 필드 추가 |
| `app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt` | Modify | `updateTypeAndAmount` 메서드 추가 |
| `app/src/main/java/com/baby/feedingtracker/data/AppDatabase.kt` | Modify | version 2, Migration 추가 |
| `app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt` | Modify | `updateRecord` 메서드 추가 |
| `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt` | Modify | `fallbackToDestructiveMigration` 제거, migration 등록 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` | Modify | `updateRecordType` 메서드 추가 |
| `app/src/main/java/com/baby/feedingtracker/ui/theme/Theme.kt` | Modify | 코랄 → 민트 컬러 교체 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` | Modify | 스와이프 제거, 타임라인 UI, 바텀시트, 수유 종류 표시 |

---

## Task 1: Data Layer — FeedingRecord, DAO, Migration

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingRecord.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt`

- [ ] **Step 1: FeedingRecord에 type, amountMl 필드 추가**

`app/src/main/java/com/baby/feedingtracker/data/FeedingRecord.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String? = null,     // "breast" | "formula" | null
    val amountMl: Int? = null      // 분유일 때만: 60, 80, 100, 120, 140, 160
)
```

- [ ] **Step 2: FeedingDao에 updateTypeAndAmount 메서드 추가**

`app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedingDao {
    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FeedingRecord>>

    @Query("SELECT * FROM feeding_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<FeedingRecord?>

    @Insert
    suspend fun insert(record: FeedingRecord)

    @Delete
    suspend fun delete(record: FeedingRecord)

    @Query("UPDATE feeding_records SET type = :type, amountMl = :amountMl WHERE id = :id")
    suspend fun updateTypeAndAmount(id: Long, type: String?, amountMl: Int?)
}
```

- [ ] **Step 3: AppDatabase version 2 + Migration 추가**

`app/src/main/java/com/baby/feedingtracker/data/AppDatabase.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FeedingRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedingDao(): FeedingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feeding_records ADD COLUMN type TEXT")
                db.execSQL("ALTER TABLE feeding_records ADD COLUMN amountMl INTEGER")
            }
        }
    }
}
```

- [ ] **Step 4: AppContainer에서 migration 등록, fallbackToDestructiveMigration 제거**

`app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.di

import android.content.Context
import androidx.room.Room
import com.baby.feedingtracker.data.AppDatabase
import com.baby.feedingtracker.data.FeedingRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "feeding-db"
    ).addMigrations(AppDatabase.MIGRATION_1_2)
     .build()

    val repository = FeedingRepository(database.feedingDao())
}
```

- [ ] **Step 5: FeedingRepository에 updateRecord 메서드 추가**

`app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

class FeedingRepository(private val dao: FeedingDao) {
    val allRecords: Flow<List<FeedingRecord>> = dao.getAll()
    val latestRecord: Flow<FeedingRecord?> = dao.getLatest()

    suspend fun addRecord() {
        dao.insert(FeedingRecord(timestamp = System.currentTimeMillis()))
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dao.delete(record)
    }

    suspend fun updateRecord(id: Long, type: String?, amountMl: Int?) {
        dao.updateTypeAndAmount(id, type, amountMl)
    }
}
```

- [ ] **Step 6: 빌드 확인**

Run: `cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/baby/feedingtracker/data/ app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt
git commit -m "feat: add type and amountMl fields to FeedingRecord with Room migration"
```

---

## Task 2: ViewModel — updateRecordType 메서드 추가

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`

- [ ] **Step 1: MainViewModel에 updateRecordType 메서드 추가**

`app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`에서 `deleteRecord` 메서드 뒤에 다음을 추가:

```kotlin
fun updateRecordType(recordId: Long, type: String?, amountMl: Int?) {
    viewModelScope.launch {
        repository.updateRecord(recordId, type, amountMl)
    }
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt
git commit -m "feat: add updateRecordType method to MainViewModel"
```

---

## Task 3: Theme — 코랄에서 민트 컬러로 변경

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/theme/Theme.kt`

- [ ] **Step 1: 코랄 컬러를 민트 컬러로 교체**

`app/src/main/java/com/baby/feedingtracker/ui/theme/Theme.kt`에서 Accent / Primary 섹션의 3개 컬러를 변경:

변경 전:
```kotlin
// Accent / Primary
val SoftCoral = Color(0xFFFF8A76)           // 메인 액센트
val SoftCoralLight = Color(0xFFFFB4A2)      // 연한 액센트
val SoftCoralDark = Color(0xFFE8735F)       // 눌림 상태
```

변경 후:
```kotlin
// Accent / Primary
val MintGreen = Color(0xFF8CC9B0)           // 메인 액센트
val MintGreenLight = Color(0xFFB5DFCC)      // 연한 액센트
val MintGreenDark = Color(0xFF6BAF96)       // 눌림 상태
```

- [ ] **Step 2: LightColorScheme에서 참조 변경**

같은 파일에서 `LightColorScheme`의 코랄 참조를 민트로 변경:

변경 전:
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = SoftCoral,
    onPrimary = Color.White,
    primaryContainer = SoftCoralLight,
    onPrimaryContainer = DarkCharcoal,
    secondary = SoftCoralLight,
    onSecondary = DarkCharcoal,
```

변경 후:
```kotlin
private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = Color.White,
    primaryContainer = MintGreenLight,
    onPrimaryContainer = DarkCharcoal,
    secondary = MintGreenLight,
    onSecondary = DarkCharcoal,
```

- [ ] **Step 3: 빌드 확인**

Run: `cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/baby/feedingtracker/ui/theme/Theme.kt
git commit -m "feat: change theme accent color from coral to mint green"
```

---

## Task 4: MainScreen — 스와이프 삭제 제거 + 타임라인 UI + 바텀시트

이 태스크는 MainScreen.kt의 대규모 변경이므로 전체 파일을 교체한다.

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`

- [ ] **Step 1: MainScreen.kt 전체 교체**

`app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }

    // 삭제 확인 다이얼로그
    recordToDelete?.let { record ->
        DeleteConfirmDialog(
            record = record,
            onConfirm = {
                viewModel.deleteRecord(record)
                recordToDelete = null
                selectedRecord = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,
                        extendedColors.gradientBottom
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -- 상단: 경과 시간 영역 --
            ElapsedTimeSection(
                elapsedMinutes = uiState.elapsedMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            )

            // -- 중단: 기록 목록 --
            FeedingRecordList(
                records = uiState.records,
                onRecordClick = { record -> selectedRecord = record },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // -- 하단: 수유 기록 버튼 --
            BottomActionButton(
                onClick = { viewModel.addRecord() },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────
// 경과 시간 섹션
// ──────────────────────────────────────────────

@Composable
private fun ElapsedTimeSection(
    elapsedMinutes: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (elapsedMinutes != null) {
            Text(
                text = "마지막 수유",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = formatElapsedTimeDisplay(elapsedMinutes),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (elapsedMinutes != null) {
            Text(
                text = "전",
                style = MaterialTheme.typography.headlineSmall,
                color = LocalExtendedColors.current.subtleText
            )
        }
    }
}

// ──────────────────────────────────────────────
// 기록 목록 (타임라인)
// ──────────────────────────────────────────────

@Composable
private fun FeedingRecordList(
    records: List<FeedingRecord>,
    onRecordClick: (FeedingRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        val groupedRecords = groupRecordsByDate(records)

        LazyColumn(
            modifier = modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            groupedRecords.forEach { (dateLabel, dayRecords) ->
                item(key = "header_$dateLabel") {
                    DateSectionHeader(dateLabel)
                }
                itemsIndexed(
                    items = dayRecords,
                    key = { _, record -> record.id }
                ) { index, record ->
                    val isLast = index == dayRecords.lastIndex
                    val previousRecord = if (index + 1 < dayRecords.size) dayRecords[index + 1] else null

                    TimelineRecordRow(
                        record = record,
                        intervalMinutes = previousRecord?.let {
                            ((record.timestamp - it.timestamp) / 60_000L)
                        },
                        showLine = !isLast,
                        onClick = { onRecordClick(record) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
// 빈 상태
// ──────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83C\uDF7C",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "아직 기록이 없어요",
                style = MaterialTheme.typography.headlineSmall,
                color = LocalExtendedColors.current.subtleText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "아래 버튼을 눌러\n첫 수유를 기록해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ──────────────────────────────────────────────
// 날짜 섹션 헤더
// ──────────────────────────────────────────────

@Composable
private fun DateSectionHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = LocalExtendedColors.current.subtleText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
    }
}

// ──────────────────────────────────────────────
// 타임라인 기록 행
// ──────────────────────────────────────────────

@Composable
private fun TimelineRecordRow(
    record: FeedingRecord,
    intervalMinutes: Long?,
    showLine: Boolean,
    onClick: () -> Unit
) {
    val dotSize = 10.dp
    val lineWidth = 1.5.dp
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 타임라인 (동그라미 + 세로선)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // 동그라미
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            // 세로선
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(36.dp)
                        .background(accentColor.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 컨텐츠
        Column(
            modifier = Modifier.padding(bottom = if (showLine) 0.dp else 4.dp)
        ) {
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 시간
                Text(
                    text = timeFormat.format(Date(record.timestamp)),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // 수유 종류 표시
                val typeText = formatRecordType(record)
                if (typeText != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalExtendedColors.current.subtleText
                    )
                }
            }

            // 간격 표시
            if (intervalMinutes != null && intervalMinutes > 0) {
                Text(
                    text = formatIntervalText(intervalMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalExtendedColors.current.subtleText.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 바텀시트: 수유 종류/용량 편집
// ──────────────────────────────────────────────

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

// ──────────────────────────────────────────────
// 토글 버튼 (모유/분유)
// ──────────────────────────────────────────────

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ──────────────────────────────────────────────
// 용량 버튼
// ──────────────────────────────────────────────

@Composable
private fun AmountButton(
    amount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            contentPadding = ButtonDefaults.ContentPadding.let {
                androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            },
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                text = "$amount",
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = ButtonDefaults.ContentPadding.let {
                androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            }
        ) {
            Text(
                text = "$amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// ──────────────────────────────────────────────
// 하단 액션 버튼
// ──────────────────────────────────────────────

@Composable
private fun BottomActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "+ 수유 기록",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ──────────────────────────────────────────────
// 삭제 확인 다이얼로그
// ──────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    record: FeedingRecord,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(record.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = "기록 삭제",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "${timeStr} 수유 기록을 삭제할까요?",
                style = MaterialTheme.typography.bodyLarge,
                color = LocalExtendedColors.current.subtleText
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "삭제",
                    color = LocalExtendedColors.current.deleteColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "취소",
                    color = LocalExtendedColors.current.subtleText
                )
            }
        }
    )
}

// ──────────────────────────────────────────────
// 유틸리티 함수
// ──────────────────────────────────────────────

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

private fun formatIntervalText(intervalMinutes: Long): String {
    val hours = intervalMinutes / 60
    val minutes = intervalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m 간격"
        hours > 0 -> "${hours}h 간격"
        else -> "${minutes}m 간격"
    }
}

private fun formatRecordType(record: FeedingRecord): String? {
    return when (record.type) {
        "breast" -> "모유"
        "formula" -> {
            if (record.amountMl != null) "분유 · ${record.amountMl}ml"
            else "분유"
        }
        else -> null
    }
}

private fun groupRecordsByDate(records: List<FeedingRecord>): List<Pair<String, List<FeedingRecord>>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayMillis = today.timeInMillis

    val yesterday = (today.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayMillis = yesterday.timeInMillis

    val dateFormat = SimpleDateFormat("M월 d일", Locale.KOREA)

    return records
        .groupBy { record ->
            when {
                record.timestamp >= todayMillis -> "오늘"
                record.timestamp >= yesterdayMillis -> "어제"
                else -> dateFormat.format(Date(record.timestamp))
            }
        }
        .toList()
}
```

- [ ] **Step 2: 빌드 확인**

Run: `cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt
git commit -m "feat: replace swipe-delete with bottom sheet, add timeline UI and feeding type display"
```

---

## Task 5: 최종 빌드 검증

- [ ] **Step 1: 전체 클린 빌드**

Run: `cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 기능 체크리스트 (수동 확인)**

앱 설치 후 다음을 확인:
1. 수유 기록 버튼 → 리스트에 새 기록 추가됨
2. 기록 항목 탭 → 바텀시트 열림
3. 바텀시트에서 모유/분유 선택 → 리스트에 반영됨
4. 분유 선택 시 용량 버튼 표시 → 선택하면 리스트에 "분유 · 120ml" 형태로 표시
5. 바텀시트 삭제 버튼 → 확인 다이얼로그 → 삭제됨
6. 타임라인 UI: 동그라미 + 세로선이 같은 날짜 내에서 연결됨
7. 민트 컬러 테마 적용됨
8. 기존 데이터가 있는 경우 migration으로 보존됨
