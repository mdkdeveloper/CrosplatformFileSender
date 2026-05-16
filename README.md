# Crosplatform File Sender

Kotlin Multiplatform application for sending files between Android and desktop devices.

## Targets

- Android: `androidApp`
- Desktop JVM: `desktopApp`
- Shared Kotlin and Compose UI: `shared`

## Useful commands

- Android debug build: `./gradlew :androidApp:assembleDebug`
- Desktop run: `./gradlew :desktopApp:run`
- Desktop hot reload: `./gradlew :desktopApp:hotRun --auto`
- Shared desktop tests: `./gradlew :shared:jvmTest`
- Android host tests: `./gradlew :shared:testAndroidHostTest`
