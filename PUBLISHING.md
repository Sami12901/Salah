# Publishing this repository

The repo is already initialised locally on branch `main` with one commit and a `v1.0.0` tag.
There is **no GitHub credential in this environment**, so the push below has not been performed —
run it from a machine (or terminal) where you are signed in to GitHub.

## 0. One-command option

```bash
cd salahmate
export GITHUB_TOKEN=ghp_xxxxxxxx      # classic token with "repo", or a fine-grained token with
                                      # Administration: read/write on your account
./publish.sh YOUR_GITHUB_USERNAME salah
```

`publish.sh` creates the repository through the API, pushes `main` and every tag, and never writes
the token to `.git/config` (it uses a one-shot credential helper). If the repo already exists it says
so and just pushes. It also refuses to run without a token rather than failing halfway.

The manual route is below.

## 1. Create the empty repository

Web UI: <https://github.com/new> → name `salah` → **do not** add a README, .gitignore or licence
(all three already exist here). Then, from the project directory:

```bash
cd salahmate            # the workspace folder name; the GitHub repo can be called "salah"
git remote add origin https://github.com/OWNER/salah.git
git push -u origin main
git push origin v1.0.0
```

Or with the GitHub CLI (creates and pushes in one step):

```bash
cd salahmate
gh repo create salah --public --source=. --remote=origin --push
git push origin v1.0.0
```

## 2. Replace the `OWNER` placeholder

The CI badge and the issue-template link use `OWNER` as a placeholder. After creating the repo:

```bash
# Linux
sed -i "s/OWNER/YOUR_GITHUB_USERNAME/g" README.md .github/ISSUE_TEMPLATE/config.yml
# macOS
sed -i '' "s/OWNER/YOUR_GITHUB_USERNAME/g" README.md .github/ISSUE_TEMPLATE/config.yml
git commit -am "docs: point CI badge at the real repository" && git push
```

## 3. What CI does automatically

`.github/workflows/android.yml` runs on every push and pull request:

1. JDK 17 + Android SDK setup, Gradle caching.
2. `./gradlew :app:testDebugUnitTest` — the 96 unit tests. The HTML report is uploaded as an artifact.
3. `./gradlew :app:assembleDebug` — the APK is uploaded as the `salah-debug-apk` artifact.
4. A separate, non-blocking `lintDebug` job.

`.github/workflows/release.yml` runs when a `v*` tag is pushed: it re-runs the tests, builds the APK,
writes a `SHA256SUMS.txt`, and attaches both to the GitHub Release. That APK is **debug-signed** on
purpose — a release keystore must never be committed. To produce a store-ready build, add these
repository secrets and switch the task in the workflow to `:app:assembleRelease`:

| Secret | Meaning |
| --- | --- |
| `SALAHMATE_KEYSTORE` | Base64 of the `.jks` file (decode it in a CI step) |
| `SALAHMATE_KEYSTORE_PASSWORD` | Keystore password |
| `SALAHMATE_KEY_ALIAS` | Key alias |
| `SALAHMATE_KEY_PASSWORD` | Key password |

See [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) before tagging anything as stable.

## 4. Repository settings worth turning on

- **Branches → Protect `main`**: require the `Build and test` check before merging.
- **Security → Dependabot alerts**: on. `gradle` ecosystem updates are safe to accept.
- **Actions → General**: leave the default read-only `GITHUB_TOKEN`; `release.yml` requests
  `contents: write` only for itself.
- **Topics**: `android`, `kotlin`, `jetpack-compose`, `prayer-times`, `quran`, `offline-first`,
  `islamic-app`.
- **About section**: short description + the offline-first badge; link the Releases page.

## 5. Repository conventions already in place

| File | Purpose |
| --- | --- |
| `.gitignore` | Ignores `build/`, `.gradle/`, `local.properties`, keystores, `artifacts/` |
| `LICENSE` | MIT for the code, with an explicit note that third-party content is not relicensed |
| `CONTRIBUTING.md` | The project's non-negotiables (no invented religious content, no hidden network calls) |
| `CHANGELOG.md` | Keep-a-Changelog format, with a "Known limitations" section |
| `.github/pull_request_template.md` | Requires quoting real test output |
| `.github/ISSUE_TEMPLATE/` | Bug and feature templates that ask for method/madhab for time issues |

## 6. If the local repo is lost

Workspace snapshots exclude `.git/config` and credentials, so the local clone can end up in a broken
state after a sandbox reset. A full bundle is generated alongside it:

```bash
# from the workspace root
git bundle create salah-repo.bundle --all      # single file containing every commit and tag
git clone salah-repo.bundle salah-restored     # restore anywhere
```

Re-run that bundle command after any commit you care about.

## 7. Before the first public release

Work through [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Two items are genuine blockers:

1. The `com.batoulapps.adhan:adhan:1.2.1` licence is unconfirmed (empty `<license/>` in its POM).
2. Hardware behaviour — alarm delivery at the exact minute, widget rendering, compass accuracy and
   audio output — has not been tested on a device. The maths and content behind them are unit-tested;
   the device behaviour is not.
