# Improvement audit

This document records the full audit performed on the 1.x codebase: bugs
found, performance issues, security concerns, and what was done about each —
plus a roadmap of ideas that did *not* fit into the 2.0 scope.

## 1. Bugs found & fixed

| # | Severity | Issue | Fix |
| --- | --- | --- | --- |
| 1 | 🔴 Critical | **Data duplication on rotation** — the seven category lists lived in `MainActivity`'s `companion object` and `addAll` was called on every re-observation, so rotating the device doubled every list. | All static state removed; Room + LiveData is the single source of truth; rotation re-renders without refetching. |
| 2 | 🔴 Critical | **Crash on small/empty feeds** — `slice(5 until size - 5)` throws `IllegalArgumentException` whenever a category returned fewer than 10 articles (e.g. API error, empty cache, offline). | Carousel/list split guards on `size > TOP_HEADLINES_COUNT`. |
| 3 | 🔴 Critical | **Build was broken by Firebase** — the committed `google-services.json` belonged to a different app (`com.rtctek.cpuinfo.debug`), so the Google Services plugin failed for `com.rtctek.newsapp`. | Firebase (unused) removed together with its plugins and the foreign config file. |
| 4 | 🔴 Critical | **TTS voice crash** — hard-coded `Voice("en-US-SMTf00", …)` descriptors were assigned to `tts.voice`; engines that don't know the voice throw `IllegalArgumentException`. | Fake voices removed; the engine's default language/voice is used. |
| 5 | 🟠 High | **Time-ago labels were wrong** — the code computed `news time − now` (negative) and stripped the leading `-` via `substring(1)`, producing nonsense for anything ≥ 1 day; timezone was hard-coded to `Asia/Kolkata`. | `TimeUtils.relativeTime` computes the correct elapsed duration with system-default UTC handling and compact `5m/3h/2d` labels (unit tested). |
| 6 | 🟠 High | **NPE on malformed timestamps** — `Instant.parse(time)` with null time, and `substring(0, time.indexOf('T'))` with `-1`, crashed the list. | All parsing is null-safe with layered fallbacks; rows without a parsable date simply omit the label. |
| 7 | 🟠 High | **NPE via intent extras** — `intent.getStringExtra(NEWS_TITLE)!!` crashed if the activity was restored without extras. | `getNewsArticle()` returns null → friendly toast + `finish()`. |
| 8 | 🟠 High | **`"null"` UI values** — Gson models declared non-nullable fields, so missing JSON keys produced `null` values masquerading as non-null, crashing later. | All API models are nullable with defaults; blank/invalid articles are filtered before persisting. |
| 9 | 🟠 High | **API key hard-coded in source control**. | Key now comes from `NEWS_API_KEY` in `gradle.properties` / `~/.gradle/gradle.properties` / CI secret; placeholder default keeps the build green. |
| 10 | 🟠 High | **"hours ago" hack to detect the saved screen** — `context.toString().contains("SavedNews")` decided the date format. | Explicit `showRelativeTime` adapter parameter. |
| 11 | 🟡 Medium | **Bookmark inserts used the headline as primary key** — two different articles with the same headline silently replaced each other. | Auto-generated `id` primary key; saved check by URL. |
| 12 | 🟡 Medium | **Saved-item deletion left a highlighted row** — long-press painted the row with `colorPrimaryVariant` and only reset it on "No". | Replaced with swipe-to-delete + Undo snackbar. |
| 13 | 🟡 Medium | **Error state was sticky** — static `apiRequestError` never reset; the app stayed broken until process death. | Per-category `CategoryUiState` recomputed on every refresh. |
| 14 | 🟡 Medium | **Unbounded coroutine scope** — repository launched work on a leaked `CoroutineScope(IO)`. | All work bound to `viewModelScope`/`suspend`. |
| 15 | 🟡 Medium | **Wrong database name** `LOGIN_DATABASE` (copy-paste) → `news_database`; broken double-checked locking cleaned up. |
| 16 | 🟡 Medium | **Back button ignored WebView history** — pressing back exited the reader instantly. | `OnBackPressedDispatcher` walks WebView history first. |
| 17 | 🟡 Medium | **Confusing base URL/path** — `@GET("/v2/top-headlines")` against a `…/v2/` base URL relied on Retrofit's leading-slash replacement. | Relative paths; the final URL is unchanged. |
| 18 | 🟢 Low | Deprecated `Handler()` constructor, `activeNetworkInfo`, missing VPN transport in connectivity check. | Modern APIs. |
| 19 | 🟢 Low | The 5-second artificial loading delay (comment even said 2 s) before any request fired. | Removed entirely — fragments render instantly from cache. |
| 20 | 🟢 Low | Lowercase tab labels ("business", "technology") straight from API values. | Proper `TabCategory(labelRes, query)` config. |
| 21 | 🟢 Low | TTS appended “get paid version to hear full news.” and read the raw `[+1234 chars]` markers. | Cleaned speech text; that string removed. |
| 22 | 🟢 Low | Double `v2` risk + Firebase perf plugin logging in release, `enableJetifier` overhead, `multiDexEnabled` on minSdk 21 (no-op). | All cleaned. |

## 2. Performance improvements

- **No full-list rebinds** — `ListAdapter` + `DiffUtil` replace
  `notifyDataSetChanged()`; only changed rows re-render (with animations).
- **Instant cold start / rotation** — UI reads from Room; the network refresh
  only happens when the 30-minute cache expires (or on pull-to-refresh).
- **Request budget** — worst case drops from *7 requests on every open/rotate*
  to *7 requests per 30 minutes* — critical on the free NewsAPI tier.
- **KSP instead of kapt** for Room — faster builds, no stub generation.
- **Jetifier removed** (no more support-library artifacts) — faster builds.
- **Coil replaces Picasso** — Kotlin-first, memory-cache friendly, automatic
  lifecycle-aware request cancellation while scrolling.
- **`unstable` diffs are cheap** — bookmark toggles rebind only the affected
  rows (`setSavedUrls` diffs the previous set).
- **Default `offscreenPageLimit`** — tabs are created lazily instead of the
  previous eager `offscreenPageLimit = 7`.

## 3. Security review

| Concern | Status |
| --- | --- |
| API key in VCS | ✅ Fixed — moved to local/user Gradle properties (documented in `SETUP.md`). |
| WebView mixed content | ✅ `MIXED_CONTENT_COMPATIBILITY_MODE` (was `ALWAYS_ALLOW`). |
| WebView file/content access | ✅ Disabled. |
| Cleartext traffic | ✅ Not enabled anywhere (default). |
| TTS intent queries | ✅ Scoped `<queries>` entry (already present, kept). |
| Backup of cached data | `allowBackup` kept — only public news data is cached. |

> Note: any key compiled into an APK is technically extractable. For a fully
> hardened setup, proxy NewsAPI through your own backend and ship no key —
> see the roadmap below.

## 4. Suggested improvements (roadmap)

Ideas that are intentionally **not** implemented in 2.0 to keep the change
reviewable:

1. **Jetpack Compose migration** — the XML UI is still maintained; Compose
   would remove the remaining layout duplication but deserves its own change.
2. **Paging 3** — infinite scroll for categories/search instead of the fixed
   first page (search already accepts a `page` parameter server-side).
3. **Key-proxied backend** — a thin server (or Cloud Function) that injects
   the NewsAPI key server-side so the APK contains no key at all.
4. **Hilt** — dependency injection once the app grows beyond one repository.
5. **Bookmarks UI polish** — folders/tags, export/import (JSON), search
   within saved items.
6. **DataStore** — replace the SharedPreferences TTL with Preferences
   DataStore for coroutine-friendly access.
7. **Baseline profiles** — further cold-start wins.
8. **Instrumented UI tests** — Espresso flows for save/search/delete.
9. **Localization** — all user-visible strings are externalized; translations
   (e.g. Hindi) can now be added purely via `values-xx/`.
10. **Country/language selection** — the API layer supports any `country`
    parameter; a settings screen could expose it.
