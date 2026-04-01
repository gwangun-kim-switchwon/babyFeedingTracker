# Bottom Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단일 화면 앱에 바텀 네비게이션을 도입하여 멀티 탭 구조로 전환 (수유 탭 + 세척 탭 placeholder)

**Architecture:** Jetpack Navigation Compose + Material 3 NavigationBar. 기존 MainScreen/MainViewModel을 feeding 패키지로 이동 및 이름 변경. BabyFeedingNavHost가 Scaffold + NavHost를 관리.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Navigation Compose 2.7.6

**Spec:** `docs/superpowers/specs/2026-04-01-bottom-nav-design.md`

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `app/build.gradle.kts` | Modify | navigation-compose 의존성 추가 |
| `app/src/main/java/com/baby/feedingtracker/ui/navigation/BottomNavItem.kt` | Create | 탭 정의 (route, label, icon) |
| `app/src/main/java/com/baby/feedingtracker/ui/navigation/BabyFeedingNavHost.kt` | Create | Scaffold(bottomBar) + NavHost |
| `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingScreen.kt` | Create (move) | MainScreen에서 이동 + 이름 변경 |
| `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingViewModel.kt` | Create (move) | MainViewModel에서 이동 + 이름 변경 |
| `app/src/main/java/com/baby/feedingtracker/ui/cleaning/CleaningScreen.kt` | Create | "준비 중" placeholder |
| `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` | Delete | FeedingScreen으로 이동됨 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` | Delete | FeedingViewModel으로 이동됨 |
| `app/src/main/java/com/baby/feedingtracker/MainActivity.kt` | Modify | BabyFeedingNavHost 호출로 변경 |

---

## Task 1: Add navigation-compose dependency

**Files:**
- Modify: `app/build.gradle.kts:66-100`

- [ ] **Step 1: Add navigation-compose dependency**

`app/build.gradle.kts`의 dependencies 블록에 navigation-compose를 추가한다.

`// Activity Compose` 줄 바로 위에 추가:

```kotlin
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Activity Compose
```

즉, 기존:
```kotlin
    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")
```

변경 후:
```kotlin
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.8.2")
```

- [ ] **Step 2: Build verification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/build.gradle.kts && git commit -m "feat: add navigation-compose dependency"
```

---

## Task 2: Create BottomNavItem

**Files:**
- Create: `app/src/main/java/com/baby/feedingtracker/ui/navigation/BottomNavItem.kt`

- [ ] **Step 1: Create navigation directory and BottomNavItem.kt**

Create directory `app/src/main/java/com/baby/feedingtracker/ui/navigation/`.

Create `app/src/main/java/com/baby/feedingtracker/ui/navigation/BottomNavItem.kt` with the following complete content:

```kotlin
package com.baby.feedingtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Feeding : BottomNavItem("feeding", "수유", Icons.Outlined.LocalDrink)
    object Cleaning : BottomNavItem("cleaning", "세척", Icons.Outlined.CleaningServices)
}
```

- [ ] **Step 2: Build verification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/navigation/BottomNavItem.kt && git commit -m "feat: create BottomNavItem sealed class for tab definitions"
```

---

## Task 3: Move MainScreen to FeedingScreen

**Files:**
- Create (move): `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingScreen.kt`
- Delete: `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`

This is a 958-line file. It is a move + rename, so the specific line changes are listed below.

- [ ] **Step 1: Create feeding directory**

```bash
mkdir -p /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/feeding
```

- [ ] **Step 2: Copy MainScreen.kt to FeedingScreen.kt**

```bash
cp /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingScreen.kt
```

- [ ] **Step 3: Apply changes to FeedingScreen.kt**

The following specific lines need to change in `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingScreen.kt`:

**Change 1: Package declaration (line 1)**

변경 전:
```kotlin
package com.baby.feedingtracker.ui
```

변경 후:
```kotlin
package com.baby.feedingtracker.ui.feeding
```

**Change 2: Remove navigationBars import (line 18)**

Remove this line entirely:
```kotlin
import androidx.compose.foundation.layout.navigationBars
```

**Change 3: Rename function and parameter type (lines 69-70)**

변경 전:
```kotlin
fun MainScreen(
    viewModel: MainViewModel,
```

변경 후:
```kotlin
fun FeedingScreen(
    viewModel: FeedingViewModel,
```

**Change 4: Remove windowInsetsPadding for navigationBars (line 220)**

변경 전:
```kotlin
                    .windowInsetsPadding(WindowInsets.navigationBars)
```

이 줄을 완전히 삭제한다. 바텀 네비게이션이 있으므로 Scaffold가 insets를 처리한다.

**Note:** `MainUiState`는 MainScreen.kt에서 직접 참조하지 않으므로 (line 74에서 `viewModel.uiState.collectAsStateWithLifecycle()`로 사용) 추가 변경 불필요. `ShareBottomSheet`는 같은 `com.baby.feedingtracker.ui` 패키지에 있고, MainViewModel/MainScreen 타입을 import하지 않으므로 변경 불필요.

- [ ] **Step 4: Delete old MainScreen.kt**

```bash
rm /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt
```

- [ ] **Step 5: Build verification**

빌드는 Task 4 (FeedingViewModel 생성) 이후에 수행한다. MainScreen이 MainViewModel을 참조하므로 FeedingViewModel이 없으면 빌드 실패함.

---

## Task 4: Move MainViewModel to FeedingViewModel

**Files:**
- Create (move): `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingViewModel.kt`
- Delete: `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`

This is a 191-line file. Move + rename with the following specific changes.

- [ ] **Step 1: Copy MainViewModel.kt to FeedingViewModel.kt**

```bash
cp /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingViewModel.kt
```

- [ ] **Step 2: Apply changes to FeedingViewModel.kt**

The following specific lines need to change in `app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingViewModel.kt`:

**Change 1: Package declaration (line 1)**

변경 전:
```kotlin
package com.baby.feedingtracker.ui
```

변경 후:
```kotlin
package com.baby.feedingtracker.ui.feeding
```

**Change 2: Rename MainUiState data class (line 22)**

변경 전:
```kotlin
data class MainUiState(
```

변경 후:
```kotlin
data class FeedingUiState(
```

**Change 3: Rename MainViewModel class (line 27)**

변경 전:
```kotlin
class MainViewModel(
```

변경 후:
```kotlin
class FeedingViewModel(
```

**Change 4: Rename uiState type (line 44)**

변경 전:
```kotlin
    val uiState: StateFlow<MainUiState> = combine(
```

변경 후:
```kotlin
    val uiState: StateFlow<FeedingUiState> = combine(
```

**Change 5: Rename MainUiState constructor call (line 54)**

변경 전:
```kotlin
        MainUiState(
```

변경 후:
```kotlin
        FeedingUiState(
```

**Change 6: Rename initialValue (line 61)**

변경 전:
```kotlin
        initialValue = MainUiState()
```

변경 후:
```kotlin
        initialValue = FeedingUiState()
```

**Change 7: Rename in factory method (line 186)**

변경 전:
```kotlin
                    return MainViewModel(repository, userRepository, googleAuthHelper, auth, onDataOwnerChanged) as T
```

변경 후:
```kotlin
                    return FeedingViewModel(repository, userRepository, googleAuthHelper, auth, onDataOwnerChanged) as T
```

- [ ] **Step 3: Delete old MainViewModel.kt**

```bash
rm /Users/seonhoappa/projects/baby-feeding-tracker/app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt
```

- [ ] **Step 4: Build verification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

빌드 실패할 수 있음 -- MainActivity.kt가 아직 MainScreen/MainViewModel을 참조하고 있기 때문. Task 7에서 해결. 여기서는 FeedingScreen.kt + FeedingViewModel.kt가 서로 올바르게 참조하는지만 확인.

- [ ] **Step 5: Commit (Task 3 + Task 4 together)**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingScreen.kt app/src/main/java/com/baby/feedingtracker/ui/feeding/FeedingViewModel.kt && git rm app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt && git commit -m "refactor: move MainScreen/MainViewModel to feeding package as FeedingScreen/FeedingViewModel"
```

---

## Task 5: Create CleaningScreen placeholder

**Files:**
- Create: `app/src/main/java/com/baby/feedingtracker/ui/cleaning/CleaningScreen.kt`

- [ ] **Step 1: Create cleaning directory and CleaningScreen.kt**

Create directory `app/src/main/java/com/baby/feedingtracker/ui/cleaning/`.

Create `app/src/main/java/com/baby/feedingtracker/ui/cleaning/CleaningScreen.kt` with the following complete content:

```kotlin
package com.baby.feedingtracker.ui.cleaning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@Composable
fun CleaningScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "준비 중",
            style = MaterialTheme.typography.headlineSmall,
            color = LocalExtendedColors.current.subtleText
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/cleaning/CleaningScreen.kt && git commit -m "feat: create CleaningScreen placeholder"
```

---

## Task 6: Create BabyFeedingNavHost

**Files:**
- Create: `app/src/main/java/com/baby/feedingtracker/ui/navigation/BabyFeedingNavHost.kt`

- [ ] **Step 1: Create BabyFeedingNavHost.kt**

Create `app/src/main/java/com/baby/feedingtracker/ui/navigation/BabyFeedingNavHost.kt` with the following complete content:

```kotlin
package com.baby.feedingtracker.ui.navigation

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.ui.cleaning.CleaningScreen
import com.baby.feedingtracker.ui.feeding.FeedingScreen
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

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
            startDestination = BottomNavItem.Feeding.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(BottomNavItem.Feeding.route) {
                FeedingScreen(
                    viewModel = feedingViewModel,
                    googleAuthHelper = googleAuthHelper,
                    googleSignInLauncher = googleSignInLauncher
                )
            }
            composable(BottomNavItem.Cleaning.route) {
                CleaningScreen()
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Feeding, BottomNavItem.Cleaning)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination to avoid building up a back stack
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = LocalExtendedColors.current.subtleText,
                    unselectedTextColor = LocalExtendedColors.current.subtleText,
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/navigation/BabyFeedingNavHost.kt && git commit -m "feat: create BabyFeedingNavHost with bottom navigation bar"
```

---

## Task 7: Update MainActivity

**Files:**
- Modify: `app/src/main/java/com/baby/feedingtracker/MainActivity.kt`

- [ ] **Step 1: Update imports**

`app/src/main/java/com/baby/feedingtracker/MainActivity.kt`에서 import를 변경한다.

변경 전 (lines 19-20):
```kotlin
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
```

변경 후:
```kotlin
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.baby.feedingtracker.ui.navigation.BabyFeedingNavHost
```

- [ ] **Step 2: Update ViewModel creation**

변경 전 (line 53):
```kotlin
                    val viewModel: MainViewModel = viewModel(
```

변경 후:
```kotlin
                    val viewModel: FeedingViewModel = viewModel(
```

변경 전 (line 55):
```kotlin
                        factory = MainViewModel.factory(
```

변경 후:
```kotlin
                        factory = FeedingViewModel.factory(
```

- [ ] **Step 3: Update MainScreen call to BabyFeedingNavHost**

변경 전 (lines 85-89):
```kotlin
                    MainScreen(
                        viewModel = viewModel,
                        googleAuthHelper = app.container.googleAuthHelper,
                        googleSignInLauncher = googleSignInLauncherWithRefresh
                    )
```

변경 후:
```kotlin
                    BabyFeedingNavHost(
                        feedingViewModel = viewModel,
                        googleAuthHelper = app.container.googleAuthHelper,
                        googleSignInLauncher = googleSignInLauncherWithRefresh
                    )
```

- [ ] **Step 4: Build verification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

- [ ] **Step 5: Commit**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/MainActivity.kt && git commit -m "feat: wire BabyFeedingNavHost in MainActivity"
```

---

## Task 8: Update ShareBottomSheet imports (if needed)

**Files:**
- Check: `app/src/main/java/com/baby/feedingtracker/ui/ShareBottomSheet.kt`

- [ ] **Step 1: Verify no changes needed**

`ShareBottomSheet.kt`는 `MainViewModel`이나 `MainScreen` 타입을 import하지 않는다 (확인 완료). `ShareBottomSheet`는 `com.baby.feedingtracker.ui` 패키지에 남아 있으며, FeedingScreen에서 호출되는 composable이므로 별도 변경 불필요.

만약 빌드 시 ShareBottomSheet 관련 에러가 발생하면, 해당 import를 `com.baby.feedingtracker.ui.feeding` 패키지로 업데이트한다.

---

## Task 9: Final build verification and manual checklist

- [ ] **Step 1: Clean build**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew clean assembleDebug
```

- [ ] **Step 2: Manual verification checklist**

앱을 실행하여 다음을 확인:

- [ ] 바텀 네비게이션 바가 화면 하단에 표시된다
- [ ] "수유" 탭이 기본 선택되어 있다
- [ ] "수유" 탭에서 기존 수유 기록 화면이 정상 표시된다
- [ ] 수유 기록 추가 버튼이 정상 동작한다
- [ ] 바텀시트 (기록 편집, 공유) 가 정상 동작한다
- [ ] "세척" 탭 클릭 시 "준비 중" 텍스트가 표시된다
- [ ] 탭 전환이 부드럽게 동작한다
- [ ] 선택된 탭은 민트(primary) 색, 비선택 탭은 subtleText 색이다
- [ ] 바텀 네비 배경이 surface 색이다
- [ ] 하단 수유 기록 버튼이 바텀 네비와 겹치지 않는다
- [ ] Edge-to-edge 레이아웃이 정상 동작한다 (상태바, 네비게이션 바 영역)

- [ ] **Step 3: Final commit (if any fixes were needed)**

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add -A && git commit -m "fix: address build/layout issues from bottom nav integration"
```
