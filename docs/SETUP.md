# Setup & build guide

## Prerequisites

| Tool | Version |
| --- | --- |
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 (bundled with recent Android Studio) |
| Android SDK | Platform 34, Build-Tools 34.0.0 |

## 1. Get a NewsAPI key

The app fetches data from [NewsAPI.org](https://newsapi.org/). The free
developer plan allows ~100 requests/day which is plenty for personal use
(the app throttles itself to a refresh every 30 minutes).

1. Register at <https://newsapi.org/register>.
2. Copy your API key.

## 2. Configure the key

**Recommended — keep the key out of version control.** Add it to your
*user-level* Gradle properties file (create it if it doesn't exist):

```bash
# macOS / Linux
echo 'NEWS_API_KEY=your_real_key_here' >> ~/.gradle/gradle.properties
```

```bat
:: Windows (Git Bash / PowerShell)
echo NEWS_API_KEY=your_real_key_here >> "%USERPROFILE%\.gradle\gradle.properties"
```

Alternatively edit `gradle.properties` in the project root and replace the
placeholder:

```properties
NEWS_API_KEY=YOUR_NEWSAPI_KEY_HERE
```

> The project ships with the placeholder value so it always *builds* —
> with the placeholder in place the app runs but every request returns an
> API error, which the UI displays with a retry button.

CI note: the included GitHub Actions workflow reads an optional
`NEWS_API_KEY` repository secret.

## 3. Build & run

From Android Studio: **File → Open**, select the repo root, let Gradle sync,
then press **Run**.

From the command line:

```bash
./gradlew :app:assembleDebug     # debug APK → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest # JVM unit tests
./gradlew :app:lint              # Android lint
```

Requirements: JDK 17. Gradle 8.4 and all dependencies are downloaded
automatically on first sync/build.

## 4. Release build

```bash
./gradlew :app:assembleRelease
```

R8 minification and resource shrinking are enabled. Keep rules for the
Gson models live in `app/proguard-rules.pro`. Sign the release the usual way
(`signingConfig`) before publishing.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Unsupported class file major version` | You are using an old JDK — switch to JDK 17 (Studio: *Settings → Build Tools → Gradle → Gradle JDK*). |
| Every category shows an API error | `NEWS_API_KEY` is still the placeholder, or you exhausted the daily quota — the exact NewsAPI message is displayed on the error screen. |
| `SDK location not found` | Create `local.properties` with `sdk.dir=/path/to/android-sdk` or open the project in Android Studio once. |
