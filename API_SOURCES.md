# Network sources used by Salah

The app is offline-first. Only three kinds of request are ever made, and each happens either because
the user asked for it or because the user explicitly selected a non-local prayer-time provider.

| Purpose | Host | When |
| --- | --- | --- |
| Bengali/English Quran translations | `api.alquran.cloud` | When a translation is opened or downloaded |
| Quran recitation audio | `cdn.islamic.network` | When the user plays or downloads audio |
| Hadith collections (optional downloads) | `cdn.jsdelivr.net` (hadith-api) | Only when the user taps Download |
| Prayer times via API | `api.aladhan.com` | Only if the provider setting is switched to AlAdhan |

No analytics, crash reporting, advertising or telemetry endpoint is contacted.

---

## AlAdhan — optional prayer time provider

Enabled only when the user changes the provider setting from "On-device calculation (Adhan)" to
"AlAdhan API". Core functionality works without it.

**Daily times**

```
GET https://api.aladhan.com/v1/timings/{DD-MM-YYYY}?latitude={lat}&longitude={lon}&method={id}&school={0|1}
```

Fields confirmed by an actual request (Dhaka, 24-10-2026, `method=1&school=1`):

- `data.timings` — `Fajr, Sunrise, Dhuhr, Asr, Sunset, Maghrib, Isha, Imsak, Midnight, Firstthird, Lastthird` as `"HH:MM"` strings.
- `data.meta.timezone` — e.g. `Asia/Dhaka`.
- `data.meta.method.id`, `data.meta.method.name`, `data.meta.school`.
- `data.date.gregorian.date` — `"24-10-2026"`.

**Calendar (month)**

```
GET https://api.aladhan.com/v1/calendar/{YYYY}/{MM}?latitude={lat}&longitude={lon}&method={id}&school={0|1}
```

- Returns 31 day objects with the same `data[]` shape.
- Timings in this endpoint include an offset suffix, e.g. `"04:35 (+06)"`. **The parser strips it**;
  the app converts clock strings into `Instant`s using the response's own timezone.

**Error shape (verified)**

Requesting an unknown city returns HTTP 200 with a body of
`{"code":503,"status":"SERVER_ERROR","data":"Geocoding is temporarily unavailable..."}`.
The provider therefore checks the body `code`, not just the HTTP status, and surfaces
`ProviderError.SERVER_ERROR` to the UI.

**Important:** `data.meta.method.location` is the *method's reference city* (for `method=1` that is
Karachi), not the user's location. The app never displays it as the user's place.

**Method mapping used** (Adhan → AlAdhan `method`):

| Adhan method | AlAdhan id |
| --- | --- |
| Muslim World League | 3 |
| University of Islamic Sciences, Karachi | 1 |
| Egyptian General Authority | 5 |
| Umm al-Qura | 4 |
| Dubai | 16 |
| Qatar | 10 |
| Kuwait | 9 |
| Singapore | 11 |
| Moonsighting Committee | 8 |
| North America (ISNA) | 2 |

Default: Karachi (1) + Hanafi Asr (`school=1`), matching what most Dhaka timetables use. The user can
change both, and the choice is never altered silently.

---

## AlQuran.cloud — translations

| Request | Result confirmed |
| --- | --- |
| `GET /v1/edition?language=bn` | `bn.bengali`, `bn.hoque` |
| `GET /v1/edition?language=en` | `en.sahih`, `en.pickthall`, `en.yusufali`, `en.hilali` |
| `GET /v1/edition?format=text&type=quran` | `quran-uthmani`, `quran-simple`, `quran-tajweed`, ... |
| `GET /v1/surah/{n}/editions/{edition}` | Parallel ayah list aligned by `numberInSurah` |
| `GET /v1/quran/quran-uthmani` | Full text (2.1 MB) — used once at build time, not by the app |

The app requests a **single surah** for one edition and caches it. The whole Quran is never
re-downloaded automatically. Only editions that were confirmed to exist are offered in settings
(`bn.bengali`, `bn.hoque`, `en.sahih`, `en.pickthall`, `en.yusufali`).

Arabic text: bundled from `quran-uthmani` into `assets/quran/quran_uthmani.json`
(114 surahs / 6236 ayahs, validated by `tools/fetch_quran_text.py`). The reader never fetches Arabic
text at runtime.

## cdn.islamic.network — recitation audio

```
https://cdn.islamic.network/quran/audio/128/{reciterEdition}/{globalAyahNumber}.mp3
```

`globalAyahNumber` is the mushaf-wide ayah index, computed in-app from the bundled surah lengths
(`QuranAudioController.globalAyahNumber`, unit-tested). Reciters offered (confirmed present in
`/v1/edition?format=audio`): `ar.alafasy`, `ar.husary`, `ar.abdulbasitmurattal`, `ar.minshawi`,
`ar.mahermuaiqly`, `ar.shaatree`.

## Hadith dataset (optional downloads)

```
https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/{ara|ben|eng}-{name}.min.json
```

- Collections advertised: `bukhari, muslim, abudawud, ibnmajah, malik, nasai, nawawi, tirmidhi,
  dehlawi, qudsi`.
- Upstream raw shape: `{metadata:{name,sections,section_details}, hadiths:[{hadithnumber, arabicnumber, text, grades, reference:{book, hadith}}]}`.
- Repository licence: **The Unlicense** (public domain dedication), per the upstream repository.

Two collections are bundled in `assets/hadith/` (an-Nawawi's Forty, Forty Hadith Qudsi) so the module
works offline out of the box; the remainder must be downloaded by the user. Every narration displays
its collection, book and hadith number, and grading only where the dataset supplies it, attributed to
the named scholar.

**Sunnah.com API** was *not* integrated: it requires an API key that is not available, and no
unauthenticated access is assumed. If a key is added later, it fits behind the same repository
interface.

---

## Map links (no data leaves the device until the user taps)

The mosque finder builds an `ACTION_VIEW` intent (`geo:` URI, or a `google.com/maps` URL when no maps
app exists). The app itself queries no mosque API and bundles no mosque list.
