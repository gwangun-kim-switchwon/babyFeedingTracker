# 맘마미아 (Mammamia) - Baby Feeding Tracker

배우자와 함께 쓰는 신생아 수유/기저귀/세척 기록 앱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.06-blue.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-yellow.svg)](https://developer.android.com)

## Features

### Feeding (수유)
- 모유 수유 기록 (좌/우 유방별 시간)
- 분유 수유 기록 (용량 선택)
- 유축 기록
- 일별 타임라인 & 통계

### Diaper (기저귀)
- 기저귀 교체, 소변, 대변 분류 기록
- 마지막 교체 경과 시간 표시
- 일별 통계 (종류별 횟수)

### Cleaning (세척)
- 젖병, 분유포트, 유축기, 기타 세척 기록
- 종류별 마지막 세척 경과 시간 표시

### Partner Sharing (배우자 공유)
- Google 로그인 후 6자리 초대 코드 생성
- 초대 코드 입력으로 배우자와 실시간 데이터 동기화
- 로그인 없이도 익명으로 즉시 사용 가능

## Screenshots

| 수유 | 기저귀 | 세척 |
|:---:|:---:|:---:|
| Timeline & stats | Timeline & stats | Timeline & stats |

## Tech Stack

| Category | Technology |
|----------|-----------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Repository Pattern |
| DI | Manual (AppContainer) |
| Database | Cloud Firestore (real-time sync + offline) |
| Auth | Firebase Auth (Anonymous + Google) |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle 8.2.2, AGP 8.2.2 |

## Project Structure

```
app/src/main/java/com/baby/feedingtracker/
├── di/                     # Dependency injection
│   └── AppContainer.kt
├── data/                   # Data layer
│   ├── *Record.kt          # Data models (Feeding, Diaper, Cleaning)
│   ├── *Repository.kt      # Repository interfaces
│   ├── *DataSource.kt      # Firestore implementations
│   ├── UserRepository.kt   # User profile & sharing
│   └── GoogleAuthHelper.kt # Google Sign-In
└── ui/                     # Presentation layer
    ├── feeding/             # Feeding screen + ViewModel
    ├── diaper/              # Diaper screen + ViewModel
    ├── cleaning/            # Cleaning screen + ViewModel
    ├── navigation/          # Bottom nav & NavHost
    ├── ShareBottomSheet.kt  # Partner sharing UI
    └── theme/               # Material 3 theme
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Firebase project with Firestore and Authentication enabled

### Setup

1. Clone the repository
   ```bash
   git clone https://github.com/gwangun-kim-switchwon/babyFeedingTracker.git
   ```

2. Add your `google-services.json` to `app/`
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable **Authentication** (Anonymous + Google Sign-In)
   - Enable **Cloud Firestore**
   - Download `google-services.json` and place it in `app/`

3. (Optional) Configure release signing in `local.properties`
   ```properties
   RELEASE_STORE_FILE=../your_keystore.jks
   RELEASE_STORE_PASSWORD=your_password
   RELEASE_KEY_ALIAS=your_alias
   RELEASE_KEY_PASSWORD=your_password
   ```

4. Build and run
   ```bash
   ./gradlew assembleDebug
   ```

## Firestore Data Model

```
users/{uid}/
├── profile/
│   └── info          # { dataOwnerUid, linkedTo, email }
├── feeding_records/  # { timestamp, type, amountMl, leftMin, rightMin }
├── diaper_records/   # { timestamp, type }
└── cleaning_records/ # { timestamp, itemType }
```

## Privacy Policy

[Privacy Policy](https://gwangun-kim-switchwon.github.io/mammamia-privacy/)

## License

This project is proprietary software. All rights reserved.

## Contact

garlicfatherdev@gmail.com
