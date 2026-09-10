# Project structure

```
News-App/
├── app/
│   ├── build.gradle                    # module config (SDK 34, key injection, deps)
│   ├── proguard-rules.pro              # R8 keep rules for Gson/Retrofit models
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/rtctek/newsapp/
│       │   │   ├── MainActivity.kt             # home: tabs + ViewPager2 + refresh
│       │   │   ├── ReadNewsActivity.kt         # WebView reader + TTS + share/save
│       │   │   ├── SavedNewsActivity.kt        # bookmarks list + swipe-delete
│       │   │   ├── SearchActivity.kt           # full-text search screen
│       │   │   ├── NewsModel.kt                # Room entity + Intent extras helpers
│       │   │   ├── adapters/
│       │   │   │   ├── CarouselAdapter.kt      # top-headlines pager
│       │   │   │   ├── FragmentAdapter.kt      # ViewPager2 tabs adapter
│       │   │   │   └── NewsListAdapter.kt      # DiffUtil story list + bookmark
│       │   │   ├── architecture/
│       │   │   │   ├── NewsDao.kt              # Room queries
│       │   │   │   ├── NewsDatabase.kt         # Room database singleton
│       │   │   │   ├── NewsRepository.kt       # NewsAPI calls + mapping + errors
│       │   │   │   ├── NewsViewModel.kt        # UI state orchestrator
│       │   │   │   └── UiState.kt              # CategoryUiState / SearchUiState
│       │   │   ├── fragments/
│       │   │   │   └── NewsListFragment.kt     # one reusable per-tab fragment
│       │   │   ├── retrofit/
│       │   │   │   ├── Article.kt / Source.kt / NewsDataFromJson.kt
│       │   │   │   ├── NewsApi.kt              # Retrofit interface (suspend)
│       │   │   │   └── RetrofitHelper.kt       # OkHttp/Retrofit factory
│       │   │   └── utils/
│       │   │       ├── CachePrefs.kt           # 30-min fetch TTL
│       │   │       ├── Constants.kt            # categories, extras, DB name
│       │   │       ├── Event.kt                # one-shot LiveData event
│       │   │       ├── NetworkUtils.kt         # connectivity check
│       │   │       └── TimeUtils.kt            # ISO-8601 → “5h ago” (unit tested)
│       │   └── res/
│       │       ├── layout/                     # 11 layouts (was 15, deduplicated)
│       │       ├── menu/                       # menu_main, menu_read_news, menu_search
│       │       ├── drawable/                   # vectors incl. new search/bookmark/dots
│       │       ├── values/ + values-night/     # colors, strings, themes
│       │       └── mipmap-*/                   # launcher icons
│       └── test/java/com/rtctek/newsapp/
│           ├── TimeUtilsTest.kt                # date formatting tests
│           └── NewsRepositoryTest.kt           # API parsing tests (MockWebServer)
├── docs/
│   ├── ARCHITECTURE.md                # how the app is wired together
│   ├── SETUP.md                       # API key + build instructions
│   ├── IMPROVEMENTS.md                # audit: bugs fixed, perf, security, roadmap
│   ├── CHANGELOG.md                   # release notes
│   └── PROJECT_STRUCTURE.md           # this file
├── scripts/
│   └── check-resources.sh             # static resource cross-reference checker
├── gradle/
│   ├── libs.versions.toml             # version catalog
│   └── wrapper/                       # Gradle 8.4 wrapper
├── screenshots/                       # app screenshots (used by README)
├── .github/workflows/android-ci.yml   # CI: build + unit tests
├── CONTRIBUTING.md
├── LICENSE                             # MIT
└── README.md
```

## Naming conventions

* **Packages**: `adapters`, `architecture` (Room + ViewModel), `fragments`,
  `retrofit` (network models), `utils` — kept from the original project so
  history stays reviewable.
* **Resources**: `snake_case`; shared state views use `view_*.xml` includes
  (`view_error_state`, `view_empty_state`); list rows `list_item.xml` /
  `item_top_headline.xml`.
* **Database**: single `articles` table; `category = 'saved'` marks bookmarks.
