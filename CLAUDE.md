# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kids Weather Outfit — native Android app that shows parents how to dress a child for the weather. Home screen shows a pre-rendered WebP illustration of a dressed child (12 outfit sets × boy/girl) plus a horizontal strip of outfit changes for 7:00–19:00. Weather comes from Open-Meteo (free, no API key), hourly, today + tomorrow.

## Commands

```bash
./gradlew assembleDebug                # build debug APK
./gradlew :domain:test                 # domain unit tests (fast, pure JVM)
./gradlew :app:testDebugUnitTest       # app unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (needs device/emulator)

# Single test class
./gradlew :domain:test --tests "com.rustanovych.kidsoutfit.domain.model.ColdSensitivityTest"
```

The Gradle configuration cache is enabled (`gradle.properties`).

## Toolchain constraints (important)

- AGP 9.3 uses the **built-in Kotlin 2.2 compiler** — there is no separate `kotlin-android` plugin. The catalog's `kotlin = 2.2.10` version only feeds the compose/jvm/serialization plugins.
- The built-in compiler reads Kotlin metadata ≤ 2.3.0. Libraries built with Kotlin 2.4 fail with "incompatible metadata" errors. This is why the version catalog pins **Koin 4.1.1** (not 4.2.x) and **Coil 3.4.0** (not 3.5.0) — do not bump them until AGP's embedded Kotlin reaches 2.4. See the comments in `gradle/libs.versions.toml`.
- Navigation 3 (1.1.4): `entry<T>` is a member of `EntryProviderBuilder` — do **not** import `androidx.navigation3.runtime.entry` (unresolved reference).

## Architecture

Two modules:

- `:app` — Android application (Compose, Material 3). Package `com.rustanovych.kidsoutfit`. minSdk 26, targetSdk 36, compileSdk 37.1.
- `:domain` — **pure Kotlin JVM** module (`com.rustanovych.kidsoutfit.domain`). Stdlib + JUnit only; no Android or third-party dependencies allowed, so it can move to KMP later. Business models like `ColdSensitivity` and `ChildGender` live here.

Stack decisions (deliberate, don't substitute):

- **DI: Koin** (not Hilt). `KidsOutfitApp` calls `startKoin`; everything registers in the root `appModule` (`app/.../di/AppModule.kt`).
- **Navigation: Navigation 3** (not Nav 2.x). `NavKey` implementations are `@Serializable` objects/classes in `ui/navigation/AppNavKeys.kt`; `MainActivity` owns the back stack via `rememberNavBackStack` + `NavDisplay`.
- Networking: Retrofit + kotlinx.serialization. Storage: DataStore Preferences. Images: Coil 3. Background: WorkManager; location: Play Services Location.

Single-activity app: `MainActivity` hosts all screens; screens live under `ui/<feature>/` (e.g. `ui/home/HomeScreen.kt`).
