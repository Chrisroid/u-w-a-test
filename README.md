# UWA Social Feed

A single-screen Android social feed built with Jetpack Compose, Clean Architecture, Room persistence, and Paging 3 RemoteMediator, delivering a seamless offline-first experience with deterministic state handling.

---

## 1. Overview
This project delivers a resilient, paginated social feed screen following modern Android best practices. It consumes paginated post data from a simulated network source, persists all records to a local Room database as the single source of truth, caches media via Coil, and cleanly handles edge states including loading, server errors, empty feeds, and offline scenarios (both with and without existing local cache). Users can browse rich posts, view relative timestamps, and optimistically toggle likes with immediate feedback and automatic rollback on failure.

---

## 2. Architecture
The codebase strictly follows MVVM combined with lightweight Clean Architecture across presentation, domain, and data layers:

```
                 ┌──────────────────┐
                 │   Compose UI     │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │    ViewModel     │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │     UseCase      │
                 └────────┬─────────┘
                          ▼
                 ┌──────────────────┐
                 │   Repository     │  (interface in domain,
                 └───────┬───┬──────┘   impl in data)
                         │   │
                ┌────────┘   └────────┐
                ▼                     ▼
        ┌──────────────┐      ┌──────────────┐
        │  Remote API  │      │     Room     │
        │  (mocked)    │      │              │
        └──────────────┘      └──────────────┘
```

### Why Clean Architecture and MVVM at this scale?
1. **Decoupled Business Rules:** The domain layer (`Post`, `User`, `PostRepository`, `GetFeedUseCase`, `ToggleLikeUseCase`) contains zero Android, Room, Retrofit, or Compose dependencies. This makes domain logic cheap and fast to test without mocks or instrumented environments.
2. **Pluggable Mock Boundaries:** The UI communicates solely through repository and use-case interfaces. Swapping the in-memory fake API for a live production Retrofit service requires zero changes outside the data layer.
3. **Reactive UI State Flow:** The presentation layer consumes an immutable `StateFlow<FeedUiState>` and `Flow<PagingData<Post>>`, preventing state inconsistency.

---

## 3. Pagination
Pagination is powered by **Android Jetpack Paging 3** using a `RemoteMediator` synchronized with a Room `PagingSource`:
- **Single Source of Truth:** The UI never directly consumes network responses. Network pages are written directly into Room inside a database transaction (`database.withTransaction`). Room then automatically invalidates and emits a fresh slice to the UI.
- **Unified Pagination and Offline Caching:** Because Paging 3 reads from Room's generated `PagingSource<Int, PostEntity>`, offline caching is not a separate ad-hoc mechanism. It is inherently part of the pagination pipeline.
- **Cursor and Key Management:** A dedicated `remote_keys` table tracks `prevKey` and `nextKey` per post item. This eliminates key collisions and race conditions between page appends and in-place row mutations.
- **Configuration:** Configured with `pageSize = 10`, `prefetchDistance = 5`, and `enablePlaceholders = false`.

---

## 4. State Handling
UI states are represented as a single sealed interface (`FeedUiState`), avoiding invalid combinations of independent booleans:

```kotlin
sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Error(val message: String) : FeedUiState
    data object Offline : FeedUiState // no network AND no cache
    data class Content(
        val posts: List<Post> = emptyList(),
        val isOffline: Boolean = false, // network down, cache shown
        val appendState: AppendState = AppendState.Idle
    ) : FeedUiState
}
```

### Offline With Cache vs. Offline Without Cache
- **Offline with Cache:** When the network is unavailable (`NetworkMonitor.isOnline == false`) but posts exist in Room, the screen renders `FeedUiState.Content(isOffline = true)`. A non-intrusive `OfflineBanner` displays "Offline - Showing cached posts" at the top while the list remains fully scrollable.
- **Offline with Zero Cache:** If the initial fetch fails and local Room storage is completely empty, the screen displays `FeedUiState.Offline` with a centered icon, clear explanation, and a "Retry" button.

---

## 5. Layered Error Handling
Errors are caught and classified at the repository boundary:
- `IOException` is categorized as a connectivity failure.
- HTTP / server errors are converted into human-readable user messages ("We couldn't load the posts. Please try again.").
- Raw system exceptions (such as `SocketTimeoutException` or SQLite errors) never leak into the UI layer.

### Initial Load Error vs. Next-Page Append Error
- **Initial Load Failure (Empty List):** The entire screen transitions to `FeedUiState.Error` with a full-screen retry action.
- **Next-Page Append Failure (Page N):** Previously loaded posts stay visible in the feed. An inline error row appears at the bottom of the feed displaying "Couldn't load more posts." alongside an inline [Retry] button. This ensures a failed pagination request never destroys user progress.
- **No Infinite Loops:** Automatic retry loops on error are strictly avoided; retries are exclusively user-initiated.

---

## 6. Image Caching
Image loading is handled via **Coil**:
- **Disk and Memory Caching:** The application-level `ImageLoader` in `AppContainer` configures an explicit disk cache capped at 50MB in the app cache directory, alongside a 25% heap memory cache.
- **Best-Effort Offline Fallback:** In offline mode, Coil serves images that were previously cached to disk. For images not yet cached, a clean placeholder / fallback surface is displayed without disrupting card layout.
- **Performance:** `AsyncImage` uses `ContentScale.Crop`, rounded clipping, and crossfade transitions to prevent UI jank during scrolling.

---

## 7. Like & Comment Interactions (Facebook Feed Fidelity)
- **Optimistic Likes & Reactions:** Tapping the Like button triggers an immediate local database update via `ToggleLikeUseCase` and `PostRepository.toggleLike()`. Long-pressing anchors a floating 7-reaction drawer (Like 👍, Love ❤️, Care 🥰, Haha 😆, Wow 😮, Sad 😢, Angry 😡) with custom styling and badges.
- **Facebook Feed Card Architecture:** Styled with Facebook's classic card layout, featuring cool-grey gutters (`#F0F2F5`), full-bleed media images, social proof summary row, and a 3-action footer bar (Like, Comment, Share).
- **Interactive Comment Bottom Sheet:** Tapping "Comment" or the comments count opens an authentic Facebook-style expandable `ModalBottomSheet`. It features rounded grey bubble comment cards (`#F0F2F5`), author avatars, timestamps, comment likes, and a sticky bottom input bar with an active send button that adds comments in real time and updates the post count in Room.
- **Native Share Sheet:** Tapping "Share" triggers Android's native share sheet (`Intent.ACTION_SEND`), pre-populating author and post text.
- **Local Simulation:** Because the mock API does not expose a remote mutation backend, mutations are persisted locally in Room. Room table updates invalidate the post's state, updating counts and icons instantly.
- **Rollback and Feedback:** If a database write fails, the UI rolls back the optimistic state and emits a transient Material 3 `Snackbar` ("Couldn't update like. Please try again."), preserving list stability.
- **Accessibility:** The like control provides contextual content descriptions ("Like post" / "Unlike post") rather than communicating state through color alone.

---

## 8. Testing
The test suite contains **25 unit tests** across 5 test suites running on the local JVM:

1. **`PostMapperTest` (3 tests):** Verifies DTO to Room Entity to Domain conversions, ISO-8601 timestamp parsing, graceful fallback on malformed dates (`Instant.EPOCH`), and null safety for media and location fields.
2. **`ToggleLikeUseCaseTest` (2 tests):** Tests that liking increments count and flags `isLiked = true`, while unliking reverses both values.
3. **`TimeFormatterTest` (6 tests):** Validates relative time formatting across all ranges (`Just now`, `5m`, `2h`, `Yesterday`, `3d`, and full dates).
4. **`FeedViewModelTest` (10 tests):** Validates UI state mapping for `Loading`, `Content`, `Empty`, `Offline` (zero cache), `Content(isOffline = true)` (cached items), full-screen `Error`, inline `AppendState.Error`, like action delegation, and reaction state retention across scroll cycles.
5. **`FeedRemoteMediatorTest` (4 tests):** Tests `REFRESH` transaction (clearing cache, inserting entities and remote keys), `APPEND` end-of-pagination detection, `PREPEND` no-op, and `MediatorResult.Error` on network failure.

### Running Unit Tests
Execute via Gradle:
```bash
./gradlew test
```

---

## 9. Trade-offs and Future Considerations
1. **Flattened `PostEntity` vs. Relational Users Table:** For this single-screen scope, author details (`userId`, `userName`, `profileImageUrl`) are flattened into `PostEntity`. In a production app with multiple screens and user profiles, authors would live in a normalized `users` table linked via foreign keys.
2. **Cache Eviction and TTL:** In accordance with the assessment requirements, cache entries persist until the next manual refresh (`REFRESH` load type clears tables). In a production product, an explicit TTL (Time-To-Live) table or cache expiry policy (e.g. 24 hours) would be introduced.
3. **In-Memory Mock API vs. MockWebServer:** An in-memory `MockPostApi` with simulated delay (`delay(400ms)`) was chosen over OkHttp `MockWebServer`. This provides 100% deterministic test execution without socket flakiness or port collisions, while allowing interactive runtime simulation via UI controls.
4. **Dependency Injection:** Built using an explicit `AppContainer` dependency container and `ViewModelProvider.Factory`. This delivers full Clean Architecture decoupling while guaranteeing complete compatibility across bleeding-edge Gradle and AGP versions.

---

## 10. How to Run
1. Open the project in Android Studio (Ladybug / Meerkat or later with JDK 17).
2. Sync Gradle files.
3. Run the `app` configuration on an Android Emulator or physical device (API 24+).
4. Run unit tests directly via `./gradlew test`.

### Reviewer Testing Controls
To evaluate all required UI states easily, tap the three-dots menu in the top app bar:
- **Slide Down to Refresh:** Swipe down from the top of the feed to trigger a pull-to-refresh sync with an emerald brand spinner.
- **Simulate Server Error (Page 1):** Toggles a simulated 500 error on the initial load to trigger the full-screen `ErrorState`.
- **Simulate Next-Page Error:** Toggles an error when scrolling to page 2+ to demonstrate the inline pagination retry row while preserving loaded posts.
- **Simulate Empty State:** Toggles an empty response from the API to demonstrate the `EmptyState`.
- **Offline Testing:** Toggle Airplane Mode on the device or emulator. If posts are cached, notice the top banner appears. If cache is cleared, the full-screen `Offline` state appears.
