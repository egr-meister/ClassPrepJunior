# ClassPrep Junior

Offline school-day preparation planner for one child, built natively with **Kotlin + Jetpack Compose**.
A parent enters the real weekly timetable and what to bring; the child checks the next school day's list
in the evening and confirms "Tomorrow ready". No accounts, no network, no analytics.

## Features

- Seven-day weekly timetable (school / non-school days), ordered lessons (1, 2, 3 …), repeated subjects allowed.
- Up to 30 manually entered subjects with bundled icons (Maths, English, Science, Art, PE, Music, History, generic).
- Reusable item library (up to 100) with a preloaded *suggestion* list (never auto-inserted).
- Three item sources: **subject items**, **weekday items** ("For the day"), **date-specific homework/tasks**.
- Checklist generated per actual date, deduplicated by stable item id (never by name), grouped into
  *Homework and tasks · Books and stationery · Clothes and sports · Other belongings · For the day*.
- Main "School Planner" screen: weekday strip with previous/next week, ruled lesson list with item counts,
  lesson filter + "All items", aligned checklist, "5 of 8 ready" strip and **Review readiness**.
  Phones stack timetable above checklist; ≥600 dp shows them side by side.
- Review → "A few things still need preparing." / "Everything on your list is ready." → **Confirm ready**
  → Tomorrow ready / Ready for Wednesday screen. Atomic, duplicate-tap safe.
- "Needs review" after any later unticking or plan change; previous confirmations stay in history.
- Parent area behind a 3-second press-and-hold (accidental-entry safeguard, with an accessible dialog alternative).
- Optional, approximate local evening reminder (off by default).
- Preparation history (latest 60), read-only snapshots, deletable by parents.
- Clearly labelled, in-memory **example week** isolated from real data, reminders and history.

## Architecture

Single `:app` module, manual DI (`AppContainer`), MVVM with `StateFlow` and lifecycle-aware collection.

```
com.classprep.junior
├── data/local          Room entities, DAOs, AppDatabase, Migrations
├── data/prefs          DataStore (reminder settings, setup progress, dedupe marker)
├── data/repository     PlannerRepository (transactions + reconciliation), ExamplePlanSource, mappers
├── data/reminders      AlarmManager scheduler, receivers, notifier (Android glue)
├── domain/model        Plain Kotlin models
├── domain/planning     DateSelection, ChecklistGenerator, PlanEngine, ConfigEdit, Validation, suggestions/example
├── domain/reminders    ReminderPolicy (next evening, stale-delivery and eligibility rules)
└── ui/{planner,review,history,parent,setup,common,theme}
```

Date selection, checklist generation, reconciliation, confirmation and reminder eligibility are pure Kotlin in
`domain/*`, driven by an injected `AppClock`, and unit-tested without Android.

**Plan revisions.** Each `PreparationPlanEntity` stores `revision`, a SHA-256 `contentSignature` of the effective
checklist + timetable, and `confirmedRevision`. Confirmed ⇔ `confirmedRevision == revision`. A changed signature
(parent edit, new task) or unticking a confirmed item bumps the revision → *Needs review*. Reconciliation keeps
ticks for unchanged ids, adds new items unticked, drops removed items, preserves date tasks, and runs in the same
Room transaction as the edit. Past plans and history are never rewritten. Parents see an explanation before
saving an edit that affects already-confirmed plans.

## Toolchain (pinned)

| Component | Version |
|---|---|
| JDK | 17 (Temurin in CI; Android Studio JBR 17/21 locally) |
| Gradle (wrapper) | 8.14.3 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| KSP | 2.2.10-2.0.2 |
| Compose BOM | 2025.09.00 (Material 3) |
| Room | 2.7.2 (KSP, schemas exported to `app/schemas`) |
| Navigation Compose | 2.9.0 |
| Lifecycle | 2.9.2 |
| Activity Compose | 1.10.1 |
| DataStore Preferences | 1.1.7 |
| Coroutines | 1.10.2 |
| compileSdk / targetSdk / minSdk | **36 / 36 / 26** |

All directly used libraries are declared directly in `gradle/libs.versions.toml`. SDK values must never be
lowered to work around build problems.

### Android 16 (API 36) behaviour handled

- Edge-to-edge is mandatory: `enableEdgeToEdge()` + `WindowInsets.safeDrawing` for system bars, cutouts and IME.
- Predictive Back: `enableOnBackInvokedCallback="true"`, only `BackHandler`/`OnBackPressedDispatcher` are used.
- Large screens ignore orientation/resizability restrictions: none are declared; layouts adapt at 600 dp.
- No exact alarms, foreground services, or wake locks are used.

## Build

```bash
# debug (no signing credentials needed)
./gradlew assembleDebug
# unit tests + release lint
./gradlew testDebugUnitTest lintRelease
# signed release (fails if credentials are missing; never falls back to debug signing)
./gradlew assembleRelease bundleRelease
```

Outputs:

- APK: `app/build/outputs/apk/release/app-release.apk` (local verification / `adb install`)
- AAB: `app/build/outputs/bundle/release/app-release.aab` (**the only file submitted to Google Play**)

## Release signing (PKCS12)

`app/build.gradle.kts` defines a `release` signing config with `storeType = "PKCS12"` and assigns
`signingConfigs.getByName("release")` to the release build type. Credentials are read from environment
variables or an uncommitted `keystore.properties` in the project root:

```properties
storeFile=/absolute/path/to/classprep-junior-release.p12
storePassword=…
keyAlias=classprep-upload
keyPassword=…
```

Environment variables: `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
`keystore.properties`, `*.p12`, `*.jks` are git-ignored. Secrets are never printed.

GitHub Secrets required by CI:

| Secret | Content |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64` of the `.p12` file |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | `classprep-upload` |
| `ANDROID_KEY_PASSWORD` | key password (same as store password for PKCS12) |

**Keys and Play App Signing.** The generated key is the **upload key**. Enrol in Play App Signing when creating
the app: Google holds the **app signing key** and re-signs APKs delivered to devices; you sign every AAB with the
upload key. Keep a backup of the `.p12` and passwords; if the upload key is lost it can be reset through Play
Console support. Upload certificate SHA-256:
`45:65:D6:25:92:CB:FB:3A:55:FD:C7:12:88:0E:78:C2:47:55:4C:CE:52:3D:84:13:35:8E:1A:36:C8:C4:A2:12`.

## CI (`.github/workflows/android.yml`)

1. JDK 17 + Android SDK Platform 36 / Build-Tools 36.0.0, committed Gradle Wrapper (validated).
2. `testDebugUnitTest` + `lintRelease`.
3. Decodes the PKCS12 keystore into `$RUNNER_TEMP`, builds signed APK + AAB.
4. `apksigner verify --print-certs` — fails on `CN=Android Debug` or missing v2 signature.
5. `jarsigner -verify` on the AAB and comparison of the AAB signer SHA-256 with the keystore certificate
   (a self-signed upload certificate is expected and not treated as invalid).
6. `scripts/check_permissions.sh` (allowlist) and `scripts/check_16kb.sh` (native libraries / alignment).
7. Uploads the verified APK + AAB, then deletes the temporary signing material.

No emulator test runs in CI.

## R8 / resource shrinking

Release builds are **not minified by default**; the signed non-minified release is the verified baseline.
After that is verified, build with `./gradlew assembleRelease bundleRelease -PenableR8=true` to enable R8 and
resource shrinking, then repeat installation and core-flow checks (including the reminder receivers and a test
reminder). Keep `app/build/outputs/mapping/release/mapping.txt` for every minified release.

## Offline behaviour, privacy and storage

- No `INTERNET` / `ACCESS_NETWORK_STATE`; first launch works in airplane mode. Fonts, icons and illustrations are bundled.
- Data lives in app-private storage: Room database `classprep.db` + DataStore `settings`.
- `android:allowBackup="false"`, `fullBackupContent` (≤ Android 11) and `dataExtractionRules` (Android 12+) exclude
  everything from cloud backup and device-to-device transfer.
- No school login, student name, contact information or identifiers are requested.
- Target dates are stored as ISO local dates (`yyyy-MM-dd`); confirmation times as UTC epoch millis.
- "Clear all local data" cancels reminders, deletes all tables and preferences, and returns to first-run setup.
- The in-app Privacy screen (Parent area → Privacy) explains the above.

## Permission allowlist

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Optional evening reminder (requested on Android 13+ only after a parent enables it, once) |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule the reminder after reboot |

AndroidX core adds the signature-protected `com.classprep.junior.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
(app-internal, not user-facing); the CI check allows exactly that one. No other permissions may appear.

## Reminder scheduling

- Off by default; configured only in the parent area. Default 19:00, adjustable 16:00–22:00 in 15-minute steps.
- One inexact `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP)` alarm for the next evening whose following day is a
  school day. No `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`, no wake-lock permission, no foreground service, no
  battery-optimisation request. The time is shown as "Around 19:00" — Android may delay delivery.
- `ReminderAlarmReceiver` (not exported, explicit immutable PendingIntent) uses `goAsync()` for a few
  milliseconds of work: drops stale deliveries (another day, or > 3 h late / after 23:30), checks eligibility
  (tomorrow is a school day, has items, not confirmed, not already notified), posts, and schedules the next evening.
- `ReminderRescheduleReceiver` handles `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`;
  the app also reschedules on every launch and after reminder-setting or timetable edits.
- The notification text is generic ("Prepare for tomorrow" / "Your school checklist is ready to review.") — no
  homework text on the lock screen. Tapping opens tomorrow's plan with the planner as the root.
- Blocked app notifications or a disabled reminder channel are detected and shown with a link to settings;
  planning keeps working.
- **Force-stop:** Android cancels the app's alarms; reminders resume the next time the app is opened.
- Debug builds have a "Debug: test in ~1 min" button (Parent area → Evening reminder) that schedules an inexact
  near-future alarm through the same receiver for local verification. Release scheduling is unchanged.

## 16 KB page-size compatibility

See *Verification results* below for the inspection of the actual APK/AAB. The app has no NDK code;
`scripts/check_16kb.sh` lists any `.so` files (including transitive ones) and checks ELF LOAD alignment and
zip alignment if any appear. Targeting API 36 alone is not treated as proof of 16 KB compatibility.

## Local verification with adb

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -v time | grep -E "ClassPrep|AndroidRuntime|FATAL"
adb shell dumpsys alarm | grep -A3 com.classprep.junior          # pending inexact alarm
adb shell dumpsys package com.classprep.junior | grep permission  # granted/requested permissions
```

## Google Play

Upload **only** `app-release.aab`. Use the APK for local installation checks.

## Verification results

See `VERIFICATION.md`.
