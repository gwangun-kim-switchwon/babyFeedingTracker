# Baby Feeding Tracker - 바텀 네비게이션 도입 설계 (Sub-project A)

## 1. Overview

기존 단일 화면 앱에 바텀 네비게이션을 도입하여 멀티 탭 구조로 전환한다.
탭 1: 수유 기록 (기존 화면), 탭 2: 세척 기록 (placeholder, Sub-project B에서 구현).

---

## 2. Navigation 구조

```
MainActivity
└── BabyFeedingNavHost (Scaffold + NavigationBar)
    ├── FeedingScreen (탭 1: 🍼 수유) ← 기존 MainScreen
    └── CleaningScreen (탭 2: ✨ 세척) ← placeholder
```

- Jetpack Navigation Compose 사용
- `NavigationBar` + `NavigationBarItem` (Material 3)
- 각 탭은 독립된 composable + ViewModel

---

## 3. 바텀 네비게이션 바

| 탭 | 아이콘 | 라벨 | Route |
|---|---|---|---|
| 수유 | `Icons.Outlined.LocalDrink` | "수유" | `"feeding"` |
| 세척 | `Icons.Outlined.CleaningServices` | "세척" | `"cleaning"` |

- Material 3 `NavigationBar` 사용
- 선택된 탭: primary 컬러 (민트), 비선택: subtleText
- 바텀 네비 배경: surface 컬러

---

## 4. 파일 구조 변경

### 새 파일

```
ui/navigation/
├── BottomNavItem.kt        # sealed class: route, icon, label 정의
└── BabyFeedingNavHost.kt   # Scaffold(bottomBar) + NavHost
```

### 이동 + 이름 변경

```
ui/MainScreen.kt      → ui/feeding/FeedingScreen.kt
ui/MainViewModel.kt   → ui/feeding/FeedingViewModel.kt
```

- `MainScreen` → `FeedingScreen` 이름 변경
- `MainViewModel` → `FeedingViewModel` 이름 변경
- 내부 로직 변경 없음, 패키지 + 클래스명만 변경

### 새 placeholder

```
ui/cleaning/
├── CleaningScreen.kt       # "준비 중" placeholder
└── CleaningViewModel.kt    # 빈 ViewModel (Sub-project B에서 구현)
```

### 유지

```
ui/ShareBottomSheet.kt      # 공유는 수유 탭에서만 사용
ui/theme/Theme.kt            # 변경 없음
```

---

## 5. BabyFeedingNavHost 구성

```kotlin
@Composable
fun BabyFeedingNavHost(
    feedingViewModel: FeedingViewModel,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "feeding",
            modifier = Modifier.padding(padding)
        ) {
            composable("feeding") {
                FeedingScreen(
                    viewModel = feedingViewModel,
                    googleAuthHelper = googleAuthHelper,
                    googleSignInLauncher = googleSignInLauncher
                )
            }
            composable("cleaning") {
                CleaningScreen()
            }
        }
    }
}
```

---

## 6. MainActivity 변경

기존:
```kotlin
MainScreen(viewModel, googleAuthHelper, launcher)
```

변경:
```kotlin
BabyFeedingNavHost(feedingViewModel, googleAuthHelper, launcher)
```

ViewModel 생성 로직은 MainActivity에서 그대로 유지. NavHost에 전달.

---

## 7. 공유 아이콘 위치

공유 아이콘 + 상태 dot은 FeedingScreen(수유 탭)에만 표시.
세척 탭에서는 표시하지 않음 (같은 Firestore 계정이면 자동 공유).

---

## 8. FeedingScreen 내부 변경

기존 `MainScreen`의 `windowInsetsPadding(WindowInsets.navigationBars)` 제거 필요.
바텀 네비게이션 바가 있으므로 Scaffold가 insets를 처리함.

---

## 9. 앱 아이콘 업데이트

수유 아이콘 기반 민트 컬러 adaptive icon 생성.
- Foreground: 젖병 아이콘 (벡터)
- Background: 민트 그린 (#8CC9B0)

---

## 10. 의존성 추가

```kotlin
implementation("androidx.navigation:navigation-compose:2.7.6")
```

---

## 11. 파일 변경 목록

| 파일 | Action | 내용 |
|------|--------|------|
| `app/build.gradle.kts` | Modify | navigation-compose 추가 |
| `ui/navigation/BottomNavItem.kt` | Create | 탭 정의 |
| `ui/navigation/BabyFeedingNavHost.kt` | Create | NavHost + BottomNav |
| `ui/feeding/FeedingScreen.kt` | Create (move) | MainScreen에서 이동 |
| `ui/feeding/FeedingViewModel.kt` | Create (move) | MainViewModel에서 이동 |
| `ui/cleaning/CleaningScreen.kt` | Create | placeholder |
| `ui/MainScreen.kt` | Delete | FeedingScreen으로 이동 |
| `ui/MainViewModel.kt` | Delete | FeedingViewModel으로 이동 |
| `MainActivity.kt` | Modify | BabyFeedingNavHost 호출 |
| `app/src/main/res/` | Modify | 앱 아이콘 교체 |

---

## 12. 범위 외

- 세척 기록 기능 (Sub-project B)
- 추가 탭 (기저귀, 수면 등)
- 탭 간 데이터 공유
