# Privacy

Salah has no account system, no cloud backend, no advertising and no analytics. This document
describes what the shipped code actually does.

## Data that stays on the device

Stored in the app's private storage (Room database and DataStore). Nothing here is uploaded:

- Settings: language, theme, location coordinates/city, calculation method, Asr madhab, minute
  adjustments, provider choice, notification preferences, Quran edition and reciter.
- Quran bookmarks, last-read position, reading history, daily ayah progress.
- Hifz portions, review log and notes.
- Prayer tracking entries.
- Tasbih sessions, dua favourites, hadith bookmarks.
- Zakat calculations (including the metal prices *you* typed).
- Downloaded Quran translations and recitation audio (app-private files).
- Hadith collections you chose to download.
- Cached prayer-time responses for the days that have been calculated.

The database lives inside the app sandbox. Other apps cannot read it on a non-rooted device.

## Data that leaves the device

Only these, and only on your action or your explicit provider choice:

| Destination | What is sent | Trigger |
| --- | --- | --- |
| `api.alquran.cloud` | Surah number and edition id | You open/download a translation |
| `cdn.islamic.network` | Reciter id and global ayah number (as a URL) | You play/download recitation |
| `cdn.jsdelivr.net` | Collection id (as a URL) | You tap Download on a hadith collection |
| `api.aladhan.com` | Latitude, longitude, date, method and school id | Only if you switch the provider to AlAdhan |

These requests contain no identifier, no account, no device id and no analytics payload. Request
bodies are never used to build a profile; the responses are cached locally solely so content can be
shown offline.

With `Provider = On-device calculation (Adhan)`, which is the default, **no prayer-time network
request is ever made**.

## Location

- Coordinates come from your last chosen city, manually entered values, or a one-time GPS/network fix
  you explicitly request.
- Location is used only to compute prayer times, the Qibla bearing and the map link you tap.
- Location is not stored anywhere except in your local settings, and it is never transmitted by the
  app itself. If you use the AlAdhan provider, coordinates are part of the request described above.
- No background location permission is requested.

## Encryption and backups

- Local data is protected by Android's app sandbox and full-disk/file-based encryption.
- Backups are created only when you export one. The export is encrypted with **AES-256-GCM** using a
  key derived from your password with **PBKDF2-HMAC-SHA256** (60,000 iterations, random 16-byte salt,
  random 12-byte IV, 128-bit authentication tag). The file header carries the magic `SMBK1`.
- The password is never stored. If you lose it, the backup cannot be recovered — and that is the point.

## Permissions and why each exists

| Permission | Why |
| --- | --- |
| `INTERNET` | Fetch translations, audio and optional hadith downloads; optional AlAdhan provider |
| `ACCESS_NETWORK_STATE` | Detect offline state so the UI can say "not downloaded yet" instead of failing silently |
| `ACCESS_COARSE_LOCATION` | Optional: get an approximate location for prayer times when you tap "use my location" |
| `POST_NOTIFICATIONS` | Prayer reminders (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-arm reminders after a reboot |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Deliver reminders at the exact minute, when the OS allows it |
| `VIBRATE` | Notification vibration and tasbih haptics |

No `ACCESS_FINE_LOCATION`, no `ACCESS_BACKGROUND_LOCATION`, no contacts, SMS, storage-wide or camera
access is requested. `android:allowBackup` is disabled so the app's data is not copied off-device by
the system backup service without your knowledge.

## Children and tracking

There is no age gate, no account, no advertising identifier read, and no third-party SDK that would
collect data. Downloaded content is served over HTTPS.

## Your control

- Data settings → export/import an encrypted backup, or delete downloaded Quran audio/translations and
  downloaded hadith collections.
- Uninstalling the app deletes all local data.
- Notification and exact-alarm state is shown in the app so you can see when the OS is restricting it.

## Changes

This file describes the code in this repository. If a future change adds any network destination or
permission, this document and `API_SOURCES.md` must be updated in the same commit.
