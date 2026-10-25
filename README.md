# Salah

[![Android CI](https://github.com/OWNER/salah/actions/workflows/android.yml/badge.svg)](https://github.com/OWNER/salah/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![minSdk 24](https://img.shields.io/badge/minSdk-24-blue.svg)](BUILD.md)
[![Kotlin 2.3](https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg)](gradle/libs.versions.toml)
[![Offline first](https://img.shields.io/badge/offline-first-0E5C4A.svg)](#features)

An offline-first Android app for prayer times, the Quran, and everyday worship. Kotlin + Jetpack
Compose (Material 3), MVVM, Room, DataStore, WorkManager, AlarmManager notifications, and a Glance
home-screen widget.

No account, no ads, no analytics, no Firebase, no custom server. Everything a user creates stays in
the app's private storage on the device.

---

## Status of this build

| Item | State |
| --- | --- |
| Debug build | **BUILD SUCCESSFUL** — `./gradlew :app:assembleDebug`, APK 27 MB, copy at `artifacts/salah-1.0.0-debug.apk` |
| Unit tests | Latest CI test task passed; the last recorded detailed count was **96/96** before two scheduler tests were added. The current report is attached to CI run `40205256092`; see [TESTING.md](TESTING.md). |
| Instrumented tests | **Not run** — no emulator or physical device was available in the build environment |
| Signed release build | **Not produced**. CI passed an unsigned R8/ProGuard APK + AAB packaging check; a production keystore and device verification are still required. See [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) |

### What is verified, and how

| Area | Status | Evidence |
| --- | --- | --- |
| Prayer-time calculation | **Verified** | 8 tests vs recorded AlAdhan values (Dhaka/London/Oslo) within ±2 min |
| AlAdhan parsing / failures | **Verified** | 6 tests over recorded fixtures, incl. the 503-in-body error shape |
| Reminder scheduling logic | **Verified as logic** | 5 planner tests; 2 additional Robolectric tests cover alarm count, persisted cancellation identity and inexact fallback. Actual OS delivery remains unverified. |
| Notification posting & channels | **Verified in a Robolectric runtime** | 6 tests: channel importance, channel choice, snooze action, replacement, denied-permission path |
| Widget content | **Verified as data** | 6 tests: next prayer, countdown, schedule rows, Hijri line, empty state |
| Compass maths | **Verified as maths** | 9 tests: bearings, shortest-arc smoothing, alignment, calibration levels |
| Quran audio addressing | **Verified** | 5 tests: CDN URL layout, cache paths, ayah stepping boundaries |
| Zakat, Hifz, Hijri, timetable, backup crypto | **Verified** | 36 tests |
| **Alarm actually firing at the right minute** | **NOT verified** | needs a device — see the 20-step checklist in TESTING.md |
| **Widget rendering on a launcher** | **NOT verified** | needs a device |
| **Compass accuracy with real sensors** | **NOT verified** | needs a device; the maths is tested, the hardware is not |
| **Audio playback through the speaker** | **NOT verified** | needs a device |

Every claim above maps to a test that ran in this environment. Nothing is marked verified because it
"looks right".

## Features

### Prayer times
- Calculated **on device** with [Adhan](https://github.com/batoulapps/adhan-java) — works with no
  network at all.
- Optional **AlAdhan** provider (API mode) behind the same `PrayerProvider` interface; if it fails,
  the app falls back to the local calculation and says so instead of showing nothing.
- Calculation method (10 methods) and Asr madhab (Shafi'i / Hanafi) are **separate, explicit
  settings**. The app never changes the user's method on its own.
- Per-prayer minute adjustments (−60…+60), high-latitude rule, Imsak and last-third-of-night values.
- Daily schedule cached in Room so a cold start in airplane mode still shows today's times.
- Countdown to the next prayer, current prayer, sunrise and sunset.

### Notifications and Adhan-style reminders
- Per-prayer opt-in, silent channel option, advance reminder (0/5/10/15/30 min), snooze (10 min),
  preview/test notification.
- Exact alarms when the OS grants them (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`); otherwise an
  inexact fallback with a visible warning — the app never claims exact delivery when it is not
  granted.
- Re-armed on reboot, timezone change, time change, date change and app update
  (`BootAndTimeReceiver`), after settings changes, and by a daily WorkManager safety net.
- Cancellation is exact: every scheduled alarm's identity is persisted so `cancelAll()` cancels real
  pending intents, not a guessed one.

### First-run setup and navigation
- Three-step onboarding on first launch: language, location (city list, manual coordinates or a
  one-time device fix), calculation method + Asr school, and the reminder opt-in. Every value writes
  the same setting the rest of the app reads, so nothing has to be re-entered later; there is also an
  explicit "skip and use defaults" path.
- Branded launcher splash (androidx core-splashscreen) that hands off to the app theme.
- Back behaviour: on any secondary tab, Back returns to Home instead of exiting from a screen that
  is not the start destination; detail screens use single-top navigation so double-tapping a quick
  action cannot stack duplicates.

### Monthly timetable
- A full month of prayer times (Fajr, sunrise, Dhuhr, Asr, Maghrib, Isha) with the Hijri day beside
  each date, month navigation across year boundaries, and "this month" reset.
- The header always states the calculation method, Asr school, timezone and location, so a shared
  screenshot cannot be mistaken for a mosque's official timetable.
- Days that cannot be computed (common at high latitudes) are shown as visible gaps with an
  explanation and a pointer to the high-latitude setting — never as `00:00` placeholders.

### Home-screen widget (Glance)
- Compact and expanded layouts, next prayer + countdown, full schedule, Hijri date.
- Resizable 3×2 provider (`res/xml/prayer_widget_info.xml`), refreshed by the daily worker and after
  a settings change.

### Quran
- All **114 surahs / 6236 ayahs** of the Uthmani text bundled in `assets/quran/quran_uthmani.json`
  (1.6 MB) — the reader works offline from first launch, no download step.
- Bengali translation (`bn.bengali`, `bn.hoque`) and English (`en.sahih`, `en.pickthall`,
  `en.yusufali`) fetched on demand through AlQuran.cloud, cached to app storage, and served from disk
  afterwards. Attribution is shown under every translated ayah.
- Per-ayah audio playback (Media3 ExoPlayer) with play/pause, seek, next/previous ayah, ayah-by-ayah
  or continuous surah playback; local files are preferred when a surah has been downloaded.
- Bookmarks, last-read position, daily reading goal and progress, Arabic text search, copy/share.
- Download manager: translation only or translation + audio, with progress and a clear failure
  message (no silent partial state).

### Hifz tracker, worship and study modules
- **Hifz**: portions by surah/ayah range with states memorised / learning / needs-revision, review
  intervals of 1/3/7/14/30/60 days, review log, notes, and a "due now" list.
- **Dua & Adhkar**: bundled, attributed dataset with categories, favourites and a daily "read" tick.
- **Hadith**: Two Forty-Hadith collections are bundled offline (an-Nawawi, Qudsi) with references and
  source-supplied grading only. The larger collections are **optional downloads** from a documented
  dataset — never generated at runtime, never scraped.
- **Learning**: Asma ul Husna, the six Kalimas, and a step-by-step Wudu guide.
- **Tasbih**: tap counter with targets, haptics, persisted sessions and history.
- **Ramadan**: suhoor/iftar with Imsak, Fajr, Maghrib and Iftar kept **as four distinct times**, plus
  the 30-day table. No moon-sighting ruling is claimed; the Hijri offset is user-controlled.
- **Islamic calendar**: Hijri month browsing, scriptural dates vs commonly-observed dates clearly
  distinguished, with an explicit note about local moon sighting.
- **Zakat**: fully documented calculation model, user-entered metal prices (never a hardcoded gold
  price), gold/silver nisab basis choice, breakdown, warnings and a stored history.
- **Prayer tracking**: private, local-only daily checklist with a 14-day view.
- **Qibla**: bearing from the Adhan library, device heading from the rotation-vector sensor (or
  accelerometer + magnetometer fallback), calibration guidance, and a numeric fallback when the
  sensor is missing or unreliable. It never claims accuracy it cannot have.
- **Mosque finder**: hands the search to an installed maps app through a `MosqueProvider`
  abstraction. No bundled mosque database is invented.

### Language, theme and accessibility
- Bengali and English UI strings, Arabic content (Quran, dua, hadith) rendered RTL at a
  user-controlled Arabic scale.
- An **Arabic** UI option is available: it switches the whole layout to RTL and translates the
  navigation and module titles. Arabic coverage of deeper screens is partial and falls back to
  English — this is stated in [TESTING.md](TESTING.md) rather than presented as complete.
- Light/dark/system themes with an original emerald-and-gold visual identity (`docs/brand/`), no
  emoji used as icons, vector icons only.
- TalkBack content descriptions on interactive and informational elements, large touch targets,
  scalable fonts, and layouts that reflow on tablets.

---

## Project layout

```
app/src/main/kotlin/com/salahmate/app/
├── core/
│   ├── data/BackupManager.kt        encrypted export/import (AES-256-GCM + PBKDF2)
│   ├── db/                          Room entities, DAOs, database (15 entities, schema v2)
│   ├── design/                      theme, colours, strings, shared components
│   ├── location/                    location provider + built-in city list
│   ├── net/                         OkHttp/Retrofit + kotlinx-serialization plumbing
│   ├── notify/                      channels, scheduler, receiver, notifier, scheduling policy
│   ├── prefs/                       DataStore-backed user settings
│   └── util/                        date/time helpers, Hijri arithmetic
├── data/                            providers and repositories (prayer, quran, hadith, content)
├── domain/                          pure logic: prayer models/schedule, Ramadan, zakat, hifz
├── feature/                         one package per screen area, each with its ViewModel
├── ui/                              activity, navigation graph, ViewModel factory
├── widget/                          Glance widget + updater + receiver
└── workers/                         WorkManager maintenance job
```

Dependencies are wired by hand in `AppContainer` (no DI framework) so ViewModels stay
constructor-injectable and unit-testable.

---

## Getting started

See [BUILD.md](BUILD.md) for the toolchain, the exact commands used in this environment, and the
low-memory Gradle settings that were required. Short version:

```bash
export JAVA_HOME=/opt/jdk
export ANDROID_HOME=/opt/android-sdk
export GRADLE_USER_HOME=/opt/gradle-home   # or your own
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The debug APK is signed with the standard debug key and installs as `com.salahmate.app.debug`, so it
can sit next to a release install.

---

## Documentation

| File | Contents |
| --- | --- |
| [BUILD.md](BUILD.md) | Toolchain, commands, memory settings, common failures |
| [API_SOURCES.md](API_SOURCES.md) | Every network endpoint used, its fields, and what was actually verified |
| [CONTENT_LICENSES.md](CONTENT_LICENSES.md) | Text, translation, hadith and library licensing |
| [PRIVACY.md](PRIVACY.md) | What is stored, what leaves the device, and permissions |
| [TESTING.md](TESTING.md) | Real test results, what is untested, and how to verify on a device |
| [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) | Steps and sign-off list before shipping |
| [HANDOFF.md](HANDOFF.md) | **Start here if you are a new agent/session** — state, open work, gotchas |
| [PUBLISHING.md](PUBLISHING.md) | Creating the GitHub repo, CI, secrets, and how to restore the repo |
| [CHANGELOG.md](CHANGELOG.md) | What changed in each version, including known limitations |

## Honest limitations

- Prayer times are **calculations**. They can differ by a few minutes from a local mosque or an
  official timetable; the app says so in the UI and here.
- Hijri dates use the tabular (Kuwaiti) algorithm plus a user offset. Local moon sighting may differ
  by a day.
- Compass accuracy depends on the device sensors and magnetic interference; the app degrades to a
  numeric bearing rather than pretending.
- Notifications depend on OS permissions (notifications and exact alarms) and on battery
  optimisation settings; the UI reports the actual state instead of promising delivery.
- Audio playback, the widget, the compass and background notification behaviour were **not** verified
  on hardware in this environment.
- Adhan 1.2.1's published POM has incomplete licence metadata, but its upstream `v1.2.1` source
  contains the MIT licence and lists the exact Maven coordinate; the full notice is bundled in the
  app. See CONTENT_LICENSES.md.
