# Changelog — Baby Feeding Tracker

## 2026-03-31: 전체 세션 작업 기록

### Cycle 1: 기능 개선
- 삭제 버그 수정 (SwipeToDismiss → 바텀시트 내 삭제 버튼)
- 수유 종류(모유/분유) + 분유 용량 기록 기능 추가
- 테마 컬러 변경 (코랄 → 민트/연두)
- 타임라인 UI (동그라미 + 세로선, 날짜별 그룹)

### Cycle 2: UX 개선 (실사용 피드백)
- 경과 시간 "2시간 15분" + "전" 분리 표시 → 한 줄 통합
- 수유 기록 추가 후 바텀시트 자동 오픈
- 모유 선택 시 즉시 닫힘, 분유+용량 선택 시 즉시 닫힘 (신규 기록만)

### Cycle 3: Firebase + Firestore 전환
- Room/KSP 제거, Firebase Auth + Firestore로 전환
- 익명 로그인, Firestore 오프라인 퍼시스턴스
- Repository 인터페이스 유지, 내부만 교체
- FeedingRecord.id: Long → String

### Cycle 4: 배우자 공유 기능
- Google Sign-In + 익명 계정 연결 (linkWithCredential)
- UserRepository (프로필, 초대 코드 생성/검증, 공유 상태)
- ShareBottomSheet (4개 상태: 미로그인/미연결/코드생성/연결됨)
- 공유 아이콘 + 상태 dot (초록/빨간)
- Firestore 보안 규칙 (파트너 접근 허용)

### Cycle 5: 리뷰 + Critical 버그 수정
- 리뷰어에 의한 전체 코드/프로세스 리뷰
- redeemInviteCode WriteBatch 원자성 적용
- 초대 코드 충돌 검증 추가
- sharingState Flow 에러 처리
- redeem 후 AppContainer 경로 전환 누락 수정

### Cycle 6: 추가 기능 + 버그 수정
- 일일 통계 (총 X회 / 모유 X회 / 분유 X회·Xml)
- 분유 용량 버튼 10ml 단위로 변경 (2줄 배치)
- 앱 재설치 후 Google 로그인 시 데이터 복원 수정
  - linkWithCredential 실패 시 signInWithCredential 폴백
  - Repository 재초기화 + ViewModel key 기반 재생성

---

## 커밋 이력 (31개)

```
96c3669 fix: recreate ViewModel when repository changes after Google login
907a9aa fix: handle app reinstall — fallback to signInWithCredential
0ccc117 feat: add daily stats summary and change formula amount to 10ml increments
1780baa fix: critical bugs — atomic WriteBatch, code collision, Flow error, data path reinit
ca18bd9 docs: add comprehensive project review report
ef8524a feat: add share icon with status dot to MainScreen
6b3fa0c feat: handle Google Sign-In result and pass all dependencies to ViewModel
267deae feat: add ShareBottomSheet with 4 sharing states UI
8e4a141 feat: add sharing state, login state, and invite code methods to MainViewModel
a11cff9 feat: wire UserRepository and GoogleAuthHelper in AppContainer
ad0e2d4 feat: add UserRepository for profile and invite code management
4693bca feat: add GoogleAuthHelper for Google Sign-In and anonymous account linking
e0d2a22 build: add play-services-auth for Google Sign-In
7087ff2 docs: add sharing feature implementation plan
c8fe811 docs: add sharing feature design spec (sub-project 2)
aeef486 fix: update Compose BOM to 2024.06.00 to fix Firebase version conflict
10ce0bd feat: add INTERNET permission for Firebase
1447be9 feat: change recordId type from Long to String for Firestore
be40323 feat: add loading state while Firebase auth initializes
a689111 feat: wire FeedingRepository to FirestoreDataSource with anonymous auth
9337dbe feat: add FirestoreDataSource with CRUD and Flow support
59435cb build: add Firebase dependencies, remove Room/KSP
afba560 feat: remove Room, change FeedingRecord id to String for Firestore
2bb20e0 docs: add Firebase + Firestore setup design spec (sub-project 1)
e357e6a docs: add lessons learned from first user feedback session
4e99a5c feat: elapsed time one-line display, auto-open/close bottom sheet
ffa056d feat: add lastAddedRecord StateFlow for auto-open bottom sheet
f4f6192 feat: return inserted record from addRecord
3e1f21b docs: add UX improvements design spec
bc5cfa9 feat: replace swipe-delete with bottom sheet, add timeline UI
fe3d949 feat: change theme accent color from coral to mint green
90f05f0 feat: add updateRecordType method to MainViewModel
df69373 feat: add type and amountMl fields to FeedingRecord with Room migration
57af010 initial: existing baby feeding tracker codebase
```

---

## 프로젝트 문서 목록

| 문서 | 위치 |
|------|------|
| 아키텍처 | `docs/architecture.md` |
| Lessons Learned | `docs/lessons-learned.md` |
| 리뷰 보고서 | `docs/review-report.md` |
| 기능 개선 설계 | `docs/superpowers/specs/2026-03-31-feeding-tracker-improvements-design.md` |
| UX 개선 설계 | `docs/superpowers/specs/2026-03-31-ux-improvements-design.md` |
| Firebase 전환 설계 | `docs/superpowers/specs/2026-03-31-firebase-firestore-setup-design.md` |
| 공유 기능 설계 | `docs/superpowers/specs/2026-03-31-sharing-feature-design.md` |
| 기능 개선 구현 계획 | `docs/superpowers/plans/2026-03-31-feeding-tracker-improvements.md` |
| UX 개선 구현 계획 | `docs/superpowers/plans/2026-03-31-ux-improvements.md` |
| Firebase 전환 구현 계획 | `docs/superpowers/plans/2026-03-31-firebase-firestore-setup.md` |
| 공유 기능 구현 계획 | `docs/superpowers/plans/2026-03-31-sharing-feature.md` |
