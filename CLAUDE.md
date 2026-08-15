# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A faithful Android port of the classic 2004 J2ME game **Gravity Defied** (a motorcycle trial racing game), packaged as `cl.figonzal.gravitydefied`. It also adds an online mod browser that downloads fan-made level packs from the gdtr.net backend.

## Build system

This is a **Gradle-based Android project** (AGP 9.1.1, Gradle 9.3.1).

### Build commands

```bash
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease        # release APK (requires keys/keystore.properties)
./gradlew bundleRelease          # release AAB for Play Store
./gradlew clean                  # also the first step of every fastlane build lane
./gradlew dependencyUpdates      # ben-manes report on outdated deps
./gradlew versionCatalogUpdate   # rewrite gradle/libs.versions.toml to latest
```

### Deployment (Fastlane)

```bash
fastlane internal                # build release AAB
fastlane internal_googleplay     # upload AAB to Play Store internal track (draft)
fastlane beta                    # build release AAB
fastlane beta_googleplay         # upload AAB to Play Store beta track (draft)
fastlane prod                    # build release AAB
fastlane prod_googleplay         # upload AAB to Play Store production track (draft)
```

All six lanes upload as **draft**; the `*_googleplay` lanes only upload — they do not build, so run the matching build lane first.

Signing comes entirely from `keys/keystore.properties` (gitignored), read by the `release`
`signingConfig` in `app/build.gradle.kts`. Fastlane deliberately does **not** inject
`android.injected.signing.*` — a second copy of the keystore path drifted per-machine once already,
and `-P` properties leak the passwords into the build log. The only property fastlane passes is
`uploadMapping`.

### Dependencies and config

- `app/build.gradle.kts`: `compileSdk 36`, `minSdk 23`, `targetSdk 36`.
- Language level Java 8 (`compileOptions JavaVersion.VERSION_1_8`), but the codebase style is Java 6/7 era — anonymous inner classes everywhere, no lambdas, no streams. Match this style in new code.
- **AndroidX: in the build, not in our source.** Our own Java under `app/src/main/java/` deliberately uses framework classes only — `android.app.Activity`, `android.widget.*`, no `androidx.*` imports — and new code should match that style for parity with the J2ME-era port. The **built APK does contain AndroidX**, however: `gradle.properties` sets `android.useAndroidX=true`, and `firebase-analytics` transitively pulls `androidx.fragment`, `androidx.activity`, `androidx.core`, etc. A couple are pinned explicitly in `gradle/libs.versions.toml` (`androidx-fragment`, `androidx-activity`) to override outdated transitive versions Play Console flagged — that's a build-time override, not a license to start importing AndroidX from app code. If a platform/Play Console issue genuinely requires an AndroidX API to fix (e.g. `WindowInsetsControllerCompat` for the immersive-mode migration on API 23–29), reach for it deliberately and call it out in the commit.
- `gradle.properties` also sets `android.r8.strictFullModeForKeepRules=false` — R8 full-mode keep-rule strictness is relaxed on purpose; don't flip it without re-testing a release build.
- **Release builds are minified**: `isMinifyEnabled = true` with `proguard-android-optimize.txt` + `app/proguard-rules.pro`, which exists mainly to `-keep class …Game.**` and `…Levels.**` so the decompiled identifiers survive R8. The "don't rename cryptic identifiers" rule (below) has this build-level counterpart — anything reflective added to those packages needs a matching keep rule. (The ACRA and Apache-HTTP rules in that file are dead leftovers from earlier dependencies; harmless, leave them.)
- **Debug installs alongside release**: `applicationIdSuffix = ".dev"`, `versionNameSuffix = "-debug"`, and `app_name` is supplied per-build-type via `resValue` in `app/build.gradle.kts` — it is *not* in `strings.xml`.
- Density splits are deliberately disabled (`bundle { density { enableSplit = false } }` in `app/build.gradle.kts`) to avoid `Resources$NotFoundException` on sideloaded/partial-split installs — see the comment there.
- Firebase: Crashlytics + Analytics, both declared in `app/build.gradle.kts`. They auto-initialize from `google-services.json` — `GDApplication.onCreate()` is intentionally empty.
- **`google-services.json` must be present in `app/`** for Firebase to work (gitignored, not in repo).
- Theme: `android:Theme.Material.Light.NoActionBar` (API 21 built-in, no Material Components library).

There are **no tests, no lint config, and no CI**.

## Architecture

### Single-activity, dedicated game thread
`GDActivity` is the only Activity (`singleInstance`, portrait, fullscreen). It implements `Runnable`: `onCreate` builds the view tree, then `doStart()` spawns `game_thread` and the entire game loop lives in `GDActivity.run()`. The loop drives the physics engine, detects crash/finish states, and swaps between gameplay and menu via `gameToMenu()` / `menuToGame()`. App "restart" is done by scheduling a fresh launch through `AlarmManager` (`doRestartApp()`).

Threading rule the codebase follows consistently: game logic runs on `game_thread`; any UI mutation is wrapped in `runOnUiThread()` and marked with a `// @UiThread` comment. Preserve this when editing. Because `game_thread` (and API callback threads) can outlive activity teardown, UI work posted from them should check `Helpers.isActivityAlive()` first, and dialogs should be closed via `Helpers.dismissDialog()` (null- and exception-safe) rather than calling `dismiss()` directly — this guard convention was introduced to fix crashes from dismissing/showing dialogs on a dead or finishing activity.

### Global singleton access
`GDActivity.shared` is a public static set in `onCreate`. Almost everything reaches shared state through `Helpers.getGDActivity()` and from there `menu`, `physEngine`, `levelLoader`, `levelsManager`, `gameView`.

### Package map
- `Game/` — the physics and rendering core. `Physics` is a **fixed-point math** trial-bike simulator (`FPMath` = fixed-point helpers); `GameView` is a custom `View` rendering frames with `Canvas`. This package is **decompiled from the original J2ME jar** (Jad). See the obfuscation note below.
- `Levels/` — binary track loading. `Loader` parses `.mrg` level-pack files; `Reader`/`Level`/`LevelHeader` decode the format. The bundled original packs are `assets/levels.mrg`; downloaded mods are `.mrg` files fetched from `http://gdtr.net/mrg/<id>.mrg`. `Levels/Level` is an in-memory parsed track — **not** the same type as `Storage/Level`.
- `Storage/` — SQLite persistence. `LevelsSQLiteOpenHelper` defines `levels.db` (tables `levels`, `highscores`; schema version 1, no migrations in `onUpgrade`). `LevelsDataSource` is the DAO; `LevelsManager` is the high-level controller for installing/switching mods and computing stats; `HighScores` and `Storage/Level` are entities.
- `Menu/` — a custom menu framework rendered as real Android Views inside `GDActivity`'s `ScrollView` (the original J2ME canvas menu was replaced; `MenuElementOld`/`SimpleMenuElement` are leftover remnants). `Menu` is the controller, `MenuScreen` a screen, `MenuElement` subclasses are rows, `Menu/Views/` holds custom `View` subclasses. The mod browser/manager screens are `LevelsMenuScreen`, `InstalledLevelsMenuScreen`, `DownloadLevelsMenuScreen`.
- `API/` — gdtr.net backend client. Endpoint `http://gdtr.net/api.php` (API `VERSION = 2`), requests run on a thread pool (`ExecutorService`) via `HttpURLConnection`. **Active calls: `getLevels`** (mod browser listing) and **`downloadMrg`** (level pack download). Methods `sendStats`, `sendKeyboardLogs`, `getNotifications` exist in the class but are not invoked — removed from call sites for privacy reasons. Callback style: `Request` + `ResponseHandler` with typed `LevelsResponse`/`NotificationsResponse`. `gdtr.net` is plain **HTTP**; cleartext is permitted only via `app/src/main/res/xml/network_security_config.xml`, which allowlists `gdtr.net` (and subdomains, e.g. `API.DEBUG_URL` → `dev.gdtr.net`) — a new host must be added there or requests fail silently.
- `Helpers.java` — static utilities, including a **Windows-1251 (CP1251) translation table**: level and menu strings use CP1251 (Russian text), not UTF-8. `Settings.java` wraps `SharedPreferences`. `KeyboardController` drives the on-screen 3×3 numeric control pad. `GDApplication.onCreate()` is empty — crash reporting is handled by Firebase Crashlytics.

## Working in this codebase

- **Do not rename or "modernize" the cryptic identifiers** (`m_longI`, `_avJ()`, `_dovI()`, single-letter fields, class `k`) in `Game/`, `Levels/Loader`, and parts of `Menu/`. They are machine-decompiled from the original J2ME game and the goal is behavior parity with the 2004 original. Treat this like generated code.
- **Leave the large blocks of commented-out code alone** unless the task is specifically about them. They are deliberate artifacts of the J2ME→Android porting process and serve as reference.
- When changing gameplay/physics behavior, remember the reference is the original game's feel — prefer minimal, surgical changes over refactors.
- All user-visible strings are in `res/values*/`. Resource qualifiers in use: tablet (`values-sw600dp`, `values-sw720dp-land`) only; `values/screen.xml` holds screen-metric resources.
- UI colors are centralized in `res/values/colors.xml`; do not add hardcoded hex color literals. Spacing/typography lives in `res/values/dimens.xml`.
- `Html.fromHtml()` must always be called via `Helpers.fromHtml()` — it handles the API 24 signature change.
- `res/layout/` only contains `levels_list_item.xml`; the main view tree is built programmatically in `GDActivity.onCreate()`.
