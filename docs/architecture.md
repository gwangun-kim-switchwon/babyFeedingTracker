# Baby Feeding Tracker - Architecture Document

## 1. Overview

신생아 수유 시각을 기록하는 단일 화면 Android 앱.
버튼 하나로 현재 시각을 저장하고, 기록 목록과 마지막 수유 이후 경과 시간을 표시한다.

**설계 원칙**: 극한의 단순함. 과잉 설계를 의도적으로 배제한다.

---

## 2. Tech Stack

| 영역 | 선택 | 비고 |
|------|------|------|
| Language | Kotlin | |
| UI | Jetpack Compose + Material 3 | |
| Local DB | Room | 단일 테이블 |
| Async | Kotlin Coroutines + Flow | |
| DI | 수동 DI (Application-level container) | ADR-001 참조 |
| Architecture | MVVM (ViewModel + Repository) | UseCase 레이어 생략 |
| minSdk / targetSdk | 26 / 34 | |

---

## 3. Architecture

```
┌─────────────────────────────────┐
│           UI Layer              │
│  MainScreen (Compose)           │
│         │                       │
│  MainViewModel                  │
│         │                       │
├─────────────────────────────────┤
│         Data Layer              │
│  FeedingRepository              │
│         │                       │
│  FeedingDao ← Room DB           │
└─────────────────────────────────┘
```

단일 화면, 단일 테이블 앱이므로 **2-레이어 구조**(UI + Data)로 충분하다.
Domain/UseCase 레이어는 비즈니스 로직이 "현재 시각 저장"과 "목록 조회"뿐이므로 생략한다.

---

## 4. Project Folder Structure

```
app/src/main/java/com/baby/feedingtracker/
├── BabyFeedingApp.kt              # Application class (DI container 보유)
├── MainActivity.kt                # Single Activity
├── di/
│   └── AppContainer.kt            # 수동 DI container
├── data/
│   ├── FeedingRecord.kt           # Room @Entity
│   ├── FeedingDao.kt              # Room @Dao
│   ├── AppDatabase.kt             # Room Database
│   └── FeedingRepository.kt       # Repository
└── ui/
    ├── MainScreen.kt              # 단일 Compose 화면
    ├── MainViewModel.kt           # ViewModel
    └── theme/
        └── Theme.kt               # Material 3 Theme
```

**Navigation**: 화면이 1개이므로 Navigation 라이브러리를 사용하지 않는다.
MainActivity가 직접 MainScreen composable을 호스팅한다.

---

## 5. Room Entity & DAO

### 5.1 Entity

```kotlin
@Entity(tableName = "feeding_records")
data class FeedingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long  // System.currentTimeMillis()
)
```

단일 컬럼(`timestamp`)만 저장한다. 수유 종류(모유/분유) 구분은 MVP 범위 밖이다.

### 5.2 DAO

```kotlin
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
}
```

- `getAll()`과 `getLatest()`는 `Flow`로 반환하여 UI가 실시간 반응한다.
- `insert`와 `delete`는 `suspend fun`으로 코루틴에서 호출한다.

### 5.3 Database

```kotlin
@Database(entities = [FeedingRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedingDao(): FeedingDao
}
```

### 5.4 Repository

```kotlin
class FeedingRepository(private val dao: FeedingDao) {
    val allRecords: Flow<List<FeedingRecord>> = dao.getAll()
    val latestRecord: Flow<FeedingRecord?> = dao.getLatest()

    suspend fun addRecord() {
        dao.insert(FeedingRecord(timestamp = System.currentTimeMillis()))
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dao.delete(record)
    }
}
```

---

## 6. Screen & UI 구성

### 단일 화면: MainScreen

```
┌──────────────────────────────┐
│  마지막 수유: 2시간 15분 전     │  ← 실시간 갱신 (1분 주기)
│                              │
│      ┌──────────────┐        │
│      │              │        │
│      │   수유 기록    │        │  ← 큰 원형 버튼
│      │              │        │
│      └──────────────┘        │
│                              │
│  ── 오늘 ──────────────────  │
│  14:30  │              [삭제] │
│  12:15  │              [삭제] │
│  09:00  │              [삭제] │
│  ── 어제 ──────────────────  │
│  23:45  │              [삭제] │
│  ...                         │
└──────────────────────────────┘
```

**UI 상태 모델**:

```kotlin
data class MainUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null  // 마지막 수유 이후 경과 분
)
```

**경과 시간 실시간 갱신**: ViewModel에서 1분 주기 `ticker` Flow를 `latestRecord`와 combine하여 경과 시간을 계산한다.

---

## 7. 수동 DI 설계

```kotlin
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context, AppDatabase::class.java, "feeding-db"
    ).build()

    val repository = FeedingRepository(database.feedingDao())
}
```

Application class에서 `AppContainer`를 생성하고, ViewModel에서 접근한다.

```kotlin
class BabyFeedingApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

ViewModel은 `ViewModelProvider.Factory`를 통해 Repository를 주입받는다.

---

## 8. ADR (Architecture Decision Records)

### ADR-001: Hilt 대신 수동 DI 사용

**상태**: Accepted

**맥락**: 의존성 주입 방식을 결정해야 한다.

**결정**: Hilt를 사용하지 않고, Application 클래스에 `AppContainer`를 두는 수동 DI를 사용한다.

**근거**:
- 주입 대상이 `AppDatabase` -> `FeedingDao` -> `FeedingRepository` -> `MainViewModel` 총 4개뿐이다
- Hilt는 kapt/ksp 빌드 시간 증가, 어노테이션 프로세서 설정, 보일러플레이트(`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, Module 클래스 등)를 요구한다
- 이 규모에서 Hilt의 비용이 이점을 초과한다

**결과**: DI 그래프가 단순하여 유지보수 부담 없음. 앱이 성장하여 화면 3개 이상, 모듈 분리가 필요해지면 Hilt 도입을 재검토한다.

---

### ADR-002: UseCase 레이어 생략

**상태**: Accepted

**맥락**: Clean Architecture의 Domain/UseCase 레이어 도입 여부를 결정해야 한다.

**결정**: UseCase 레이어를 생략하고 ViewModel이 Repository를 직접 호출한다.

**근거**:
- 비즈니스 로직이 `insert(현재시각)`과 `delete(record)` 두 가지뿐이다
- UseCase를 만들면 `AddFeedingRecordUseCase`, `DeleteFeedingRecordUseCase`, `GetFeedingRecordsUseCase` 등 단순 위임 클래스만 늘어난다
- Repository 메서드가 이미 유스케이스 단위로 명명되어 있어 의도가 명확하다

**결과**: 코드량 감소, 탐색 용이. 수유 종류 분기, 알림 로직 등 도메인 규칙이 추가되면 UseCase 도입을 재검토한다.

---

### ADR-003: 단일 화면 + Navigation 미사용

**상태**: Accepted

**맥락**: 화면 구성과 네비게이션 방식을 결정해야 한다.

**결정**: 단일 Activity + 단일 Composable 화면으로 구성하고, Jetpack Navigation 라이브러리를 사용하지 않는다.

**근거**:
- 기능이 "기록/조회/삭제"로 모두 한 화면에 수용 가능하다
- Navigation 라이브러리는 화면 간 이동이 존재할 때 가치가 있다
- 삭제 확인은 AlertDialog로 처리하며, 별도 화면이 아니다

**결과**: 의존성 감소, 구조 단순화. 통계 화면, 설정 화면 등이 추가되면 Navigation 도입을 재검토한다.
