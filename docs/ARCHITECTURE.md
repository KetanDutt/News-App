# Architecture

The app follows a **single-activity-per-screen** structure with **MVVM** and the
recommended **Room-as-single-source-of-truth** pattern. Everything is written in
Kotlin with coroutines + LiveData (no third-party state frameworks).

```
┌─────────────────────────────── UI layer ────────────────────────────────┐
│  MainActivity (tabs + ViewPager2)     SearchActivity (search)           │
│    └─ NewsListFragment (×7 tabs)      SavedNewsActivity (bookmarks)     │
│         CarouselAdapter / NewsListAdapter  (DiffUtil, ListAdapter)      │
│         ReadNewsActivity (WebView + TTS, launched with article extras)   │
└───────────────▲───────────────────────────────────┬─────────────────────┘
                │ LiveData<…UiState>, LiveData<List<NewsModel>>
┌───────────────┴──────────── ViewModel layer ───────┴────────────────────┐
│                        NewsViewModel (AndroidViewModel)                 │
│   • refreshAll(force) / refreshCategory(category)  → loading & error    │
│   • toggleSaved / insertSaved / deleteSaved        → bookmark state     │
│   • search(query) with 400 ms debounce                                  │
│   • exposes: observeCategory, categoryStatus, savedNews, savedUrls,     │
│     searchState, refreshing, message (one-shot Event)                   │
└───────▲──────────────────────────┬──────────────────┬───────────────────┘
        │ suspend calls            │ transactions     │ LiveData
┌───────┴──────────┐   ┌───────────┴─────────┐  ┌─────┴──────────────┐
│  NewsRepository  │   │      NewsDao        │  │  NewsDatabase      │
│  (Retrofit/      │   │  (Room queries)     │  │  (Room, table      │
│   OkHttp/Gson)   │   │                     │  │   "articles")      │
└───────┬──────────┘   └─────────────────────┘  └────────────────────┘
        │ HTTPS
        ▼
   NewsAPI.org  (/v2/top-headlines, /v2/everything)
```

## Data flow

1. **Launch** – `MainActivity` calls `NewsViewModel.refreshAll(force = false)`.
   If the 30-minute cache (see `CachePrefs`) is still fresh, nothing is
   fetched and the UI renders instantly from Room.
2. **Fetch** – `NewsRepository` calls NewsAPI through Retrofit *suspend*
   functions. The JSON is mapped to `NewsModel` rows; articles with a blank
   title or missing URL are dropped, order is preserved via `sortIndex`.
3. **Store** – The ViewModel replaces the category's rows inside a Room
   transaction (`clearCategory` + `insertAll`).
4. **Render** – Each tab observes `dao.observeCategory(category)` directly, so
   every write is pushed to the UI automatically — including after rotation,
   process restore, or while the user is offline.
5. **Failure** – Errors are mapped to friendly messages (`CategoryUiState.error`,
   parsed from NewsAPI's JSON error envelope when available). Tabs keep
   showing their cached data and offer a **Retry** button when empty.

## Why this design

| Decision | Rationale |
| --- | --- |
| Room as single source of truth | Offline support, instant rotation, one code path for fresh & cached data |
| One `NewsListFragment` for all tabs | The original code had 7 near-identical fragments; a single parameterized fragment removes ~400 duplicated lines |
| `ListAdapter` + `DiffUtil` | Animated, efficient updates instead of full `notifyDataSetChanged` |
| `Event` wrapper for messages | Snackbar/Toast messages survive re-observation without re-showing |
| Cache TTL (30 min) in SharedPreferences | Keeps a free NewsAPI key far below its daily request limit; explicit pull-to-refresh bypasses it |
| Injected base URL in `RetrofitHelper` | The repository is unit-testable against `MockWebServer` |
| Core library desugaring | `java.time` parsing/formatting on all supported API levels (minSdk 21) |

## Concurrency model

* UI state is mutated **only on the main thread** (`viewModelScope` uses
  `Dispatchers.Main.immediate`).
* Network calls run on `Dispatchers.IO` inside the repository.
* Room transactions are suspend functions executed by Room's own IO executor.
* In-flight refreshes are de-duplicated per category via the `refreshJobs` map.

## Key classes

| Class | Responsibility |
| --- | --- |
| `NewsViewModel` | Orchestrates fetching, caching, bookmarks, search; exposes UI state |
| `NewsRepository` | NewsAPI calls + JSON→entity mapping + error translation |
| `NewsDao` / `NewsDatabase` | Persistence (category cache + saved list) |
| `NewsListAdapter` | DiffUtil-backed list rendering with bookmark toggle |
| `CarouselAdapter` | Top-headlines pager on the Home tab |
| `TimeUtils` | Pure-JVM ISO-8601 parsing and human-readable labels (unit tested) |
| `CachePrefs` | 30-minute fetch TTL |
| `Event` | One-shot message wrapper |
