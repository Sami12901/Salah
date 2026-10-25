# HANDOFF — Salah (Android project)

Read this first if you are a new assistant/agent picking up this project. It exists so you do **not**
have to guess what was done, what was verified, and what is still open.

Last updated: 2026-10-25 · GitHub repo `Sami12901/Salah` · `main` HEAD `3591fad` · no release/tag published. Arena work is on `arena/3a3a9b25-salah`.

---

## 1. What this project is

**Salah** — an offline-first Android app for prayer times, the Quran and daily worship.
Kotlin + Jetpack Compose (Material 3), MVVM with `core/ data/ domain/ feature/` layering, Room,
DataStore, WorkManager, AlarmManager + notifications, Glance widget, Media3 audio.

Hard constraints the original requester set — **do not break these**:

- No Firebase, no custom cloud server, no accounts, no subscriptions, no paid services for core use.
- Never invent religious content: no fabricated hadith, no invented grading, no invented translations,
  no "verified/approved" claims.
- Never fabricate test results or claim device testing that did not happen.
- Never silently change the user's calculation method or Asr school; they are separate settings.
- No hidden network calls — every host must be listed in `API_SOURCES.md` and justified in `PRIVACY.md`.
- Original branding/UI only; no copied assets or third-party app design.
- Never present a hardcoded gold price, nisab value or AlAdhan response as live/current data.

## 2. Current state — verified facts

| Item | State |
| --- | --- |
| Source | 172 tracked files, ~14k lines of Kotlin in `app/src/main/kotlin/com/salahmate/app/` |
| Unit tests | The prior recorded run was **96/96**. CI run `40203476334` passed `:app:testDebugUnitTest`, but its detailed count is only in the uploaded report artifact. |
| Debug build | CI run `40203476334` passed `:app:assembleDebug`. |
| Android lint | CI run `40203476334` passed `:app:lintDebug`. |
| Git | `main` is at merged setup-fix commit `2ee3335`; Arena changes are on `arena/3a3a9b25-salah` (latest `9b7d40a`). No version tag is present. |
| Published to GitHub | Repository exists at `Sami12901/Salah`; **no release published**. |
| Release build | Never run; production signing secrets/keystore not verified. |
| Device testing | **None.** No emulator, no phone, ever |

### Test classes (all passing)

`HijriCalendarTest` 6 · `HijriMonthTest` 5 · `CompassMathTest` 9 · `NotificationPostingTest` 6
(Robolectric) · `NotificationContentTest` 4 · `PrayerAlarmComputeTest` 5 · `AlAdhanParsingTest` 6 ·
`AyahAudioTest` 5 · `BackupCryptoTest` 6 · `LocalAdhanProviderTest` 8 · `QuranAyahNumberTest` 4 ·
`HifzSchedulerTest` 3 · `PrayerMonthTest` 6 · `PrayerScheduleTest` 8 · `ZakatCalculatorTest` 9 ·
`WidgetContentTest` 6

## 3. What is implemented

Prayer times on device (Adhan) + optional AlAdhan provider behind `PrayerProvider` with visible
fallback · separate method/Asr settings · per-prayer minute adjustments · high-latitude rule · Imsak
and last-third values · Room-cached daily schedule · reminders (per-prayer opt-in, silent channel,
advance, snooze, test, exact-with-fallback, re-arm on boot/timezone/time change + daily worker) ·
Glance widget (compact/medium/expanded) · monthly timetable · Quran reader (all 114 surahs bundled
offline, translations fetched+cached, per-ayah audio with offline preference, bookmarks, last-read,
search, downloads) · Hifz tracker · Hadith library (2 bundled collections + optional downloads,
source-supplied grading only) · Dua/Adhkar · Learning (Asma ul Husna, Kalimas, Wudu) · Tasbih ·
Prayer tracking · Ramadan · Islamic calendar · Zakat calculator · Qibla compass · mosque finder
(delegates to maps) · 3-step onboarding · encrypted backup (AES-256-GCM + PBKDF2) · Bengali/English
UI, Arabic RTL option · splash · single-top navigation and Back-to-Home on secondary tabs.

## 4. What is NOT verified — say so, do not paper over it

These have unit-tested *logic* but have **never** run on hardware:

1. Alarm delivery at the exact minute, Doze behaviour, exact-alarm revocation fallback.
2. Glance widget rendering/refresh on a real launcher.
3. Compass with real sensors (maths tested; hardware not).
4. Media3 audio playback through the speaker, seeking, auto-advance.
5. Android 13/14/15 notification+exact-alarm permission dialogs.
6. RTL visual inspection, tablet reflow.

`TESTING.md` holds a 20-step manual device checklist for exactly these.

## 5. Open work, in priority order

1. **Confirm CI on the updated main branch.** The initial main run stopped at
   `android-actions/setup-android@v3` because v3.2.2 requested Google's removed `tools` package. The
   fix is merged to main, and follow-up PR CI run `40203476334` on commit `9b7d40a` passed tests,
   debug build, and lint. The main branch's post-merge run should still be checked.
2. **Release build + R8/ProGuard pass** (`assembleRelease`) — never executed; minification is on for
   release and could break reflection/serialization. The release workflow now requires a signing key
   and creates a draft signed APK/AAB; configure GitHub Actions secrets before tagging.
3. **Device test pass** — run the TESTING.md checklist, then move items out of §4 as they pass. This
   is still a blocker before publishing a stable release.
4. Room migration test v1 → v2 (schema export exists in `app/schemas/`, no migration test yet).
5. Instrumented tests for Compose screens (none exist; `androidTest` has only dependencies).
6. Rename the package `com.salahmate.app` → `app.salah` **only** if breaking upgrades is acceptable
   (applicationId change means a new install, not an update).

**Resolved:** Adhan 1.2.1 is MIT-licensed: the exact upstream `v1.2.1` source tag includes the MIT
license and its README lists `com.batoulapps.adhan:adhan:1.2.1`. The full MIT notice is bundled in the
app. See `CONTENT_LICENSES.md`.

## 6. Environment gotchas — these cost real time

- **Toolchain lives outside the snapshot.** `/opt/jdk`, `/opt/android-sdk`, `/opt/gradle-home` and the
  swap file all vanish on sandbox reset. Rebuild script is in `BUILD.md` § "Rebuilding the toolchain".
- **`.git` is not preserved by workspace snapshots.** Restore with
  `git clone /home/user/salah-repo.bundle .` — regenerate the bundle after any commit you care about:
  `git bundle create /home/user/salah-repo.bundle --all`.
- **`gradlew` loses its executable bit** after a restore → `chmod +x gradlew` (that is the only cause
  of `Permission denied` on `./gradlew`).
- **No swap = Gradle daemon "disappears unexpectedly"** (kernel OOM). Keep `-Xmx1280m`, add swap.
- GitHub authentication depends on the active Arena connection. Use `gh auth status` before a push or PR;
  never request or paste credentials in chat. This session can query the existing repository, but that
  does not imply a signed release keystore or GitHub Actions secrets are available.
- Kotlin quirks already fixed — **do not reintroduce**:
  `flow.first(x)` is invalid here, always `x.first()` · Room cannot persist `Instant` (use Long) ·
  `@Upsert` misuse · Compose BOM must stay 2025.12.01 with AGP 8.13.2 · no `textDirection` parameter
  on `Text` (set it in the `arabicStyle` TextStyle) · `StatusPill` takes `color`, not `positive` ·
  `divider` → `HorizontalDivider`.

## 7. Bugs already found and fixed — do not re-fix or accidentally revert

- Silent prayer notification channel was `IMPORTANCE_DEFAULT` (still played a sound) → now
  `IMPORTANCE_LOW`.
- `PrayerNotifier.cancelAll()` left the test notification (id 9001) alive.
- Alarm cancellation targeted a hardcoded `2000-01-01` placeholder, so real alarms were never
  cancelled → scheduled alarms are now persisted by identity (`prayer|date|advance`) and replayed.
- Back from a secondary tab exited the app; detail screens could stack duplicates → fixed.
- Compass dial jumped at the 0°/360° boundary → shortest-arc smoothing in `CompassMath.smooth`.

## 8. First five commands for a new session

```bash
cd /home/user/salahmate
git clone /home/user/salah-repo.bundle . 2>/dev/null || true   # only if .git is missing
chmod +x gradlew
# rebuild toolchain per BUILD.md if /opt/jdk is missing, then:
export JAVA_HOME=/opt/jdk ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
       GRADLE_USER_HOME=/opt/gradle-home PATH=/opt/jdk/bin:$PATH
./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Expect: 96 tests pass, APK produced. If the numbers differ, investigate before trusting this file.

## 9. Document map

`README.md` overview + verified/not-verified matrix · `BUILD.md` toolchain and commands ·
`TESTING.md` real results + device checklist · `API_SOURCES.md` every endpoint and verified fields ·
`CONTENT_LICENSES.md` content + library licences (incl. the Adhan MIT confirmation) · `PRIVACY.md` data flows and
permissions · `RELEASE_CHECKLIST.md` blockers · `PUBLISHING.md` GitHub/CI setup · `CHANGELOG.md`
changes + known limitations · `CONTRIBUTING.md` project rules.
