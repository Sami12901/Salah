# Contributing to Salah

Thanks for helping. This project has a few firm rules that keep it trustworthy for people who rely on
it for daily worship.

## Non-negotiables

1. **Never invent religious content.** No fabricated hadith, no invented gradings, no "verified"
   claims. If a source does not provide a grading, the UI shows none.
2. **Never fabricate test results or approval.** If something was not run on a device, say so.
3. **Do not silently change a user's calculation method or Asr school.** They are separate settings.
4. **No hidden network calls.** Every host the app may contact must be listed in `API_SOURCES.md`.
   A PR that adds an endpoint without updating that file (and `PRIVACY.md` if data leaves the device)
   will be rejected.
5. **No analytics, ads, or account systems.** The app must work fully offline for prayer times and
   Quran text.
6. **No copied UI or assets.** Original branding and layouts only.

## Getting set up

See [BUILD.md](BUILD.md). The short version:

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Before opening a pull request

- [ ] `./gradlew :app:testDebugUnitTest` passes, and you added tests for new pure logic
      (calculation, formatting, scheduling rules — anything without Android dependencies).
- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] Kotlin/Compose code follows the existing package layout: `core/`, `data/`, `domain/`, `feature/`.
      Display logic that can be reasoned about without Android belongs in `domain/` or an `*Math`/
      `*Content` object so it can be unit-tested.
- [ ] User-visible text goes through `t(bangla, bn, en)` (or `t3` where Arabic is translated).
- [ ] Any new permission is justified in `PRIVACY.md`.
- [ ] At least one real build/test result is quoted in the PR description.

## Reporting a wrong prayer time

Include: city and country, the calculation method and Asr school selected in Settings, the app
version, and the time you expected vs what the app showed. Differences of a few minutes against a
local mosque are expected — the app calculates, it does not receive a mosque's timetable.

## Reporting a content error

Point at the entry and its citation field. If a translation or hadith text is wrong, the fix usually
belongs upstream (AlQuran.cloud / the hadith dataset) — describe what you see and we will decide
together where the correction should live.

## Commit style

Short imperative subject lines, one concern per commit, e.g. `fix: silent channel no longer plays a
sound`. Reference the issue number when there is one.
