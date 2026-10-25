# Testing Salah

This document records **actual** results from this build environment, and states clearly what could
not be executed here. No test result below is estimated.

Environment: JVM unit tests via `./gradlew :app:testDebugUnitTest`, JDK 17 (Temurin 17.0.21), Robolectric available but
not required by the current tests. No emulator and no physical device were available, so nothing is
claimed about runtime behaviour on hardware.

## Last run

```
BUILD SUCCESSFUL — :app:testDebugUnitTest
96 tests, 96 passed, 0 failed, 0 skipped
```

| Test class | Tests | Result | What it covers |
| --- | --- | --- | --- |
| `core.HijriCalendarTest` | 6 | pass | Gregorian ↔ Hijri conversion against fixed known dates, offset handling, malformed input |
| `core.HijriMonthTest` | 5 | pass | 29/30-day month lengths, year length, month ordering, localized month names |
| `data.AlAdhanParsingTest` | 6 | pass | Parsing recorded AlAdhan responses from `src/test/resources/fixtures/`, timezone-offset stripping, `24:00` handling, error payloads |
| `data.BackupCryptoTest` | 6 | pass | Encrypt/decrypt round trip, magic header, randomised salt+IV, wrong-password rejection, non-envelope rejection |
| `data.LocalAdhanProviderTest` | 8 | pass | On-device calculation vs recorded AlAdhan values for Dhaka/London/Oslo within ±2 minutes |
| `data.QuranAyahNumberTest` | 4 | pass | Global ayah numbering used for audio URLs, out-of-range input |
| `domain.HifzSchedulerTest` | 3 | pass | Review intervals 1/3/7/14/30/60 days, daily review for learning/needs-revision, due detection |
| `domain.PrayerMonthTest` | 6 | pass | Month grids incl. leap years, month shifting across year ends, formatted rows, partial-day flagging, gap detection, Hijri offset |
| `domain.PrayerScheduleTest` | 8 | pass | Current/next prayer selection, boundaries, countdown maths, qibla sanity |
| `core.CompassMathTest` | 9 | pass | Bearing normalisation, shortest-arc deltas, Qibla arrow rotation, alignment across the 0/360 boundary, circular smoothing, sensor-accuracy mapping, localised cardinal labels, turn guidance |
| `core.NotificationPostingTest` | 6 | pass | **Robolectric**: channels exist with correct importance (silent really silent), channel selection, title wording, snooze action, replacement of a previous alert, nothing posted without permission, cancel clears app-owned ids |
| `core.notify.NotificationContentTest` | 4 | pass | Advance / on-time / snoozed wording in both languages, channel + urgency selection, stable unique ids |
| `core.notify.PrayerAlarmComputeTest` | 5 | pass | Disabled reminders, sunrise excluded, per-prayer opt-out, advance shift, past alarms dropped, sort order and day horizon |
| `data.AyahAudioTest` | 5 | pass | CDN URL layout, per-reciter cache paths, next/previous ayah boundaries, local-vs-stream source |
| `widget.WidgetContentTest` | 6 | pass | Next prayer, countdown, five schedule rows, Hijri line with offset, Bengali labels, empty state for missing/incomplete data, method label |
| `domain.ZakatCalculatorTest` | 9 | pass | 2.5% rate, both nisab bases, debt deduction, never-negative net wealth, missing-price and stale-price warnings |

Re-run with:

```bash
./gradlew :app:testDebugUnitTest --rerun-tasks
```

## What was verified beyond unit tests

- `./gradlew :app:assembleDebug` completes and produces `app-debug.apk` (27 MB).
- `:app:compileDebugKotlin` is clean after the final change set.
- API contracts were checked against live endpoints before being coded: AlAdhan timings and calendar
  responses, AlQuran.cloud editions and ayah audio URLs, the hadith dataset manifest. See
  API_SOURCES.md for exactly which fields were confirmed.
- Adhan's output was checked to be independent of the JVM default timezone (a probe run produced
  identical instants under different `TZ` values).

## Bugs these tests found (and that were then fixed)

Writing the notification and compass tests exposed real defects rather than confirming the code:

1. **The "silent" prayer channel was not silent.** It was created at `IMPORTANCE_DEFAULT`, which still
   plays the notification tone. Now `IMPORTANCE_LOW` — the highest level that never makes a sound.
2. **"Cancel all notifications" left the test reminder behind.** `PrayerNotifier.cancelAll()` only
   cancelled the five prayer ids; the test notification (id 9001) survived. It is now cancelled too.
3. **The compass dial jumped at north.** Headings were used raw, so 359° → 1° animated the long way
   round and the arrow flickered. Heading is now smoothed on the shortest arc (`CompassMath.smooth`).

## What has **not** been verified

Be aware of these before relying on the app:

1. **Notification delivery.** Channel creation, channel choice, wording, the snooze action and the
   permission-denied path are now covered by Robolectric tests. What is *still* unverified is whether
   the OS fires the alarm at the scheduled minute, how it behaves under Doze, and the exact-alarm
   fallback after the user revokes the permission. That needs a device.
2. **Reboot / timezone re-arming.** `BootAndTimeReceiver` is registered and implemented but was not
   triggered on hardware.
3. **Home-screen widget.** The content the widget renders (next prayer, countdown, rows, Hijri line,
   empty state) is unit-tested via `WidgetContent`; the Glance *rendering* and refresh path have not
   been seen on a launcher.
4. **Compass.** The maths (bearing, shortest-arc smoothing, alignment, accuracy mapping, guidance
   text) is unit-tested, but real sensor input, calibration behaviour and the numeric fallback have
   not been exercised on hardware, so no accuracy claim is made.
5. **Audio playback.** URL layout, cache paths and ayah stepping are unit-tested; actual Media3
   playback, seeking, auto-advance and speaker output were not played on a device.
6. **Android 14/15/16 permission flows** (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`) were not
   exercised interactively.
7. **RTL rendering** was not visually inspected. The Arabic UI option switches layout direction and
   translates navigation and module titles; deeper screens fall back to English.
8. **Tablet layout** reflow was not visually verified.
9. `assembleRelease` was not run (no keystore present).

## Manual verification checklist (device required)

| # | Check | Expected |
| --- | --- | --- |
| 1 | Fresh install, airplane mode on | App opens, Quran reader shows Arabic text for all 114 surahs |
| 2 | Home screen | Today's five prayers, sunrise/sunset, Hijri date, countdown ticking |
| 3 | Change calculation method → Dhuhr time changes, Asr school changes Asr only | Method is never changed back automatically |
| 4 | Enable reminders, grant notification permission, wait for one prayer | Notification arrives; snooze repeats after 10 minutes |
| 5 | Deny exact alarms | Inexact fallback used, warning visible, no false "exact" claim |
| 6 | Reboot the device | Reminders still scheduled (BootAndTimeReceiver) |
| 7 | Change device timezone | Schedule recomputed for the new zone |
| 8 | Add the widget | Next prayer, schedule and Hijri date render; refresh after settings change |
| 9 | Quran: play a surah, seek, next ayah | Audio plays; offline surah plays with airplane mode on |
| 10 | Download a surah with audio, then clear cache from Data settings | Files removed, storage number drops |
| 11 | Qibla screen | Arrow moves with device; numeric bearing matches a known value for the location |
| 12 | Zakat with gold price left empty | Warning shown, gold not silently valued at 0 without notice |
| 13 | Backup export with password → import on a clean install | Bookmarks, hifz, tracking, zakat restored; wrong password rejected |
| 14 | Switch language to Arabic | Layout flips to RTL, navigation labels in Arabic |
| 15 | TalkBack on Home and Quran reader | Every control announces a label; no unlabeled icons |
| 16 | Fresh install, first launch | Onboarding appears; after finishing, Home shows the chosen city/method and does not reappear |
| 17 | From the Track tab press system Back | Returns to Home, does not exit the app |
| 18 | Tap the same quick action twice, then Back | Returns to Home in one step (no duplicate screen) |
| 19 | Monthly timetable: switch months and use "this month" | Rows change month, Hijri days update, method/timezone header matches Settings |
| 20 | Month with missing times (high latitude, e.g. Tromsø in June) | Gaps are visible and explained, no `00:00` times invented |
