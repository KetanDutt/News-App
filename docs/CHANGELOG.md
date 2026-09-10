# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project adheres to [Semantic Versioning](https://semver.org/).

## [2.0.0] — 2026-09-10

Full overhaul of the app: architecture, UI, toolchain and documentation.

### Added
- **Offline support** — fetched articles are cached in Room per category; the
  app renders instantly from cache on cold start, rotation and while offline.
- **Search** — new full-text search screen (NewsAPI *everything* endpoint) with
  a 400 ms debounce, shimmer loading, no-result and error states.
- **Bookmark from any list** — every story row has a bookmark button that
  saves/removes the article without leaving the list (icon syncs live).
- **Pull-to-refresh** on the home screen, bypassing the 30-minute cache TTL.
- **Retry buttons** on every error state; friendly error messages parsed from
  the NewsAPI error envelope (quota, invalid key, offline, …).
- **Empty states** for the saved list and search results.
- **Undo** — deleting a saved story (swipe) can be undone from the snackbar.
- **Swipe-to-delete** on saved stories (replaces the long-press dialog and its
  row-tinting hack).
- **WebView progress bar** and in-WebView back navigation; hardware back now
  walks WebView history before leaving the screen.
- **Unit tests** for date formatting (`TimeUtilsTest`) and API
  parsing/error handling (`NewsRepositoryTest` with MockWebServer).
- **CI** — GitHub Actions workflow building the APK and running unit tests.
- **Documentation** — `docs/` folder: architecture, setup, project structure,
  improvement audit and this changelog.

### Changed
- **Architecture** — Room is now the single source of truth; global static
  state (`MainActivity.generalNews`, `apiRequestError`, …) was removed
  entirely. 7 near-identical category fragments collapsed into one
  `NewsListFragment`; 6 duplicate layouts collapsed into one.
- **Modernized toolchain** — Gradle 7.4 → 8.4, AGP 7.2.2 → 8.3.2, Kotlin
  1.7.10 → 1.9.24, compileSdk/targetSdk 33 → 34, version catalog
  (`gradle/libs.versions.toml`), kapt → KSP.
- **UI refresh** — new Material theme (proper light *and* dark palettes),
  redesigned story cards (title + source • time + bookmark), ViewPager2-based
  top-headlines carousel with dot indicators, themed shimmer skeletons.
- **Relative timestamps** — correct, locale-safe "5m/3h/2d ago" labels via
  `TimeUtils` (replaces the broken negative-hours substring hack); saved list
  shows absolute dates.
- **Text-to-speech** — uses the engine's default language/voice (removing fake
  voice descriptors that crashed), added 1.5x speed, radio-checked speed
  group, guards against speaking before the engine is ready, and strips
  NewsAPI's `[+1234 chars]` truncation markers before speaking.
- **Image loading** — Picasso → Coil (Kotlin-first, lifecycle-aware, smaller).
- **API key handling** — no longer hard-coded; read from `NEWS_API_KEY` in
  `gradle.properties` / `~/.gradle/gradle.properties` / CI secret.
- Swipe-to-save feedback switched to Material snackbars with consistent strings.

### Fixed
- **Duplicated articles on rotation** — the old code re-`addAll`-ed into static
  lists on every configuration change.
- **Crash on small/empty feeds** — `slice(5 until size - 5)` threw
  `IllegalArgumentException` when a category returned < 10 articles.
- **Crash on malformed timestamps** — `Instant.parse(null)` /
  `substring(0, indexOf('T'))` NPEs when the API omitted or malformed
  `publishedAt`.
- **Wrong "time ago" values** — the old code computed *news time minus now*
  and stripped the minus sign; days were shown as nonsensical hours.
- **Build-breaking Firebase config** — the committed `google-services.json`
  belonged to a different app/package, so the Google Services plugin failed
  the build. Firebase (unused, copy-pasted) was removed.
- **TTS voice crash** — setting hard-coded `Voice` objects unsupported by the
  engine threw `IllegalArgumentException`.
- **Web security** — WebView `MIXED_CONTENT_ALWAYS_ALLOW` →
  `MIXED_CONTENT_COMPATIBILITY_MODE`; file/content access disabled.
- **jcenter dependency** — the dead jcenter/bintray + JitPack carousel library
  was replaced with a ViewPager2 carousel; repositories reduced to
  Google + Maven Central.
- **Non-nullable Gson models** — API models are now nullable-safe against
  JSON missing fields (Gson bypasses Kotlin constructors).
- **Deprecated APIs** — `Handler()`, `activeNetworkInfo`, and others replaced
  with current equivalents.
- **Wrong database name** (`LOGIN_DATABASE` — copy-paste) → `news_database`.
- **Saved-rows duplicate headlines** — primary key switched from headline to
  an auto-generated id.

### Removed
- Committed APKs (`apk/`, `app/debug/`, ~14 MB), `.idea/` folder and the
  foreign `google-services.json`.
- Firebase Performance (plugins + dependency) — was never meaningfully used.
- The third-party `CarouselView` (JitPack) and Picasso dependencies.
- ~450 lines of copy-pasted fragment/adapter code and commented-out dead XML.
- The "get paid version to hear full news" TTS suffix.

## [1.0.0] — 2022

Initial public release: category tabs, bookmarking, dark mode, sharing,
text-to-speech, shimmer loading.
