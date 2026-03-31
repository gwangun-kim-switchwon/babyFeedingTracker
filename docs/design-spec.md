# Baby Feeding Tracker - Design Specification

## Design Philosophy

**Soft Minimal** - 불필요한 장식을 제거하고, 대담한 타이포그래피와 충분한 여백으로 정보를 전달합니다.

핵심 원칙:
- 한 손 조작 최적화 (thumb zone 중심 인터랙션)
- 정보 계층 명확화 (경과 시간 > 기록 목록 > 액션)
- 플랫 디자인 (elevation 없음, 색상 차이로 구분)
- 부드러운 인터랙션 (spring animation, swipe gesture)

---

## Color Palette

### Primary Colors

| Name          | Hex       | Usage                        |
|---------------|-----------|------------------------------|
| Cream White   | `#FFF8F5` | 메인 배경 (gradient top)      |
| Warm White    | `#FFFBF8` | 카드/섹션 배경                |
| Soft Beige    | `#FFF1EB` | 미세한 구분 배경 (gradient bottom) |

### Accent Colors

| Name             | Hex       | Usage                    |
|------------------|-----------|--------------------------|
| Soft Coral       | `#FF8A76` | 메인 액센트, CTA 버튼     |
| Soft Coral Light | `#FFB4A2` | 연한 액센트, hover 상태    |
| Soft Coral Dark  | `#E8735F` | 눌림 상태                 |

### Text Colors

| Name           | Hex       | Usage                         |
|----------------|-----------|-------------------------------|
| Dark Charcoal  | `#2D2626` | 주요 텍스트 (시간, 제목)       |
| Warm Gray      | `#8E8685` | 서브 텍스트 (라벨, 간격)       |
| Light Gray     | `#BEB8B6` | 비활성/힌트 텍스트             |

### Utility Colors

| Name           | Hex       | Usage                         |
|----------------|-----------|-------------------------------|
| Soft Red       | `#FF6B6B` | 삭제 텍스트/아이콘             |
| Soft Red Bg    | `#FFE8E8` | 스와이프 삭제 배경             |
| Divider        | `#F0E8E4` | 섹션 구분선                   |

### Gradient

- **방향**: Top → Bottom (수직)
- **시작**: `#FFF8F5` (Cream White)
- **끝**: `#FFF1EB` (Soft Beige)

---

## Typography Scale

| Style          | Size  | Weight    | Line Height | Letter Spacing | Usage                    |
|----------------|-------|-----------|-------------|----------------|--------------------------|
| Display Large  | 48sp  | Bold      | 56sp        | -1.0sp         | 경과 시간 메인 표시       |
| Display Medium | 36sp  | SemiBold  | 44sp        | -0.5sp         | (예약)                   |
| Headline Large | 28sp  | Bold      | 36sp        | 0              | (예약)                   |
| Headline Medium| 22sp  | SemiBold  | 28sp        | 0              | (예약)                   |
| Headline Small | 18sp  | Medium    | 24sp        | 0              | "전" 보조 텍스트          |
| Title Large    | 20sp  | SemiBold  | 28sp        | 0              | 기록 시간 (HH:mm)        |
| Title Medium   | 16sp  | Medium    | 24sp        | 0              | 일반 타이틀              |
| Title Small    | 14sp  | Medium    | 20sp        | 0              | (예약)                   |
| Body Large     | 16sp  | Normal    | 24sp        | 0              | 본문 텍스트              |
| Body Medium    | 14sp  | Normal    | 20sp        | 0              | 서브 라벨 ("마지막 수유")  |
| Body Small     | 12sp  | Normal    | 16sp        | 0              | 간격 텍스트 ("2h 15m 간격")|
| Label Large    | 16sp  | SemiBold  | 24sp        | 0              | CTA 버튼 텍스트          |
| Label Medium   | 12sp  | Medium    | 16sp        | 0.5sp          | 날짜 헤더 ("오늘")        |
| Label Small    | 10sp  | Medium    | 14sp        | 0              | (예약)                   |

---

## Layout Wireframe

```
┌──────────────────────────────────┐
│         [status bar]             │  ← system, edge-to-edge
│                                  │
│                                  │  ← 48dp top padding
│  마지막 수유                      │  ← Body Medium, Warm Gray
│  2시간 15분                      │  ← Display Large (48sp), Bold
│  전                              │  ← Headline Small, Warm Gray
│                                  │
│                                  │  ← 충분한 여백
│  오늘 ───────────────────        │  ← Label Medium + 0.5dp divider
│                                  │  ← 12dp gap
│  14:30          2h 15m 간격      │  ← Title Large + Body Small
│                                  │  ← 14dp vertical padding per row
│  12:15          3h 15m 간격      │     ← swipe left to reveal "삭제"
│                                  │
│  09:00                           │  ← 첫 기록은 간격 없음
│                                  │
│                                  │  ← 16dp section gap
│  어제 ───────────────────        │
│                                  │
│  23:45          9h 15m 간격      │
│  21:00                           │
│                                  │
│                                  │
│  ┌──────────────────────────┐    │  ← 24dp horizontal padding
│  │      + 수유 기록          │    │  ← 56dp height, 16dp radius
│  └──────────────────────────┘    │     Soft Coral bg, white text
│                                  │  ← 16dp bottom + nav bar inset
│         [navigation bar]         │
└──────────────────────────────────┘
```

### Empty State

```
┌──────────────────────────────────┐
│         [status bar]             │
│                                  │
│                                  │
│  첫 수유를                       │  ← Display Large (48sp), Bold
│  기록해보세요                    │     2줄, 좌측 정렬
│                                  │
│                                  │
│                                  │
│           🍼                     │  ← 48sp emoji
│                                  │
│     아직 기록이 없어요            │  ← Headline Small, Warm Gray
│                                  │
│     아래 버튼을 눌러              │  ← Body Medium, 70% alpha
│     첫 수유를 기록해보세요        │
│                                  │
│                                  │
│                                  │
│  ┌──────────────────────────┐    │
│  │      + 수유 기록          │    │
│  └──────────────────────────┘    │
│                                  │
└──────────────────────────────────┘
```

---

## Component Specifications

### 1. Background

- **Type**: Vertical gradient (Brush.verticalGradient)
- **Colors**: `#FFF8F5` → `#FFF1EB`
- **Edge-to-edge**: Yes (status bar, navigation bar 투명)

### 2. Elapsed Time Section

- **위치**: 상단, 좌측 정렬
- **Padding**: top 48dp, horizontal 24dp, bottom 16dp
- **서브 라벨**: "마지막 수유" - Body Medium, `#8E8685`
- **메인 시간**: Display Large (48sp), Bold, `#2D2626`, letterSpacing -1.5sp
- **보조 텍스트**: "전" - Headline Small, `#8E8685`
- **빈 상태**: "첫 수유를\n기록해보세요" (줄바꿈)

### 3. Date Section Header

- **구성**: 텍스트 + 수평 구분선
- **텍스트**: Label Medium, SemiBold, `#8E8685`, letterSpacing 0.5sp
- **구분선**: 0.5dp height, `#F0E8E4`
- **간격**: 텍스트와 구분선 사이 12dp
- **Padding**: top 8dp, bottom 12dp

### 4. Record Row (Swipeable)

- **높이**: auto (padding 14dp vertical, 4dp horizontal)
- **시간 텍스트**: Title Large (20sp), Medium weight, `#2D2626`
- **간격 텍스트**: Body Small (12sp), `#8E8685` at 70% alpha
- **시간과 간격 사이**: 12dp gap
- **Swipe 방향**: End to Start (왼쪽으로 스와이프)
- **삭제 배경**: Soft Red at 15% alpha
- **삭제 텍스트**: Label Medium, `#FF6B6B`, SemiBold

### 5. Bottom Action Button (CTA)

- **Type**: Material3 Button (not FAB)
- **높이**: 56dp
- **Shape**: RoundedCornerShape(16dp) - pill 느낌
- **배경색**: `#FF8A76` (Soft Coral)
- **텍스트색**: White
- **텍스트**: "+ 수유 기록", 17sp, SemiBold
- **Elevation**: 0dp (flat)
- **Margin**: horizontal 24dp, vertical 16dp + navigation bar inset
- **위치**: 화면 최하단 (thumb zone 최적화)

### 6. Delete Confirmation Dialog

- **배경색**: `#FFF8F5` (Cream White)
- **제목**: "기록 삭제" - Headline Small
- **본문**: "{HH:mm} 수유 기록을 삭제할까요?" - Body Large, `#8E8685`
- **확인 버튼**: "삭제" - `#FF6B6B`, SemiBold
- **취소 버튼**: "취소" - `#8E8685`

### 7. Empty State

- **위치**: 화면 중앙 (vertical center)
- **이모지**: 🍼 48sp
- **제목**: "아직 기록이 없어요" - Headline Small, `#8E8685`
- **설명**: 2줄, Body Medium, `#8E8685` at 70% alpha, center aligned

---

## Spacing System

| Token       | Value  | Usage                                |
|-------------|--------|--------------------------------------|
| xs          | 4dp    | 라벨과 메인 텍스트 사이              |
| sm          | 8dp    | 날짜 헤더 top padding, 리스트 gap     |
| md          | 12dp   | 날짜 헤더 bottom, 시간-간격 사이      |
| lg          | 16dp   | 섹션 간 간격, 하단 버튼 padding       |
| xl          | 24dp   | 수평 margin, 경과 시간 horizontal     |
| xxl         | 48dp   | 경과 시간 top padding                |

---

## Interaction Design

### Swipe to Delete
1. 사용자가 기록 행을 왼쪽으로 스와이프
2. 배경이 서서히 `#FF6B6B` 15% alpha로 변화 (animateColorAsState)
3. "삭제" 텍스트가 우측에 나타남
4. 40% 이상 스와이프 시 삭제 확인 다이얼로그 표시
5. 다이얼로그에서 최종 확인 후 삭제

### Button Press
- Elevation 변화 없음 (항상 0dp)
- Material3 기본 ripple effect 유지

---

## Design Decisions (vs. Previous)

| Before                        | After                          | Reason                              |
|-------------------------------|--------------------------------|-------------------------------------|
| TopAppBar "수유 기록"          | 제거                           | 공간 낭비, 앱 목적은 자명           |
| Card + elevation              | 플랫 행 (no card)              | 모던 미니멀 트렌드                  |
| CircleShape FAB (96dp)        | Pill Button (56dp, 하단)       | thumb zone 최적화                   |
| IconButton 삭제               | SwipeToDismiss                 | 더 깔끔한 UI, 실수 방지             |
| headlineLarge 28sp 경과 시간   | displayLarge 48sp 경과 시간     | 정보 계층 강화                       |
| Pastel Pink 계열               | Warm Cream + Soft Coral        | 더 세련되고 중성적인 톤             |
| 단색 배경                      | Subtle vertical gradient       | 깊이감 + 부드러운 분위기             |
