# Crosplatform File Sender

Kotlin Multiplatform application for sending files between devices.

## Targets

- Android: `androidApp`
- iOS: `iosApp`
- Desktop JVM: `desktopApp`
- Shared Kotlin and Compose UI: `shared`

## Useful commands

- Android debug build: `./gradlew :androidApp:assembleDebug`
- Desktop run: `./gradlew :desktopApp:run`
- Desktop hot reload: `./gradlew :desktopApp:hotRun --auto`
- Shared desktop tests: `./gradlew :shared:jvmTest`
- Android host tests: `./gradlew :shared:testAndroidHostTest`
- iOS simulator tests: `./gradlew :shared:iosSimulatorArm64Test`

Open `iosApp` in Xcode to run the iOS target.
