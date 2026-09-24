# AGENTS.md — Android Social Feed (Technical Assessment)

This is the standing context file for this project. Read it fully before
starting work. It defines scope, architecture, exact contracts, and the
checklist your work will be verified against. Where this file gives a
concrete spec (JSON shape, wireframe, error copy), treat it as binding, not
illustrative. Where it gives rationale ("why"), use it to resolve ambiguity
you hit that isn't explicitly specified — don't invent scope instead.

Recommended autonomy profile for this project: **review-driven development**
(balanced autonomy with checkpoints) — this is a reviewed assessment
deliverable, not a throwaway prototype; check in before big structural
deviations from §10, but proceed autonomously within it.

---

## 0. What "done" means, in one paragraph

A reviewer can clone this project, open it in Android Studio, run it, read
the README and understand the architecture in under 5 minutes, scroll a
paginated feed of posts with images/likes/comments/location/time, turn off
the network and see cached posts with an offline banner (or a clean
no-cache offline screen if nothing's cached), trigger and retry an error,
like/unlike a post, and run `./gradlew test` and see ≥3 passing unit tests
that actually exercise the architecture, not trivial assertions.

Recommended completion budget: **2–4 hours.** Treat §14 (priority order) and
§15 (cut list) as load-bearing — they exist specifically to stop scope creep
on a project type that invites it.

---

## 1. Requirements Recap

Build a single-screen Android social feed that:

- Fetches **paginated posts** from a mock API (no real backend required)
- Each post displays: user name + profile photo, post text + optional
  media, location + relative time, like count + comment count, and a
  tappable like control
- Explicitly handles: **loading, empty, offline, error** states (including
  the offline-with-cache vs offline-with-nothing distinction, and
  first-page-error vs next-page-error distinction)
- Has **image caching**, **basic offline caching (Room)**, and **layered
  error handling**
- Ships **≥3 unit tests** (more is fine, don't pad)
- Ships a **README.md** explaining architecture, decisions, and trade-offs

Deliberately **out of scope** — do not build these even if a "complete" app
would have them: comment thread/detail screen, post creation, auth/login,
a real backend, cache TTL/expiry logic, dark theme/design system polish,
pagination prefetch tuning beyond Paging3 defaults. If you find yourself
building any of these, stop — it's scope creep, not thoroughness.

---

## 2. Tech Stack (use exactly this; don't substitute without checking in)

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Standard |
| UI | Jetpack Compose + Material 3 | Fastest correct path to one polished screen |
| Architecture | MVVM + lightweight Clean Architecture (presentation → domain → data) | Domain has zero Android/Retrofit/Room deps → cheap, fast unit tests; clean mock-API swap point |
| Async | Coroutines + Flow/StateFlow | Idiomatic pagination + reactive state |
| DI | Hilt | Removes boilerplate; drop to manual DI only if Hilt setup is visibly eating your time budget |
| Pagination | Paging 3 (`androidx.paging`) | `RemoteMediator` + Room `PagingSource` gives pagination AND offline caching from one mechanism — this is the central architectural bet of the project |
| Networking | Retrofit interface (`PostApi`), backed by a mock implementation | Interface documents the "real" contract even though it's mocked; see §7 for which mock strategy |
| Local persistence | Room | Source of truth the UI always reads from (never read network directly in the UI layer) |
| Image loading | Coil, `AsyncImage` | Built-in memory + disk cache, Compose-native |
| Connectivity | `ConnectivityManager` wrapped as a `Flow<Boolean>` | Drives the explicit offline state instead of inferring it only from failed calls |
| Testing | JUnit4 + MockK + kotlinx-coroutines-test (+ Turbine optional) | Standard Flow/ViewModel test combo |

Do not add a library that doesn't solve a concrete problem in this list.

---

## 3. Architecture

```
                 ┌──────────────────┐
                 │   Compose UI      │
                 └────────┬──────────┘
                          ▼
                 ┌──────────────────┐
                 │    ViewModel      │
                 └────────┬──────────┘
                          ▼
                 ┌──────────────────┐
                 │     UseCase       │
                 └────────┬──────────┘
                          ▼
                 ┌──────────────────┐
                 │   Repository      │  (interface in domain,
                 └───────┬───┬───────┘   impl in data)
                         │   │
                ┌────────┘   └────────┐
                ▼                     ▼
        ┌──────────────┐      ┌──────────────┐
        │  Remote API   │      │     Room      │
        │  (mocked)     │      │              │
        └──────────────┘      └──────────────┘
```

**Hard rule:** domain has no Retrofit/Room/Compose/Android imports. The UI
never talks to Retrofit or Room directly — always through
ViewModel → UseCase → Repository. This is what makes the ViewModel/UseCase
tests fast (fake the repository interface) and what makes swapping the mock
API for a real one later a data-layer-only change.

### Package structure

```
com.example.socialfeed/
├── MainActivity.kt
├── SocialFeedApp.kt                    (@HiltAndroidApp)
│
├── domain/
│   ├── model/         Post.kt, User.kt          (pure Kotlin, no framework deps)
│   ├── repository/    PostRepository.kt          (interface)
│   └── usecase/       GetFeedUseCase.kt, ToggleLikeUseCase.kt
│
├── data/
│   ├── local/          PostEntity.kt, PostDao.kt, RemoteKeysEntity.kt,
│   │                    RemoteKeysDao.kt, AppDatabase.kt
│   ├── remote/         PostApi.kt, PostDto.kt, MockPostApi.kt (see §7)
│   ├── mapper/         PostMapper.kt (Dto ↔ Entity ↔ Domain)
│   ├── paging/          FeedRemoteMediator.kt
│   ├── connectivity/    NetworkMonitor.kt
│   └── repository/      PostRepositoryImpl.kt
│
├── presentation/feed/
│   ├── FeedViewModel.kt
│   ├── FeedUiState.kt
│   ├── FeedScreen.kt
│   └── components/  PostCard.kt, LoadingView.kt, EmptyState.kt,
│                     ErrorState.kt, OfflineBanner.kt
│
├── di/   NetworkModule.kt, DatabaseModule.kt, RepositoryModule.kt
│
src/test/java/com/example/socialfeed/
├── data/    PostMapperTest.kt, PostRepositoryTest.kt, FeedRemoteMediatorTest.kt
├── domain/  ToggleLikeUseCaseTest.kt
└── presentation/  FeedViewModelTest.kt
```

Keep this structure. Don't split every three lines into a new file, and
don't collapse it into a single 500-line `FeedScreen.kt` either.

---

## 4. Domain Models

```kotlin
data class Post(
    val id: String,
    val user: User,
    val text: String,
    val mediaUrl: String?,
    val location: String?,
    val createdAt: Instant,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean
)

data class User(
    val id: String,
    val name: String,
    val profileImageUrl: String?
)
```

DTOs never leak past `data/mapper/`. The domain model represents exactly
what the UI/use cases need — nothing more.

---

## 5. Mock API Contract

Model requests/responses on this exact shape, whichever mock strategy (§7)
you use:

```
GET /posts?page=1&pageSize=10
```

```json
{
  "posts": [
    {
      "id": "post_001",
      "user": {
        "id": "user_001",
        "name": "John Doe",
        "profileImage": "https://..."
      },
      "text": "Having a great day!",
      "media": { "type": "image", "url": "https://..." },
      "location": "Lagos, Nigeria",
      "createdAt": "2026-09-24T12:30:00Z",
      "likesCount": 124,
      "commentsCount": 32,
      "likedByCurrentUser": false
    }
  ],
  "page": 1,
  "pageSize": 10,
  "hasNextPage": true
}
```

Seed ~40–60 posts across 3+ pages, varied: some with no media, some with
long text, some with 0 likes/comments (exercises edge states honestly).
Media/profile photos can be picsum.photos or similar static placeholder
URLs.

---

## 6. Repository Interface

```kotlin
interface PostRepository {
    fun getPosts(): Flow<PagingData<Post>>
    suspend fun toggleLike(postId: String)
}
```

The UI/ViewModel must never know or care whether posts came from Retrofit,
Room, or the mock source — that's entirely the repository's job.

---

## 7. Mock API Strategy — pick one, document the choice in the README

**Preferred for this time budget:** a local fake (`FakePostRemoteDataSource`
or `MockPostApi`) implementing the `PostApi` contract in-memory, with
`delay(300–800ms)` to simulate latency, and a way to force an error/empty
response (a debug flag or hidden long-press) so loading/error/empty states
are demoable without physically disabling network.

**Alternative if you have time / prefer reproducibility:** OkHttp
MockWebServer serving JSON fixtures from `testdata/posts_page_N.json`. Same
architecture either way — only the `remote/` implementation changes. Note
whichever you pick, and why, in the README.

---

## 8. Pagination (Paging 3)

- `PostDao` exposes a Room-generated `PagingSource<Int, PostEntity>`.
- `FeedRemoteMediator : RemoteMediator<Int, PostEntity>`:
  - `REFRESH`: clear + reload page 1 (initial load / pull-to-refresh only)
  - `APPEND`: fetch next page using a cursor/page number tracked in
    `RemoteKeysEntity`, insert into Room
  - `PREPEND`: `endOfPaginationReached = true` (feed never prepends)
  - On failure → `MediatorResult.Error`, surfaced by Paging3 as
    `LoadState.Error` on `append` — UI shows an inline retry row, existing
    posts stay visible. `REFRESH` failure with no cached rows is the only
    case that shows the full-screen error state.
- `PagingConfig(pageSize = 10–15, prefetchDistance = 5,
  enablePlaceholders = false)`
- Stable item keys in the UI: `items(items = posts, key = { it.id })`
- The UI must never request the same page twice, and must not duplicate
  posts on refresh.

---

## 9. UI States — exact specification

Model as one sealed interface, not independent booleans:

```kotlin
sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Error(val message: String) : FeedUiState
    data object Offline : FeedUiState               // no network AND no cache
    data class Content(
        val posts: List<Post>,
        val isOffline: Boolean = false,              // network down, cache shown
        val appendState: AppendState = AppendState.Idle
    ) : FeedUiState
}
sealed interface AppendState {
    data object Idle : AppendState
    data object Loading : AppendState
    data class Error(val message: String) : AppendState
}
```

Derive most of this from Paging3's own `LoadState` (`refresh`, `append`,
`prepend`) rather than reinventing it — map `LoadState` into the sealed
interface above.

### 9.1 Loading (initial)
```
        CircularProgressIndicator
        Loading posts...
```
Skeleton placeholders are a nice-to-have, not required.

### 9.2 Content
Standard scrollable feed of `PostCard`s.

### 9.3 Empty (API returns zero posts successfully)
```
┌──────────────────────────────┐
│            📭                 │
│       No posts yet            │
│  Check back later for new     │
│  posts.                       │
└──────────────────────────────┘
```

### 9.4 Offline — WITH cached posts
```
┌───────────────────────────────┐
│ ⚠ Offline                     │
│ Showing cached posts          │
└───────────────────────────────┘
Post
Post
Post
```
Small banner, feed still scrollable. This is `Content(isOffline = true)`,
**not** the full-screen `Offline` state.

### 9.5 Offline — WITHOUT cached posts
```
┌──────────────────────────────┐
│        No connection          │
│  Connect to the internet      │
│  and try again.               │
│          [Retry]              │
└──────────────────────────────┘
```

### 9.6 Error (server/API failure, first load)
```
Something went wrong.
We couldn't load the posts.
[Try Again]
```

### 9.7 Pagination error (page 1 ok, page N fails)
```
Post
Post
Post
Could not load more posts.
[Retry]
```
Previously loaded posts must remain visible — never blank the list because
a later page failed.

**Do not auto-retry the API on a loop.** Retry is always user-initiated.

---

## 10. Error Handling — layered, exact copy

- Domain/data layer never throws across boundaries — map once at the
  repository boundary: `IOException` → network error, HTTP 4xx/5xx →
  server error, anything else → unknown error. Never surface raw exception
  text (`SocketTimeoutException...`) to the user.
- User-facing copy:
  - Network: `No internet connection. Showing cached posts.`
  - Server: `We couldn't load the posts. Please try again.`
  - Empty: `No posts yet. Check back later.`
  - Pagination: `Couldn't load more posts.`
- Like action failure: optimistic update rolls back + a `Snackbar`. Do not
  transition the whole screen to an error state over a failed like.

---

## 11. Offline Caching (Room)

```kotlin
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val profileImageUrl: String?,
    val text: String,
    val mediaUrl: String?,
    val location: String?,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val likedByCurrentUser: Boolean
)
```

Flattening user fields into `PostEntity` instead of a relational
user table is intentional for this scope — state this explicitly in the
README rather than silently doing it.

Flow: network succeeds → write to Room → UI reads from Room via
`PagingSource` (always — the UI never reads network directly). Network
fails → UI still reads Room; `NetworkMonitor` + cache-presence together
decide between §9.4 and §9.5. No TTL/expiry — cache persists until the
next successful `REFRESH`. This is a deliberate scope cut; say so in the
README's trade-offs section, don't build TTL logic.

---

## 12. Image Caching (Coil)

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(post.user.profileImageUrl)
        .crossfade(true)
        .build(),
    contentDescription = "${post.user.name} profile photo"
)
```

- Confirm Coil's disk cache is enabled with a reasonable size (~50MB) in
  the `ImageLoader` built in DI.
- Always set a placeholder and an error/fallback drawable — a bad URL must
  never blank a row.
- Post images: full width, `ContentScale.Crop`, rounded corners.
- Offline image behavior is best-effort (Coil disk cache serves
  previously-loaded images only) — say this in the README, don't try to
  guarantee full offline image availability.

---

## 13. Like Interaction

```
Before:  ♡ Like        124 likes
Tap
After:   ♥ Liked        125 likes
```

Optimistic UI update on tap; `ToggleLikeUseCase` → repository call is local
/simulated since there's no real backend mutation endpoint — **state this
explicitly in the README** so it doesn't read as an oversight. No auth
system for this.

`contentDescription` on the like control must reflect state: `"Like post"`
/ `"Unlike post"`.

---

## 14. Build Order (follow this sequence)

**Priority 1 — core functionality**
1. Project setup, Compose config
2. Domain models
3. Mock API (§7)
4. Retrofit interface + mapper
5. Repository implementation
6. Paging wiring (RemoteMediator + Room PagingSource)
7. Feed renders on screen

**Priority 2 — required UX**
8. Loading state · 9. Error state · 10. Empty state · 11. Offline state
   (both variants) · 12. Like interaction

**Priority 3 — persistence**
13. Room caching wired to offline states

**Priority 4 — quality**
14. Coil caching confirmed · 15. Unit tests (§16) · 16. README (§17)
17. UI polish (spacing, icons, accessibility labels)

---

## 15. What to cut first if time runs short

In order: extra unit tests beyond the required 3 → debug toggles for
forcing error states (document manually in README instead) → offline
banner visual polish → like-rollback-on-failure Snackbar → pull-to-refresh
→ skeleton loading placeholders. Never cut: pagination, the offline/error/
empty state distinctions in §9, or the README.

---

## 16. Testing — required ≥3, aim for 5–7

All of these work against fakes (fake `PostRepository`, fake
`NetworkMonitor`) — no Robolectric/instrumentation needed for ViewModel/
UseCase/mapper tests.

1. **`PostMapperTest`** — DTO → domain mapping correctness (esp. timestamp/
   location handling)
2. **`ToggleLikeUseCaseTest`** — liking increments count + sets `isLiked`;
   unliking reverses it
3. **`FeedViewModelTest`** — `Loading` → `Content` on success (collect
   `uiState` with Turbine or `runTest`)
4. **`FeedViewModelTest`** (separate test) — repository/mediator error maps
   to `Error`; `NetworkMonitor` disconnected + empty cache maps to `Offline`
5. **`FeedRemoteMediatorTest`** — `REFRESH` clears + repopulates; `APPEND`
   sets `endOfPaginationReached` when the source returns fewer than
   `pageSize` items
6. *(if time allows)* Relative-time formatter test (`5m`, `2h`, `Yesterday`,
   `3d`) — keep the formatter outside the Composable specifically so it's
   testable
7. *(if time allows)* `PostDaoTest` with in-memory Room DB

Run with `./gradlew test`.

---

## 17. README.md — required sections

1. **Overview** — one paragraph
2. **Architecture** — the diagram from §3 + one paragraph on why Clean
   Architecture/MVVM at this size (mainly: cheap tests, clean mock-API
   swap point)
3. **Pagination** — Paging3 + RemoteMediator + Room, and why this one
   mechanism also provides offline caching
4. **State handling** — the sealed `FeedUiState`, and the offline-with-
   cache vs offline-with-nothing distinction
5. **Error handling** — layer-by-layer mapping; refresh-error vs
   append-error UX difference
6. **Image caching** — Coil defaults, offline best-effort caveat
7. **Like interaction** — explicitly state it's local/simulated (§13)
8. **Testing** — what's covered, how to run
9. **Trade-offs / what I'd do with more time** — flattened `PostEntity`
   (§11), no cache TTL, in-memory mock vs MockWebServer (§7), no comment
   thread, no auth, minimal accessibility/design polish
10. **How to run** — build/run instructions, and how to trigger the
    error/offline states for review if you built a debug toggle

---

## 18. Coding Style

- `val` over `var` unless mutation is actually needed
- `data class` for models; prefer immutability
- Meaningful names (`posts`, `nextPageKey`, `cachedPosts`,
  `networkError`) — never `data1`, `temp`, `foo`
- Keep Composables focused (`PostCard`, `PostHeader`, `PostMedia`,
  `PostActions` — not one 500-line `FeedScreen`), but don't fragment every
  three lines into a new file either
- No secrets/tokens/credentials hardcoded anywhere
- Meaningful `contentDescription` on all images/icons; don't communicate
  state through color alone

---

## 19. Definition of Done (self-check before calling this complete)

**Feed**
- [ ] Posts load from mock API — name, photo, text, optional media,
      location, relative time, likes, comments all render
**Interaction**
- [ ] Like/unlike works and updates the count optimistically
**Pagination**
- [ ] Page 1 loads; further pages load on scroll; bottom loading indicator
      shown; pagination errors are retryable without losing loaded posts
**States**
- [ ] Loading, Content, Empty, Offline (both variants), Error, and
      pagination-error all implemented per §9 exactly
**Caching**
- [ ] Room stores posts; cached posts render offline with the banner;
      Coil image caching confirmed enabled
**Testing**
- [ ] ≥3 unit tests, all passing, matching §16 in spirit
**Documentation**
- [ ] README covers all 10 sections in §17

If any box is unchecked, that's the next task — not a note for later.