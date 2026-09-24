# UWA Social Feed (Technical Assessment)

A high-performance, single-screen Android social feed built with Jetpack Compose, Clean Architecture, Room persistence, and Paging 3 RemoteMediator, delivering an authentic offline-first experience with deterministic state handling.

---

## Deliverables & Quick Links

- **Download Debug APK:** [artifacts/app-debug.apk](artifacts/app-debug.apk) (Ready to install on Android API 24+)
- **Screen Recording / Demo:** 

https://github.com/user-attachments/assets/f5ca7b0f-d710-488c-9ccb-7c170d55d536





.
- **Specification Context:** [AGENTS.md](AGENTS.md) (Complete architectural brief and assessment rubric).

---

## 1. Features & Requirements Implemented

Every core requirement from the prompt has been fulfilled:
- **Author Identity:** User's name, profile photo (circular clip, Coil disk/memory caching, crossfade).
- **Post Content:** Post body text, optional full-bleed media images (`ContentScale.Crop`).
- **Metadata:** Location pin with city/country, relative time (`Just now`, `5m`, `2h`, `Yesterday`, `3d`).
- **Metrics & Reactions:** Social proof metrics row with authentic single/dual reaction badges, likes count, and comment count.
- **Optimistic Interactions:** Instant like/unlike toggle, floating 7-reaction drawer on long press, and an authentic Facebook-style expandable comment bottom sheet with real-time Room persistence.
- **Slide Down to Refresh:** Pull-to-refresh (`PullToRefreshBox`) integrated directly with Paging 3 and Room cache invalidation.
- **Brand Identity:** Styled with the official Uwa Social emerald and leaf green palette (`#1B873F`, `#22A447`, `#38B449`) matching `uwasocial.com`.

---

## 2. Architecture & Technical Decisions

The application strictly implements **MVVM with Clean Architecture** across three isolated layers:

```
Presentation (Compose UI + ViewModel) 
       ▼
Domain (Pure Kotlin Models + UseCases + Repository Interface)
       ▼
Data (Room Local Database + Remote Mock API + RemoteMediator)
```

### Key Architectural Choices from AGENTS.md

1. **Pure Kotlin Domain Layer:**
   The domain models (`Post`, `User`) and use cases have zero Android, Room, Retrofit, or Compose dependencies. This makes business logic fast and cheap to test on the local JVM without emulator overhead.

2. **Room as Single Source of Truth (Paging 3 RemoteMediator):**
   The UI never observes network responses directly. Network pages are written to Room SQLite inside atomic database transactions (`database.withTransaction`). Paging 3 automatically consumes Room's `PagingSource<Int, PostEntity>`, ensuring offline caching and pagination share a single unified mechanism.

3. **Explicit UI State Modeling:**
   UI states are modeled as a sealed interface (`FeedUiState`), enforcing clean separation:
   - `Loading`: Full-screen initial progress indicator.
   - `Content`: Active feed list of posts.
   - `Content(isOffline = true)`: Network is down, but cached posts are visible with a top `OfflineBanner`.
   - `Offline`: Network is down and Room has zero cached posts (full-screen retry view).
   - `Error`: Initial server failure with zero cached posts (`FullScreenErrorState`).
   - `AppendState.Error`: Later page fails while scrolling; loaded posts stay visible with an inline `[Retry]` row.
   - `Empty`: Zero posts returned by the API (`📭 No posts yet`).

4. **Image Caching (Coil):**
   Configured in `SocialFeedApp` with a 50MB disk cache and memory cache for resilient offline image browsing.

5. **Layered Error Handling:**
   Network and HTTP exceptions are mapped at the repository boundary into clean, user-facing error copy without leaking raw framework exceptions.

---

## 3. Reviewer Testing Controls

A reviewer testing menu is available via the **three-dots icon in the top app bar** to test all required edge states deterministically:

- **Simulate Server Error (Page 1):** Clears the cache and triggers an HTTP 500 failure to demonstrate the full-screen `ErrorState` with the `[Try Again]` recovery button.
- **Simulate Next-Page Error:** Arms page 2+ with an error and emits a snackbar cue. Scrolling to post 10 demonstrates the non-destructive inline `Couldn't load more posts. [Retry]` footer.
- **Simulate Empty State:** Clears posts to demonstrate `EmptyState` (`📭 No posts yet`). Tapping `[Check Again]` restores the feed.
- **Simulate Offline:** Toggle Airplane Mode. Notice the `OfflineBanner` displays above cached posts.

---

## 4. Testing & Verification

The project includes **25 passing unit tests** across 5 test classes (exceeding the required minimum of 3):

1. **`PostMapperTest` (3 tests):** Validates DTO, Entity, and Domain conversions, ISO-8601 parsing, and null safety.
2. **`ToggleLikeUseCaseTest` (2 tests):** Tests like incrementing, unliking decrement, and state immutability.
3. **`TimeFormatterTest` (6 tests):** Tests relative timestamps across all ranges (`Just now`, `5m`, `2h`, `Yesterday`, `3d`, formatted dates).
4. **`FeedViewModelTest` (10 tests):** Tests UI state machine mappings, offline banner logic, append errors, and reaction retention.
5. **`FeedRemoteMediatorTest` (4 tests):** Tests Room transactions on `REFRESH`, pagination end detection on `APPEND`, and error propagation.

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

---

## 5. How to Run

1. Clone repository: `git clone https://github.com/Chrisroid/u-w-a-test.git`
2. Open in Android Studio (Ladybug / Meerkat or later with JDK 17).
3. Sync Gradle and run the `app` target on an emulator or physical device (API 24+).
