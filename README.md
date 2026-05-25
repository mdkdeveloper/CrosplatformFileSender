# Local File Sender

Local File Sender is a Kotlin Multiplatform app for moving files between Android and desktop devices on the same local network. It uses LAN discovery, a shared keyword, and a simple file browser so you can send or download selected files without a cloud service.

## Supported platforms

- Android app: `androidApp`
- Desktop JVM app: `desktopApp`
- Shared Compose UI and app logic: `shared`

## Main features

- Discover nearby devices over UDP on the local network.
- Optionally start device discovery automatically when the app opens.
- Pair devices by using the same discovery keyword.
- Browse local files and the remote device's shared folders.
- On Android, browse internal shared storage plus mounted SD cards and USB storage from the local file pane when all-files access is granted.
- On Android, receive one or more files from another app's Share action and keep them pending until a destination device and folder are selected.
- Send selected files and folders to another device.
- Download selected files and folders from another device.
- On Android, select files and folders with checkboxes inside opened folders; root folders are opened by tapping and are not selectable.
- Drag files and folders between the local and remote file panes to upload or download them.
- On desktop, drop files and folders from the system file manager into the remote file pane to send them.
- On desktop, edit or paste paths directly in file panes; on Android, tap the displayed path to copy it.
- Track active, paused, completed, failed, and cancelled transfers in a queue.
- Refresh open file panes after successful in-app transfers.
- Limit parallel incoming and outgoing transfers.
- Share only selected folders through the whitelist.
- Open the local Whitelist root to jump quickly to existing whitelist folders, including disabled ones.
- Delete selected local or remote file-browser items with the `Delete` key using local confirmation and remote whitelist policies.
- Archive folders before transfer when needed.
- Keep backup archives for received or replaced files, depending on the selected backup mode.
- Switch between system language, English, and Ukrainian.
- Use a dark theme by default and switch between dark and light themes in Settings.

## Requirements

- Both devices must be connected to the same local network.
- Network discovery and transfer traffic must be allowed by the OS firewall.
- Android builds require Android Studio or an installed Android SDK.
- Desktop builds require a JDK. Native desktop packages require a full JDK with `jpackage` and `jlink`.
- Android devices may need Wi-Fi/multicast permissions for LAN discovery to work reliably.
- Android full local file browsing requires enabling the app's all-files access permission in system settings. The system folder picker remains available for whitelist folders.

## Run and build

Use the Gradle wrapper from the repository root.

### Run the desktop app

```bash
./gradlew :desktopApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :desktopApp:run
```

### Build the Android release APK

```bash
./gradlew -PappVersion=1.0.0 :androidApp:assembleRelease
```

On Windows PowerShell:

```powershell
.\gradlew.bat "-PappVersion=1.0.0" :androidApp:assembleRelease
```

The release APK is written under:

```text
androidApp/build/outputs/apk/release/
```

### Run shared JVM tests

```bash
./gradlew :shared:jvmTest
```

On Windows PowerShell:

```powershell
.\gradlew.bat :shared:jvmTest
```

### Package helpers

The repository also includes helper scripts that build release artifacts and open the output folder when possible. The first argument is the app version; when omitted, it defaults to `1.0.0`.

```bash
./build-android.sh 1.0.0
./build-linux.sh 1.0.0
bash ./build-linux-portable.sh 1.0.0
./build-macos.sh 1.0.0
```

```powershell
.\build-android.bat 1.0.0
.\build-windows.bat 1.0.0
.\build-windows-portable.bat 1.0.0
.\build-linux-wsl.bat 1.0.0
.\build-linux-portable-wsl.bat 1.0.0
```

Useful output locations:

- Android APK: `androidApp/build/outputs/apk/release/`
- Windows MSI: `desktopApp/build/compose/binaries/main-release/msi/`
- Windows portable zip: `desktopApp/build/compose/binaries/main-release/zip/`
- Linux DEB: `desktopApp/build/compose/binaries/main-release/deb/`
- Linux portable tar.gz: `desktopApp/build/compose/binaries/main-release/tar/`
- macOS DMG: `desktopApp/build/compose/binaries/main-release/dmg/`

macOS DMG builds must be run on macOS with a full JDK that includes `jpackage` and `jlink`.
On macOS, check Java with `java -version` and list installed JDKs with `/usr/libexec/java_home -V`.

## Basic usage

1. Run the app on at least two devices on the same LAN.
2. Open Settings on each device and set the same discovery keyword.
3. Start discovery from the Files screen, or enable automatic startup discovery in Settings.
4. Add one or more whitelist folders on the device that should expose files.
5. Select a discovered device.
6. Browse local files and remote files. On Android, tap folders to open them and use checkboxes inside opened folders to select files or folders.
7. Open a destination folder in the receiving file pane. The root view is not a transfer destination.
8. Use Send selected or Download selected, or drag files and folders between the local and remote panes.
9. On Android, share files from another app to Local File Sender, then select the receiving device and open a remote destination folder before tapping Send shared files. Cancel clears the pending shared files.
10. In the desktop app, you can also drop files and folders from the system file manager into the remote pane to send them.
11. On desktop, edit a pane's path field and press Enter, or click elsewhere, to navigate to that path. On Android, tap the displayed path to copy it.
12. Select file-browser items and press `Delete` to request deletion from the active pane.
13. Open the transfer queue to pause, resume, cancel, or check progress.

## Sharing and safety notes

Only whitelisted folders are exposed to other devices. If no whitelist folder is enabled, other devices may discover this device but will not be able to browse useful remote files.

The local file browser shows a Whitelist root when at least one whitelist folder path still exists. Missing or empty whitelist paths are hidden there and cannot be enabled until the path exists again.

The discovery keyword is used to match devices on the LAN. Devices with different keywords ignore each other's discovery messages and remote file requests.

When receiving files, the backup setting controls whether existing files or folders are archived before being replaced. Backup archives can be restored or deleted from Settings.

Local file-browser deletions always ask first and only move items to the system trash when the current platform and path support it. Whitelist folder roots are protected from deletion.

Remote deletion requests are disabled by default. Settings can allow the owner device to ask before deleting, move allowed items to trash, or delete them permanently, and each whitelist folder can override that default. Nested whitelist overrides use the closest enabled whitelist folder that specifies a delete policy.

## Troubleshooting

### Devices do not appear

- Make sure both devices are on the same Wi-Fi or LAN.
- Check that discovery is started on both devices.
- Confirm that both devices use the same discovery keyword.
- Allow the app through Windows Defender Firewall, Linux firewall rules, or any third-party firewall.
- On Android, keep Wi-Fi enabled and allow network permissions when prompted.

### Remote files are empty

- Add a whitelist folder on the remote device.
- Make sure the whitelist folder is enabled.
- On Android, choose a folder that the app can access through the system picker.

### Android local files are empty

- Grant all-files access when the local file pane prompts for it.
- Tap Refresh after attaching an SD card or USB drive.
- The Android local roots are internal storage at `/storage/emulated/0` plus mounted removable storage detected by the system.

### Transfers fail

- Keep both apps open until the transfer finishes.
- Check that the destination folder still exists and is writable.
- Make sure the remote device did not leave the network.
- Try lowering parallel transfer limits in Settings if the network is unstable.

### Desktop package build fails

- Install a full JDK 17 or newer.
- Set `JAVA_HOME` to the JDK directory.
- Confirm that `jpackage` and `jlink` are available in `JAVA_HOME/bin`.

## Project structure

- `shared` contains shared Kotlin Multiplatform logic, Compose UI, discovery, remote catalog, transfer queue, archive, backup, settings, and tests.
- `androidApp` contains the Android entry point, manifest, resources, and Android app packaging.
- `desktopApp` contains the desktop JVM entry point and Compose Desktop packaging configuration.
- `scripts` contains helper scripts for packaging Android, Windows, and Linux builds.
