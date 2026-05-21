# 맘마미아 v2.0 — 설계 문서

**날짜:** 2026-05-01  
**버전:** v2.0 (현재 v1.1.2 기준)  
**범위:** 세척 탭 제거 · 성장 기록 탭 신설 · 홈 화면 위젯

---

## 1. 변경 개요

| 항목 | 내용 |
|------|------|
| 세척 탭 제거 | UI(탭·화면·네비게이션)만 제거. 데이터 레이어(CleaningDataSource/Repository) 코드는 유지, Firestore 데이터도 삭제하지 않음 |
| 성장 기록 탭 신설 | 세척 탭 자리(탭 4번)에 성장 기록 탭 삽입. 생후 배너 + 요약 카드 + 타임라인 + FAB |
| 홈 화면 위젯 | Glance API 4×1 위젯. 마지막 수유 경과 시간만 심플하게 표시 |
| 푸시 알림 | 이번 버전에서 제외 |

---

## 2. 세척 탭 제거

### 변경 범위
- `ui/navigation/BottomNavItem.kt` — `Cleaning` 항목 제거
- `ui/navigation/BabyFeedingNavHost.kt` — `cleaning` route 제거
- `di/AppContainer.kt` — CleaningDataSource/Repository 인스턴스 선언은 유지 (데이터 보존)

### 탭 순서 (변경 후)
수유 → 수면 → 기저귀 → 통계 → **성장** (5탭)

---

## 3. 성장 기록 탭

### 3-1. 데이터 모델

```kotlin
// data/GrowthRecord.kt
data class GrowthRecord(
    override val id: String = "",
    override val timestamp: Long = System.currentTimeMillis(),
    override val note: String? = null,
    override val recordedBy: String? = null,
    val heightCm: Float? = null,     // 키 (cm), 독립 입력
    val weightKg: Float? = null,     // 몸무게 (kg), 독립 입력
    val headCm: Float? = null,       // 머리둘레 (cm), 독립 입력
) : BaseRecord
```

**유효성 조건:** `heightCm`, `weightKg`, `headCm` 중 최소 1개 이상 입력 필수.

**Firestore 경로:** `users/{uid}/growth_records/{docId}`

### 3-2. 데이터 레이어

기존 아키텍처 패턴(`BaseDataSource<T>` / `BaseRepository<T>`) 그대로 적용.

| 파일 | 역할 |
|------|------|
| `data/GrowthRecord.kt` | 데이터 클래스 |
| `data/GrowthDataSource.kt` | `BaseDataSource<GrowthRecord>` 상속, `collectionName = "growth_records"`, `toRecord()` 구현 |
| `data/GrowthRepository.kt` | `BaseRepository<GrowthRecord>` 상속. 별도 고유 메서드 없음 |

`AppContainer.kt`에 `GrowthDataSource`, `GrowthRepository` 등록.

### 3-3. 화면 구성 (GrowthScreen)

```
GrowthScreen
├── BabyProfileBanner (기존 컴포넌트 재사용, 생후 N일 배너)
├── 요약 카드 Row (3개)
│   ├── 키 최신값 카드 (없으면 "--")
│   ├── 몸무게 최신값 카드 (없으면 "--")
│   └── 머리둘레 최신값 카드 (없으면 "--")
├── LazyColumn (타임라인)
│   ├── GrowthTimelineCard (입력된 필드만 표시)
│   └── 무한 스크롤 (기존 loadMore 패턴)
└── FAB → GrowthBottomSheet
```

**요약 카드:** 각 측정값의 최신 기록을 독립적으로 쿼리. 예) 키는 마지막으로 heightCm이 null이 아닌 기록.

**타임라인 카드 표시 예시:**
- `몸무게 4.8 kg` / `2026.05.01 · 소아과 방문`
- `키 56.2 cm · 머리둘레 37.5 cm` / `2026.04.15`

### 3-4. GrowthBottomSheet 입력 필드

| 필드 | 타입 | 필수 여부 |
|------|------|-----------|
| 키 | Float, OutlinedTextField, 단위 cm | 선택 |
| 몸무게 | Float, OutlinedTextField, 단위 kg | 선택 |
| 머리둘레 | Float, OutlinedTextField, 단위 cm | 선택 |
| 날짜/시간 | RecordDateTimeEditor 재사용 | 필수 (기본값: 현재) |
| 메모 | String, OutlinedTextField | 선택 |

저장 버튼은 3개 중 1개 이상 입력 시 활성화.

### 3-5. GrowthViewModel

기존 ViewModel 패턴과 동일:
- `recentRecords: StateFlow<List<GrowthRecord>>`
- `loadMore()` 무한 스크롤
- `addRecord(record: GrowthRecord): DataResult<Unit>`
- `deleteRecord(id: String): DataResult<Unit>`
- `errorMessage: StateFlow<String?>` (Snackbar)
- `latestHeight`, `latestWeight`, `latestHead`: 요약 카드용 최신값 StateFlow

---

## 4. 홈 화면 위젯

### 4-1. 의존성 추가 (build.gradle)

```kotlin
implementation("androidx.glance:glance-appwidget:1.1.0")
implementation("androidx.glance:glance-material3:1.1.0")
```

### 4-2. 데이터 공급 방식

위젯이 Firestore를 직접 구독하는 대신 **DataStore를 중간 캐시**로 사용.

- 수유 기록 추가/삭제 시 → `FeedingRepository`에서 DataStore에 마지막 수유 timestamp + type 저장
- 위젯은 DataStore에서 읽어 경과 시간 계산
- 위젯 주기적 갱신: `android:updatePeriodMillis="1800000"` (30분, 시스템 최소값)
- 앱 실행 시 위젯 즉시 업데이트 (`GlanceAppWidget.update()` 호출)

**DataStore 추가 키:**
```kotlin
val LAST_FEEDING_TIMESTAMP = longPreferencesKey("last_feeding_timestamp")
val LAST_FEEDING_TYPE = stringPreferencesKey("last_feeding_type")  // "모유(좌)", "분유" 등
```

### 4-3. 위젯 구성

| 항목 | 내용 |
|------|------|
| 크기 | 4×1 (minWidth: 250dp, minHeight: 54dp) |
| 배경 | 앱 primary 컬러(Coral/Peach) 그라데이션 |
| 표시 내용 | 🍼 아이콘 + "N시간 M분 전" + 수유 타입(모유좌/우/분유/유축) |
| 기록 없을 때 | "아직 기록 없음" |
| 탭 동작 | 앱 수유 탭으로 딥링크 |

### 4-4. 신규 파일

| 파일 | 역할 |
|------|------|
| `ui/widget/FeedingWidget.kt` | `GlanceAppWidget` 구현, Composable UI |
| `ui/widget/FeedingWidgetReceiver.kt` | `GlanceAppWidgetReceiver` |
| `res/xml/feeding_widget_info.xml` | 위젯 메타데이터 |

### 4-5. Manifest 추가

```xml
<receiver android:name=".ui.widget.FeedingWidgetReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/feeding_widget_info" />
</receiver>
```

권한 추가: `android.permission.RECEIVE_BOOT_COMPLETED` (부팅 후 위젯 복원)

---

## 5. 파일 변경 요약

### 신규 파일 (8개)
- `data/GrowthRecord.kt`
- `data/GrowthDataSource.kt`
- `data/GrowthRepository.kt`
- `ui/growth/GrowthViewModel.kt`
- `ui/growth/GrowthScreen.kt`
- `ui/widget/FeedingWidget.kt`
- `ui/widget/FeedingWidgetReceiver.kt`
- `res/xml/feeding_widget_info.xml`

### 수정 파일 (7개)
- `di/AppContainer.kt` — GrowthDataSource/Repository 등록, DataStore 키 추가
- `ui/navigation/BottomNavItem.kt` — Cleaning 제거, Growth 추가
- `ui/navigation/BabyFeedingNavHost.kt` — cleaning route 제거, growth route 추가
- `data/FeedingRepository.kt` — 수유 기록 시 DataStore 업데이트 추가
- `AndroidManifest.xml` — 위젯 receiver, RECEIVE_BOOT_COMPLETED 권한 추가
- `build.gradle` — Glance 의존성 추가
- `firestore.rules` — growth_records 컬렉션 규칙 추가

---

## 6. Firestore 보안 규칙 추가

```
match /growth_records/{docId} {
  allow read, write: if isOwnerOrLinked();
}
```

---

## 7. 버전

- versionName: `2.0.0`
- versionCode: `9`
