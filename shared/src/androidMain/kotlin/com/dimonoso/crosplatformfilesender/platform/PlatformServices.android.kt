package com.dimonoso.crosplatformfilesender.platform

import android.os.Build
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType

actual fun createPlatformServices(): PlatformServices =
    PlatformServices(
        deviceInfo = PlatformDeviceInfo(
            id = "android-${Build.MANUFACTURER}-${Build.MODEL}",
            displayName = Build.MODEL.ifBlank { "Android device" },
            platformName = "Android ${Build.VERSION.SDK_INT}",
            family = PlatformFamily.Android,
        ),
        fileSystem = AndroidPlatformFileSystem(),
        networkPermissions = AndroidNetworkPermissionGateway(),
    )

private class AndroidPlatformFileSystem : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.ScopedStorage

    override fun roots(): List<FileEntry> =
        listOf(
            FileEntry(
                path = "content://system-file-picker",
                name = "System file picker",
                type = FileEntryType.Directory,
                isBrowseable = false,
            ),
            FileEntry(
                path = "app://sandbox",
                name = "App sandbox",
                type = FileEntryType.Directory,
            ),
        )

    override fun list(path: String): List<FileEntry> = emptyList()
}

private class AndroidNetworkPermissionGateway : NetworkPermissionGateway {
    override fun currentState(): NetworkPermissionState =
        NetworkPermissionState(
            supportsUdpDiscovery = true,
            requiresRuntimeApproval = false,
            statusLabel = "Android LAN access scaffolded",
        )
}
