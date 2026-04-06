# 맘마미아 (Mammamia)

배우자와 함께 쓰는 신생아 수유/기저귀/세척 기록 앱

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.06-blue.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-yellow.svg)](https://developer.android.com)

## 소개

**맘마미아**는 신생아를 돌보는 부부를 위한 육아 기록 앱입니다.

수유, 기저귀, 젖병 세척까지 한 앱에서 기록하고, 배우자와 실시간으로 공유할 수 있습니다. 로그인 없이 바로 사용할 수 있으며, 배우자 공유가 필요할 때만 Google 로그인을 하면 됩니다.

> 이 프로젝트는 기획, 설계, UI/UX 디자인, 구현, 코드 리뷰, QA까지 전 과정을 **Claude Code (Anthropic AI)**와 협업하여 개발되었습니다. 아키텍처 설계부터 Firestore 보안 규칙, 배우자 공유 시스템, 가로 모드 대응까지 — AI가 팀원처럼 함께 만든 앱입니다.

## 주요 기능

### 수유 기록
- 모유 수유 (좌/우 유방별 시간 기록)
- 분유 수유 (60~160ml 용량 선택)
- 유축 기록
- 마지막 수유 경과 시간 표시
- 일별 타임라인 & 일일 통계 (횟수, 총량)

### 기저귀 기록
- 기저귀 / 소변 / 대변 분류 기록
- 마지막 교체 경과 시간 표시
- 일별 종류별 통계

### 세척 기록
- 젖병, 분유포트, 유축기, 기타 세척 기록
- 종류별 마지막 세척 경과 시간 표시

### 배우자 공유
- 6자리 초대 코드로 간편 연결 (10분 유효)
- 연결 즉시 실시간 데이터 동기화
- 오프라인에서도 기록 가능 (온라인 복귀 시 자동 동기화)
- 연결 상태 아이콘 표시 (초록: 연결됨, 빨강: 미연결)

## 스크린샷

| 수유 | 기저귀 | 세척 |
|:---:|:---:|:---:|
| 타임라인 & 통계 | 타임라인 & 통계 | 타임라인 & 통계 |

## 기술 스택

| 분류 | 기술 |
|------|-----|
| 언어 | Kotlin 1.9.22 |
| UI | Jetpack Compose (Material 3) |
| 아키텍처 | MVVM + Repository 패턴 |
| DI | Manual DI (AppContainer) |
| 데이터베이스 | Cloud Firestore (실시간 동기화 + 오프라인 지��) |
| 인증 | Firebase Auth (익명 + Google 로그인) |
| 내비게이션 | Navigation Compose |
| 비동기 처리 | Kotlin Coroutines + Flow |
| 빌드 | Gradle 8.2.2, AGP 8.2.2 |

## 프로젝트 구조

```
app/src/main/java/com/baby/feedingtracker/
├── di/                     # 의존성 주입
│   └── AppContainer.kt
├── data/                   # 데이터 레이어
│   ├── *Record.kt          # 데이터 모델 (Feeding, Diaper, Cleaning)
│   ├── *Repository.kt      # Repository 인터페이스
│   ├── *DataSource.kt      # Firestore 구현체
│   ├── UserRepository.kt   # 사용자 프로필 & 공유 상태 관리
│   └── GoogleAuthHelper.kt # Google 로그인 처리
└── ui/                     # 프레젠테이션 레이어
    ├── feeding/             # 수유 화면 + ViewModel
    ├── diaper/              # 기저귀 화면 + ViewModel
    ├── cleaning/            # 세척 화면 + ViewModel
    ├── navigation/          # 하단 내비게이션 & NavHost
    ├── ShareBottomSheet.kt  # 배우자 공유 UI
    └── theme/               # Material 3 테마
```

## 시��하기

### 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Firebase 프로젝트 (Firestore + Authentication 활성화)

### 설정 방법

1. 저장소 클론
   ```bash
   git clone https://github.com/gwangun-kim-switchwon/babyFeedingTracker.git
   ```

2. `google-services.json`을 `app/` 디렉토리에 추가
   - [Firebase Console](https://console.firebase.google.com)에서 프로젝트 생성
   - **Authentication** 활성화 (익명 + Google 로그인)
   - **Cloud Firestore** 활성화
   - `google-services.json` 다운로드 후 `app/`에 배치

3. (선택) `local.properties`에 릴리즈 서명 설정
   ```properties
   RELEASE_STORE_FILE=../your_keystore.jks
   RELEASE_STORE_PASSWORD=your_password
   RELEASE_KEY_ALIAS=your_alias
   RELEASE_KEY_PASSWORD=your_password
   ```

4. 빌드 및 실행
   ```bash
   ./gradlew assembleDebug
   ```

## Firestore 데이터 모델

```
users/{uid}/
├── profile/
│   └── info          # { dataOwnerUid, linkedTo, email }
├── feeding_records/  # { timestamp, type, amountMl, leftMin, rightMin }
├── diaper_records/   # { timestamp, type }
└── cleaning_records/ # { timestamp, itemType }
```

## AI 협업 개발

이 프로젝트는 **사람 + AI 페어 프로그래밍**의 결과물입니다.

[Claude Code](https://claude.ai/claude-code)가 다음 역할을 수행했습니다:

- **아키텍트**: MVVM 구조 설계, Firestore 데이터 모델링, ADR 문서화
- **UI/UX 디자이너**: "Soft Minimal" 디자인 시스템, 컬러 팔레트, 컴포넌트 설계
- **Android 개발자**: Kotlin + Jetpack Compose 전체 구현
- **코드 리뷰어**: SOLID 원칙 검증, 보안 취약점 점검
- **QA 엔지니어**: 엣지 케이스 테스트, 가로 모드/오프라인 시나리오 검증
- **PM**: 6개 개발 사이클 관리, 작업 분해 및 진행 추적

아이디어와 방향 설정은 사람이, 설계부터 구현까지의 실행은 AI가 담당하는 새로운 개발 방식을 실험한 프로젝트입니다.

## 개인정보 처리방침

[개인정보 처리방침](https://gwangun-kim-switchwon.github.io/mammamia-privacy/)

## 라이선스

이 프로젝트는 [MIT License](LICENSE)로 배포됩니다.

## 연락처

garlicfatherdev@gmail.com
