# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A faithful Android port of the classic 2004 J2ME game **Gravity Defied** (a motorcycle trial racing game), packaged as `org.happysanta.gd` (module name `AGDTR`). It also adds an online mod browser that downloads fan-made level packs from the gdtr.net backend.

## Build system

This is a **legacy pre-Gradle Android project**. There is no `build.gradle`, no Gradle wrapper, and no `build.xml`. It is built by an IDE using the old Android SDK tooling:

- IntelliJ IDEA legacy Android plugin (`.idea/`, `AGDTR.iml`), or Eclipse ADT (`.project`, `.classpath`).
- Compile target **Android API 19**; `minSdkVersion=8`, `targetSdkVersion=19` (`AndroidManifest.xml`, `project.properties`).
- Language level is Java 6/7 era — no lambdas, no streams, anonymous inner classes everywhere. Match this style.
- Dependencies are plain jars / project libraries, not Maven coordinates: ACRA 4.5.0 (`libs/acra-4.5.0.jar`, crash reporting) and `android-support-v4`. Networking uses the legacy `org.apache.http` client that shipped with old Android.

There are **no tests, no lint config, and no CI**. Do not add a Gradle build or test harness unless explicitly asked — it would not match how this project is built.

## Architecture

### Single-activity, dedicated game thread
`GDActivity` is the only Activity (`singleInstance`, portrait, fullscreen). It implements `Runnable`: `onCreate` builds the view tree, then `doStart()` spawns `game_thread` and the entire game loop lives in `GDActivity.run()`. The loop drives the physics engine, detects crash/finish states, and swaps between gameplay and menu via `gameToMenu()` / `menuToGame()`. App "restart" is done by scheduling a fresh launch through `AlarmManager` (`doRestartApp()`).

Threading rule the codebase follows consistently: game logic runs on `game_thread`; any UI mutation is wrapped in `runOnUiThread()` and marked with a `// @UiThread` comment. Preserve this when editing.

### Global singleton access
`GDActivity.shared` is a public static set in `onCreate`. Almost everything reaches shared state through `Helpers.getGDActivity()` and from there `menu`, `physEngine`, `levelLoader`, `levelsManager`, `gameView`. `Global` holds compile-time flags (`DEBUG`, `ACRA_ENABLED`) and shared state (screen `density`, the Roboto Condensed typeface).

### Package map
- `Game/` — the physics and rendering core. `Physics` is a **fixed-point math** trial-bike simulator (`FPMath` = fixed-point helpers); `GameView` is a custom `View` rendering frames with `Canvas`. This package is **decompiled from the original J2ME jar** (Jad). See the obfuscation note below.
- `Levels/` — binary track loading. `Loader` parses `.mrg` level-pack files; `Reader`/`Level`/`LevelHeader` decode the format. The bundled original packs are `assets/levels.mrg`; downloaded mods are `.mrg` files fetched from `http://gdtr.net/mrg/<id>.mrg`. `Levels/Level` is an in-memory parsed track — **not** the same type as `Storage/Level`.
- `Storage/` — SQLite persistence. `LevelsSQLiteOpenHelper` defines `levels.db` (tables `levels`, `highscores`; schema version 1, no migrations in `onUpgrade`). `LevelsDataSource` is the DAO; `LevelsManager` is the high-level controller for installing/switching mods and computing stats; `HighScores` and `Storage/Level` are entities.
- `Menu/` — a custom menu framework rendered as real Android Views inside `GDActivity`'s `ScrollView` (the original J2ME canvas menu was replaced; `MenuElementOld`/`SimpleMenuElement` are leftover remnants). `Menu` is the controller, `MenuScreen` a screen, `MenuElement` subclasses are rows, `Menu/Views/` holds custom `View` subclasses. The mod browser/manager screens are `LevelsMenuScreen`, `InstalledLevelsMenuScreen`, `DownloadLevelsMenuScreen`.
- `API/` — gdtr.net backend client. Endpoint `http://gdtr.net/api.php` (API `VERSION = 2`), requests run on `AsyncTask` via Apache `HttpClient`. Calls: `getLevels`, `getNotifications` (shown as an `AlertDialog` on launch), `sendStats`, `sendKeyboardLogs`, `downloadMrg`. Callback style: `Request` + `ResponseHandler` with typed `LevelsResponse`/`NotificationsResponse`.
- `Helpers.java` — static utilities, including a **Windows-1251 (CP1251) translation table**: level and menu strings use CP1251 (Russian text), not UTF-8. `Settings.java` wraps `SharedPreferences`. `KeyboardController` drives the on-screen 3×3 numeric control pad. `GDApplication` initializes ACRA (crash reports POST to `http://gdtr.net/report.php`).

## Working in this codebase

- **Do not rename or "modernize" the cryptic identifiers** (`m_longI`, `_avJ()`, `_dovI()`, single-letter fields, class `k`) in `Game/`, `Levels/Loader`, and parts of `Menu/`. They are machine-decompiled from the original J2ME game and the goal is behavior parity with the 2004 original. Treat this like generated code.
- **Leave the large blocks of commented-out code alone** unless the task is specifically about them. They are deliberate artifacts of the J2ME→Android porting process and serve as reference.
- When changing gameplay/physics behavior, remember the reference is the original game's feel — prefer minimal, surgical changes over refactors.
- All user-visible strings are in `res/values*/`. Resource qualifiers in use include tablet (`values-sw600dp`, `values-sw720dp-land`) and API-level (`values-v11`, `values-v14`) variants.
