# Changelog

All notable changes to this project. Format follows [Keep a Changelog](https://keepachangelog.com/),
versioning follows [Semantic Versioning](https://semver.org/).

## [1.0.0] — unreleased

First complete build. Everything below is implemented; hardware-dependent behaviour is marked.

### Added
- Prayer times calculated on device with Adhan (works offline), plus an optional AlAdhan provider
  behind the same interface with a visible fallback.
- Separate calculation-method and Asr-school settings, per-prayer minute adjustments, high-latitude
  rule, Imsak and last-third-of-night values, daily cache.
- Reminders: per-prayer opt-in, silent mode, advance reminders, snooze, test notification, exact-alarm
  request with an honest inexact fallback, re-arming on boot/timezone/time change and a daily worker.
- Home-screen widget (compact/medium/expanded) with next prayer, countdown, schedule and Hijri date.
- Quran reader: all 114 surahs bundled offline, Bengali/English translations fetched on demand and
  cached, per-ayah audio with offline preference, bookmarks, last-read, search, download manager.
- Hifz tracker with spaced review intervals, review log and notes.
- Hadith library with two bundled collections and optional downloads; references and
  source-supplied grading only.
- Dua & adhkar, Asma ul Husna, Kalimas and a Wudu guide, all with citations.
- Tasbih counter with persisted sessions, prayer tracking, Ramadan timetable, Islamic calendar,
  Zakat calculator with a documented model, Qibla compass with numeric fallback, mosque finder that
  delegates to a maps app.
- Three-step first-run onboarding (language, location, method + reminders) and a monthly timetable.
- Encrypted local backup/restore (AES-256-GCM + PBKDF2).
- Bengali, English and RTL Arabic interface options; light/dark themes; TalkBack labels.

### Fixed
- Android CI stopped before tests because setup-android v3 requested Google's removed `tools` package;
  CI now uses setup-android v4 and installs only `platform-tools`.
- Release automation now requires a release keystore, builds minified APK + AAB, verifies the APK, and
  creates a draft release candidate instead of publishing a debug-signed APK.
- Adhan 1.2.1's licence is confirmed as MIT from the exact upstream tag; its complete notice is bundled.
- The "silent" notification channel was created at `IMPORTANCE_DEFAULT`, which still plays a sound;
  it is now `IMPORTANCE_LOW` (found by the new notification tests).
- "Cancel all notifications" left the test reminder behind; all app-owned ids are now cancelled.
- Alarm cancellation targeted a hardcoded placeholder date, so real pending alarms were never
  cancelled; scheduled alarms are now tracked by identity and cancelled precisely.
- Back from a secondary tab exited the app instead of returning to Home; detail screens could stack
  duplicates; both fixed.
- The compass dial jumped at the 0°/360° boundary; heading is now smoothed on the shortest arc.

### Known limitations
- Not verified on hardware: notification delivery at the alarm minute, widget rendering, compass
  accuracy, audio playback, Android 13/14 permission dialogs, RTL visual inspection.
- The Adhan POM has an empty licence field, but its upstream v1.2.1 tag includes the MIT licence and lists the same artifact coordinate; see CONTENT_LICENSES.md.
- Hijri dates use the tabular algorithm plus a user offset; local moon sighting can differ by a day.
