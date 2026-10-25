# Building Salah

## Toolchain

| Component | Version / path used |
| --- | --- |
| JDK | 17 (`/opt/jdk` in this environment) |
| Android SDK | `compileSdk` 36, `targetSdk` 36, `minSdk` 24 (`/opt/android-sdk`) |
| Gradle | wrapper 8.14.3 (`./gradlew`) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.3.21 (KSP 2.3.12, Kotlin serialization plugin) |
| Compose BOM | 2025.12.01 (Material 3) |
| Room | 2.8.5 (KSP, schema export to `app/schemas`) |
| Gradle user home | `/opt/gradle-home` in this environment |

Java 17 is required: the project uses `java.time` on API 24 with core library desugaring enabled.

## Commands

```bash
cd salahmate
export JAVA_HOME=/opt/jdk
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export GRADLE_USER_HOME=/opt/gradle-home     # any writable directory
export PATH=$JAVA_HOME/bin:$PATH

./gradlew :app:compileDebugKotlin      # fast feedback loop
./gradlew :app:testDebugUnitTest       # JVM unit tests
./gradlew :app:assembleDebug           # debug APK
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (28.2 MB in this build). A copy of the APK from this
build is kept at `artifacts/salahmate-1.0.0-debug.apk` with its SHA-256 in
`artifacts/SHA256SUMS.txt`; regenerate both after a new build.

`local.properties` must point at the SDK:

```properties
sdk.dir=/opt/android-sdk
```

## Memory settings

The build environment has 2 GB of RAM and no swap by default. `gradle.properties` is tuned for that
and **should not be raised** without more memory:

```properties
org.gradle.jvmargs=-Xmx1280m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.parallel=false
kotlin.daemon.jvmargs=-Xmx1024m
```

If the Gradle daemon reports "disappeared unexpectedly" on a small machine, it was killed by the
kernel for running out of memory. Adding swap fixes it:

```bash
sudo fallocate -l 6G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

## Release build

```bash
export SALAHMATE_KEYSTORE=keystore/salahmate-release.jks
export SALAHMATE_KEYSTORE_PASSWORD=...
export SALAHMATE_KEY_ALIAS=salahmate
export SALAHMATE_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

`app/build.gradle.kts` only applies the release signing config when the keystore file actually
exists, so a missing keystore cannot silently produce an unsigned artifact. Minification and resource
shrinking are enabled for release; see `app/proguard-rules.pro`.

## Known environment issues

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Gradle daemon disappeared unexpectedly` | OOM on a 2 GB machine | add swap, keep `-Xmx1280m` |
| Compose compiler errors with a newer BOM | AGP/Compose mismatch | stay on Compose BOM 2025.12.01 with AGP 8.13.2 |
| `Unresolved reference: first` on a Flow | `kotlinx.coroutines.flow.first(flow)` is not available in this version | call `flow.first()` with `import kotlinx.coroutines.flow.first` |
| Missing `local.properties` | SDK location unknown | create it with `sdk.dir=...` |
| Room schema errors | KSP generated schema out of date | `./gradlew :app:kspDebugKotlin` (schema written to `app/schemas`) |

## Rebuilding the toolchain from scratch

`/opt/jdk` and `/opt/android-sdk` live outside the workspace snapshot, so they disappear when the
sandbox is reset. What is needed to build again:

```bash
# JDK 17
sudo mkdir -p /opt/jdk && sudo chown "$USER" /opt/jdk
curl -sSL -o /tmp/jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xzf /tmp/jdk17.tar.gz -C /opt/jdk --strip-components=1

# Android SDK (build-tools 35 + platform 36 are what this project uses)
sudo mkdir -p /opt/android-sdk && sudo chown "$USER" /opt/android-sdk
curl -sSL -o /tmp/cmdline.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
mkdir -p /opt/android-sdk/cmdline-tools && unzip -q /tmp/cmdline.zip -d /opt/android-sdk/cmdline-tools
mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;35.0.0"

# Swap (2 GB RAM machine) and an empty Gradle home
sudo fallocate -l 6G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
export GRADLE_USER_HOME=/opt/gradle-home
```

`gradlew` may lose its executable bit after a snapshot restore; `chmod +x gradlew` fixes it
(`Permission denied` on `./gradlew` is that and nothing else). First build re-downloads dependencies
and takes several minutes; later builds are incremental.
