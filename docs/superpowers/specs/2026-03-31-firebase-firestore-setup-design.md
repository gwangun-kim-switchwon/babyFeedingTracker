# Baby Feeding Tracker - Firebase + Firestore 전환 설계

## 1. Overview

Sub-project 1: 기존 Room 기반 데이터 레이어를 Firebase Firestore로 전환하고, 익명 인증을 추가한다.
Sub-project 2(이후 별도 설계)에서 공유 기능을 붙이기 위한 기반 작업.

**설계 원칙**: 기존 UI와 ViewModel은 최대한 유지. Repository 인터페이스 불변, 내부 구현만 교체.

---

## 2. Firestore 데이터 구조

```
users/{uid}/
  feeding_records/{recordId}
    - timestamp: Long
    - type: String?        // "breast" | "formula" | null
    - amountMl: Int?       // 60, 80, 100, 120, 140, 160
    - createdAt: Timestamp // Firestore 서버 시간 (정렬/충돌 해결용)
```

- 익명 로그인 시 발급되는 `uid`가 루트 경로.
- 나중에 공유 시 파트너와 같은 경로를 바라보게 됨.
- `recordId`는 Firestore 자동 생성 ID 사용.

---

## 3. 아키텍처

### 변경 전

```
MainScreen → MainViewModel → FeedingRepository → FeedingDao (Room)
```

### 변경 후

```
MainScreen → MainViewModel → FeedingRepository → FirestoreDataSource
```

- `FeedingRepository`의 메서드 시그니처 유지. 내부 구현만 Room → Firestore로 교체.
- `FeedingDao`, `AppDatabase` 삭제. Room 의존성 제거.
- `FirestoreDataSource`: Firestore 컬렉션에서 snapshotFlow를 통해 `Flow<List<FeedingRecord>>` 등을 반환.
- `AppContainer`에서 FirebaseAuth + FirebaseFirestore 인스턴스를 생성하고 주입.
- Firestore 오프라인 퍼시스턴스는 Android에서 기본 활성화이므로 별도 설정 불필요.

---

## 4. FeedingRecord 모델 변경

Room 어노테이션을 제거하고, Firestore 문서 매핑용으로 변경:

```kotlin
data class FeedingRecord(
    val id: String = "",           // Firestore document ID (Long → String)
    val timestamp: Long = 0L,
    val type: String? = null,
    val amountMl: Int? = null
)
```

- `id` 타입이 `Long` → `String`으로 변경 (Firestore 문서 ID는 문자열).
- `@Entity`, `@PrimaryKey` 등 Room 어노테이션 제거.
- 기본값 추가 (Firestore deserialization에 필요).

**영향 범위**: `id`를 참조하는 ViewModel, MainScreen의 `record.id` 호출부. `Long` → `String` 타입 변경 반영 필요.

---

## 5. FirestoreDataSource

```kotlin
class FirestoreDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val recordsCollection
        get() = firestore.collection("users").document(uid).collection("feeding_records")

    fun getAll(): Flow<List<FeedingRecord>>
    fun getLatest(): Flow<FeedingRecord?>
    suspend fun insert(record: FeedingRecord): String   // 반환: document ID
    suspend fun delete(recordId: String)
    suspend fun updateTypeAndAmount(recordId: String, type: String?, amountMl: Int?)
}
```

- `getAll()`: `snapshotFlow`로 실시간 업데이트. timestamp DESC 정렬.
- `getLatest()`: `getAll()`에서 첫 번째 항목을 map으로 추출. 또는 별도 쿼리(limit 1).
- `insert()`: `add()`로 문서 추가 후 생성된 ID 반환.
- `delete()`: `document(recordId).delete()`.
- `updateTypeAndAmount()`: `document(recordId).update()`.

---

## 6. FeedingRepository 변경

인터페이스 유지, 내부를 FirestoreDataSource로 위임:

```kotlin
class FeedingRepository(private val dataSource: FirestoreDataSource) {
    val allRecords: Flow<List<FeedingRecord>> = dataSource.getAll()
    val latestRecord: Flow<FeedingRecord?> = dataSource.getLatest()

    suspend fun addRecord(): FeedingRecord {
        val timestamp = System.currentTimeMillis()
        val record = FeedingRecord(timestamp = timestamp)
        val id = dataSource.insert(record)
        return record.copy(id = id)
    }

    suspend fun deleteRecord(record: FeedingRecord) {
        dataSource.delete(record.id)
    }

    suspend fun updateRecord(id: String, type: String?, amountMl: Int?) {
        dataSource.updateTypeAndAmount(id, type, amountMl)
    }
}
```

- `updateRecord`의 `id` 파라미터 타입: `Long` → `String`.

---

## 7. 앱 시작 흐름 + 익명 인증

### BabyFeedingApp

```kotlin
class BabyFeedingApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

### AppContainer

```kotlin
class AppContainer(context: Context) {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    init {
        // 익명 로그인
        if (auth.currentUser != null) {
            initRepository(auth.currentUser!!.uid)
        } else {
            auth.signInAnonymously().addOnSuccessListener { result ->
                initRepository(result.user!!.uid)
            }
        }
    }

    private fun initRepository(uid: String) {
        val dataSource = FirestoreDataSource(firestore, uid)
        _repository.value = FeedingRepository(dataSource)
    }
}
```

- `repository`가 nullable StateFlow. 인증 완료 전에는 null.
- 이미 익명 로그인된 경우(앱 재시작) 즉시 초기화.

### MainActivity — 로딩 상태

```kotlin
setContent {
    BabyFeedingTrackerTheme {
        val repository by app.container.repository.collectAsState()
        if (repository != null) {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(repository!!)
            )
            MainScreen(viewModel = viewModel)
        } else {
            // 간단한 로딩 표시
            LoadingScreen()
        }
    }
}
```

---

## 8. ViewModel 변경

- `updateRecordType(recordId: Long, ...)` → `updateRecordType(recordId: String, ...)`
- `deleteRecord(record: FeedingRecord)` — 변경 없음 (record.id가 String으로 바뀌지만 시그니처는 동일)
- 나머지 로직 변경 없음.

---

## 9. MainScreen 변경

- `record.id` 참조하는 곳의 타입이 Long → String. 바텀시트 호출부: `viewModel.updateRecordType(record.id, type, amountMl)` — record.id가 String이 되므로 자연스럽게 호환.
- LazyColumn의 `key`가 `record.id` (String) — 정상 동작.
- 그 외 UI 변경 없음.

---

## 10. 의존성 변경

### 추가

```kotlin
// project build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// app/build.gradle.kts
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
}
```

### 제거

```kotlin
// app/build.gradle.kts에서 삭제
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

KSP 플러그인도 Room 외에 사용하지 않으면 제거.

---

## 11. google-services.json

Firebase 콘솔에서 Android 앱을 등록하고 `google-services.json`을 다운로드하여 `app/` 디렉토리에 배치해야 함. 이 파일은 사용자가 직접 생성해야 하며, git에 커밋하지 않는다 (.gitignore에 추가).

---

## 12. 파일 변경 목록

| 파일 | Action | 내용 |
|------|--------|------|
| `build.gradle.kts` (project) | Modify | Google services 플러그인 추가 |
| `app/build.gradle.kts` | Modify | Firebase 추가, Room/KSP 제거 |
| `app/google-services.json` | Create | 사용자가 Firebase 콘솔에서 다운로드 |
| `.gitignore` | Create | google-services.json 제외 |
| `data/FeedingRecord.kt` | Modify | Room 어노테이션 제거, id를 String으로 변경 |
| `data/FirestoreDataSource.kt` | Create | Firestore CRUD + Flow |
| `data/FeedingRepository.kt` | Modify | FirestoreDataSource 위임 |
| `di/AppContainer.kt` | Modify | FirebaseAuth + Firestore, 익명 로그인, repository StateFlow |
| `BabyFeedingApp.kt` | Modify | 변경 없음 (기존 구조 유지) |
| `MainActivity.kt` | Modify | 로딩 상태 처리 |
| `ui/MainViewModel.kt` | Modify | recordId Long → String |
| `ui/MainScreen.kt` | Modify | 최소 변경 (타입 호환) |
| `data/FeedingDao.kt` | Delete | |
| `data/AppDatabase.kt` | Delete | |

---

## 13. 범위 외

- Google 로그인 (Sub-project 2)
- 초대 코드/공유 기능 (Sub-project 2)
- 공유 상태 UI (Sub-project 2)
- 기존 Room 데이터 마이그레이션 (불필요 — 미출시 앱)
- 다크 모드, 통계, 알림
