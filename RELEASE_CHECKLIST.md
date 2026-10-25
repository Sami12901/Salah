# Release checklist

## Blockers (must be resolved before any public release)

- [ ] Run the manual device checklist in [TESTING.md](TESTING.md) on at least one Android 12, one
      Android 14 and one Android 16 device.
- [ ] Confirm a production signing keystore is available to the release workflow and never commit
      it to the repository. The workflow publishes only a **draft** release candidate for review.

### Resolved release review

- [x] **Adhan `com.batoulapps.adhan:adhan:1.2.1` licence confirmed as MIT** against the upstream
      `v1.2.1` source tag and its exact Maven coordinates; the full notice is bundled at
      `app/src/main/assets/licenses/adhan-MIT.txt`. See [CONTENT_LICENSES.md](CONTENT_LICENSES.md).
- [ ] Verify the widget renders and refreshes on a real launcher.
- [ ] Verify notification delivery, snooze, reboot re-arming and the inexact fallback.
- [ ] Verify compass behaviour and the low-accuracy path on a device with and without a magnetometer.

## Build

- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` — all tests pass
- [ ] `./gradlew :app:assembleRelease`
- [ ] Keystore held outside the repository; `SALAHMATE_KEYSTORE*` environment variables set
- [ ] `app/build.gradle.kts` versionCode/versionName bumped
- [ ] Room schema export committed (`app/schemas/`) and a migration test present if the schema changed
- [ ] Check the release APK for `android:debuggable="false"` and that no test/debug tooling remains
- [ ] `./gradlew :app:dependencies` reviewed for unexpected transitive additions

## Content

- [ ] Every hadith shown carries a reference and, where the source supplies one, a grading with the
      scholar's name
- [ ] Translation attribution appears with each translated text
- [ ] No audio or font file with unknown provenance was added
- [ ] `CONTENT_LICENSES.md` matches the shipped assets exactly (`app/src/main/assets/**`)

## Privacy and policy

- [ ] `PRIVACY.md` matches the permissions in `AndroidManifest.xml` exactly
- [ ] `API_SOURCES.md` matches every host the code can contact
- [ ] No analytics/crash SDK was introduced
- [ ] Play Store data-safety declaration filled in from `PRIVACY.md` (no data collected, none shared)

## Store listing

- [ ] Screenshots from a real device (phone + tablet), no mocked timings presented as live
- [ ] Description states that prayer times are calculations and may differ from local timetables
- [ ] Description states that the app issues no religious rulings and is not affiliated with any
      authority
- [ ] Content rating and target-audience questionnaires completed honestly

## Post-release

- [ ] Tag the commit; attach the APK and AAB checksums
- [ ] Re-verify the three content APIs still respond in the documented shape (see API_SOURCES.md)
- [ ] Re-check hadith dataset availability and licence
