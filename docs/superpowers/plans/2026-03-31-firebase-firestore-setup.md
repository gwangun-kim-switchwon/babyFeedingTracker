# Firebase + Firestore 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Room 기반 데이터 레이어를 Firebase Firestore로 전환하고, 익명 인증을 추가한다.

**Architecture:** Room/KSP 제거, Firebase Auth(익명) + Firestore로 교체. Repository 인터페이스 유지, 내부 구현만 변경. AppContainer에서 인증 후 Repository 초기화, MainActivity에서 로딩 상태 처리.

**Tech Stack:** Firebase BOM 32.7.0, Firebase Auth, Cloud Firestore, Kotlin Coroutines + Flow

**Spec:** `docs/superpowers/specs/2026-03-31-firebase-firestore-setup-design.md`

**Prerequisites:** 사용자가 Firebase Console에서 프로젝트 생성, Android 앱 등록(com.baby.feedingtracker), google-services.json 다운로드, 익명 인증 활성화, Firestore 데이터베이스 생성을 완료해야 함.

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `build.gradle.kts` (project) | Modify | Google services 플러그인 추가 |
| `app/build.gradle.kts` | Modify | Firebase 추가, Room/KSP 제거 |
| `.gitignore` | Create | google-services.json 등 제외 |
| `data/FeedingRecord.kt` | Modify | Room 어노테이션 제거, id Long→String |
| `data/FirestoreDataSource.kt` | Create | Firestore CRUD + Flow |
| `data/FeedingRepository.kt` | Modify | FirestoreDataSource 위임 |
| `di/AppContainer.kt` | Modify | Firebase 인증 + Firestore + repository StateFlow |
| `MainActivity.kt` | Modify | 로딩 상태 처리 |
| `ui/MainViewModel.kt` | Modify | recordId Long→String |
| `ui/MainScreen.kt` | Modify | recordId Long→String 타입 호환 |
| `data/FeedingDao.kt` | Delete | |
| `data/AppDatabase.kt` | Delete | |

---

## Task 1: Build Configuration — Firebase 추가, Room 제거

**Files:**
- Modify: `build.gradle.kts` (project root)
- Modify: `app/build.gradle.kts`
- Create: `.gitignore`

- [ ] **Step 1: project build.gradle.kts에 Google services 플러그인 추가**

`build.gradle.kts` (project root) 전체를 다음으로 교체:

```kotlin
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

KSP 플러그인 제거 (Room만 사용했으므로 더 이상 불필요).

- [ ] **Step 2: app/build.gradle.kts에서 Room 제거, Firebase 추가**

`app/build.gradle.kts` 전체를 다음으로 교체:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.baby.feedingtracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.baby.feedingtracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 3: .gitignore 생성**

프로젝트 루트에 `.gitignore` 파일 생성:

```
# Firebase
app/google-services.json

# Gradle
.gradle/
build/
app/build/
local.properties

# IDE
.idea/
*.iml

# OS
.DS_Store
```

- [ ] **Step 4: google-services.json 존재 확인**

Run: `ls /Users/seonhoappa/projects/baby-feeding-tracker/app/google-services.json`

이 파일이 없으면 사용자에게 Firebase Console에서 다운로드하여 `app/` 디렉토리에 배치하도록 안내. 파일이 있어야 빌드가 성공함.

- [ ] **Step 5: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add build.gradle.kts app/build.gradle.kts .gitignore && git commit -m "build: add Firebase dependencies, remove Room/KSP"
```

---

## Task 2: Data Model — FeedingRecord Room 어노테이션 제거 + id String 변경

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingRecord.kt`
- Delete: `app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt`
- Delete: `app/src/main/java/com/baby/feedingtracker/data/AppDatabase.kt`

- [ ] **Step 1: FeedingRecord.kt 교체**

`app/src/main/java/com/baby/feedingtracker/data/FeedingRecord.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

data class FeedingRecord(
    val id: String = "",
    val timestamp: Long = 0L,
    val type: String? = null,      // "breast" | "formula" | null
    val amountMl: Int? = null       // 분유일 때만: 60, 80, 100, 120, 140, 160
)
```

- [ ] **Step 2: FeedingDao.kt 삭제**

```bash
rm /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/data/FeedingDao.kt
```

- [ ] **Step 3: AppDatabase.kt 삭제**

```bash
rm /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/data/AppDatabase.kt
```

- [ ] **Step 4: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add -A app/src/main/java/com/baby/feedingtracker/data/ && git commit -m "feat: remove Room, change FeedingRecord id to String for Firestore"
```

---

## Task 3: FirestoreDataSource — Firestore CRUD + Flow

**Files:**
- Create: `app/src/main/java/com/baby/feedingtracker/data/FirestoreDataSource.kt`

- [ ] **Step 1: FirestoreDataSource.kt 생성**

`app/src/main/java/com/baby/feedingtracker/data/FirestoreDataSource.kt` 생성:

```kotlin
package com.baby.feedingtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreDataSource(
    private val firestore: FirebaseFirestore,
    private val uid: String
) {
    private val recordsCollection
        get() = firestore.collection("users").document(uid).collection("feeding_records")

    fun getAll(): Flow<List<FeedingRecord>> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    FeedingRecord(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        type = doc.getString("type"),
                        amountMl = doc.getLong("amountMl")?.toInt()
                    )
                }
            }
    }

    fun getLatest(): Flow<FeedingRecord?> {
        return recordsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.let { doc ->
                    FeedingRecord(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        type = doc.getString("type"),
                        amountMl = doc.getLong("amountMl")?.toInt()
                    )
                }
            }
    }

    suspend fun insert(record: FeedingRecord): String {
        val data = hashMapOf(
            "timestamp" to record.timestamp,
            "type" to record.type,
            "amountMl" to record.amountMl,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        val docRef = recordsCollection.add(data).await()
        return docRef.id
    }

    suspend fun delete(recordId: String) {
        recordsCollection.document(recordId).delete().await()
    }

    suspend fun updateTypeAndAmount(recordId: String, type: String?, amountMl: Int?) {
        recordsCollection.document(recordId).update(
            mapOf(
                "type" to type,
                "amountMl" to amountMl
            )
        ).await()
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/data/FirestoreDataSource.kt && git commit -m "feat: add FirestoreDataSource with CRUD and Flow support"
```

---

## Task 4: Repository + DI — Firestore 위임 + 익명 인증

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt`

- [ ] **Step 1: FeedingRepository.kt 교체**

`app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.data

import kotlinx.coroutines.flow.Flow

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

- [ ] **Step 2: AppContainer.kt 교체**

`app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker.di

import android.content.Context
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.FirestoreDataSource
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppContainer(context: Context) {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            initRepository(currentUser.uid)
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

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/data/FeedingRepository.kt app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt && git commit -m "feat: wire FeedingRepository to FirestoreDataSource with anonymous auth"
```

---

## Task 5: MainActivity — 로딩 상태 처리

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/MainActivity.kt`

- [ ] **Step 1: MainActivity.kt 교체**

`app/src/main/java/com/baby/feedingtracker/MainActivity.kt` 전체를 다음으로 교체:

```kotlin
package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        setContent {
            BabyFeedingTrackerTheme {
                val repository by app.container.repository.collectAsState()

                if (repository != null) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.factory(repository!!)
                    )
                    MainScreen(viewModel = viewModel)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/MainActivity.kt && git commit -m "feat: add loading state while Firebase auth initializes"
```

---

## Task 6: ViewModel + MainScreen — recordId Long → String

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`
- Modify: `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`

- [ ] **Step 1: MainViewModel.kt — updateRecordType 파라미터 Long→String**

`app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`에서 `updateRecordType` 메서드를 변경:

변경 전:
```kotlin
    fun updateRecordType(recordId: Long, type: String?, amountMl: Int?) {
```

변경 후:
```kotlin
    fun updateRecordType(recordId: String, type: String?, amountMl: Int?) {
```

- [ ] **Step 2: MainScreen.kt — LazyColumn key 확인**

`app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`에서 `itemsIndexed`의 key 확인:

현재:
```kotlin
                    key = { _, record -> record.id }
```

`record.id`가 String이 되었으므로 LazyColumn key로 그대로 사용 가능. **변경 불필요**.

바텀시트에서 `viewModel.updateRecordType(record.id, type, amountMl)` 호출부도 `record.id`가 String이므로 자동 호환. **변경 불필요**.

- [ ] **Step 3: 빌드 확인**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (google-services.json이 있는 경우)

- [ ] **Step 4: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt && git commit -m "feat: change recordId type from Long to String for Firestore"
```

---

## Task 7: INTERNET 퍼미션 확인 + 최종 검증

**Files:**
- Check: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: AndroidManifest.xml에 INTERNET 퍼미션 확인**

Firebase는 INTERNET 퍼미션이 필요하다. `app/src/main/AndroidManifest.xml`을 확인:

현재 INTERNET 퍼미션이 없으면 추가:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".BabyFeedingApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.Light.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 2: 전체 클린 빌드**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/AndroidManifest.xml && git commit -m "feat: add INTERNET permission for Firebase"
```

- [ ] **Step 4: 기능 체크리스트 (수동 확인)**

앱 설치 후 다음을 확인:
1. 앱 시작 시 로딩 표시 후 메인 화면 전환
2. 수유 기록 버튼 → 기록 추가됨 → 바텀시트 자동 오픈
3. 모유/분유 선택, 용량 선택 동작
4. 기록 삭제 동작
5. 앱 종료 후 재시작 → 기록 유지됨
6. Firebase Console → Firestore에서 데이터 확인 가능
