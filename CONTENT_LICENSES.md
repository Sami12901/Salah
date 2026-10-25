# Content and library licences

## Religious content in the app

| Content | Source | Status |
| --- | --- | --- |
| Quran Arabic text (Uthmani) | AlQuran.cloud `quran-uthmani` edition, downloaded once at build time into `assets/quran/quran_uthmani.json` | The Quran text itself is not copyrightable; the digital edition is distributed by AlQuran.cloud as an open dataset. Attribution is shown in the app |
| Bengali translation | `bn.bengali` (Muhiuddin Khan) and `bn.hoque` (Zohurul Hoque) via AlQuran.cloud | Fetched on demand; translator shown under every translated ayah |
| English translation | `en.sahih` (Saheeh International), `en.pickthall`, `en.yusufali` via AlQuran.cloud | Fetched on demand; translator shown |
| Recitation audio | `cdn.islamic.network` (AlQuran.cloud CDN) | Streamed or downloaded by explicit user action; reciter named in settings |
| Hadith — an-Nawawi's Forty, Forty Hadith Qudsi (bundled) | Fawaz Ahmed's `hadith-api` dataset (originally from sunnah.com), processed by `tools/fetch_hadith.py` | Dataset published under **The Unlicense**; provenance and grading preserved verbatim |
| Hadith — other collections (optional) | Same dataset, downloaded by the user at runtime | Same licence; nothing fixed into the APK |
| Duas, adhkar, Asma ul Husna, Kalimas, Wudu guide | Compiled locally from well-known published sources; each entry carries its own citation field | Sources are displayed in the UI with each entry |

### What is deliberately *not* included

- No hadith text, grading or reference was written by this project. Where the source has no grading,
  the UI shows none.
- No third-party app's content, UI design, artwork or brand was copied. The app name **Salah**, the
  mihrab/crescent/8-point-star mark in `docs/brand/` and all layouts are original to this project.
- No adhan audio recording is bundled: no recording with verified redistribution rights was available.
  The app therefore uses the system notification sound (or silent channel) and says so in settings
  rather than shipping audio of unknown provenance.
- No mosque database is bundled or invented; the mosque finder delegates to a maps app.

## Third-party libraries

| Library | Version | Licence |
| --- | --- | --- |
| AndroidX Core / Lifecycle / Activity / Navigation | per `gradle/libs.versions.toml` | Apache License 2.0 |
| Jetpack Compose (BOM 2025.12.01), Material 3 | 2025.12.01 | Apache License 2.0 |
| Room | 2.8.5 | Apache License 2.0 |
| DataStore | 1.3.0 | Apache License 2.0 |
| WorkManager | 2.12.0 | Apache License 2.0 |
| Glance (appwidget) | 1.2.0 | Apache License 2.0 |
| Media3 ExoPlayer | 1.8.0 | Apache License 2.0 |
| Core splashscreen | 1.0.1 | Apache License 2.0 |
| Kotlin standard library / kotlinx.coroutines / kotlinx.serialization | 2.3.21 / 1.10.2 / 1.9.0 | Apache License 2.0 |
| Retrofit | 3.0.0 | Apache License 2.0 |
| OkHttp | 5.1.0 | Apache License 2.0 |
| Adhan (batoulapps) | 1.2.1 | **Unverified.** The published POM contains an empty `<license/>` element and the repository does not state a licence file at the artefact level. Treated as a **release blocker** until confirmed: either confirm the licence from the upstream repository, or replace the library with an in-house calculation. See RELEASE_CHECKLIST.md |
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| Robolectric | 4.16 | MIT |
| MockK | 1.14.7 | Apache License 2.0 |

## Fonts

Arabic text uses the platform's Arabic-capable font family (see `core/design/Theme.kt`). No font file
is bundled, so no font licence applies to the APK.

## Attribution in the app

Attribution is not only in this file: translation edition/translator appears under translated ayahs,
the hadith collection source and licence appear at the end of each collection, and dua/learning entries
carry their citation. About → *Sources & licences* summarises them on device.
