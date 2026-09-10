# Contributing

Looking to report an issue/bug or make a feature request? Please open a
[GitHub issue](https://github.com/Raj-m01/News-App/issues) with a clear
description and, if possible, steps to reproduce.

Thanks for your interest in contributing!

## Prerequisites

Working on this project requires:

- [Basic Android development](https://developer.android.com/) knowledge
- [Kotlin](https://kotlinlang.org/)
- [Android Studio](https://developer.android.com/studio) (Hedgehog or newer)
- JDK 17 (bundled with recent Android Studio versions)
- An emulator or device to test on

## Getting started

1. **Fork** the repository (top-right **Fork** button).
2. **Clone your fork**

   ```bash
   git clone https://github.com/<your-username>/News-App.git
   cd News-App
   ```

3. **Add your NewsAPI key** (see [`docs/SETUP.md`](docs/SETUP.md) for details):

   ```bash
   echo 'NEWS_API_KEY=your_real_key_here' >> ~/.gradle/gradle.properties
   ```

4. **Open the project** in Android Studio and let Gradle sync, or build from
   the command line:

   ```bash
   ./gradlew :app:assembleDebug :app:testDebugUnitTest
   ```

## Making changes

1. Create a feature branch:

   ```bash
   git checkout -b feature/my-feature
   ```

2. Keep the codebase healthy:
   - Follow the existing Kotlin style (`kotlin.code.style=official`).
   - New UI strings go into `res/values/strings.xml` (never hard-code text).
   - New date logic belongs in `TimeUtils` with a unit test.
   - Run `scripts/check-resources.sh` if you add/remove resources.
   - Make sure `./gradlew :app:testDebugUnitTest` passes.

3. Commit with a clear message and push to your fork:

   ```bash
   git add .
   git commit -m "Add my feature"
   git push origin feature/my-feature
   ```

4. **Open a pull request** against `main` with a descriptive title, what
   changed and why, and screenshots/screen recordings for UI changes.

## Code of conduct

Be respectful and constructive. Maintainers may report behaviour that is not
in line with the community standards, and spam pull requests will be closed
without merge.
