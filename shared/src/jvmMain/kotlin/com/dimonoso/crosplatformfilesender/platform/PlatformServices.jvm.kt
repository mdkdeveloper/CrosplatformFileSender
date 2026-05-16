package com.dimonoso.crosplatformfilesender.platform

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import java.io.File
import java.net.InetAddress

actual fun createPlatformServices(): PlatformServices {
    val userName = System.getProperty("user.name", "user")
    val hostName = runCatching { InetAddress.getLocalHost().hostName }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "desktop"

    return PlatformServices(
        deviceInfo = PlatformDeviceInfo(
            id = "desktop-$hostName-$userName",
            displayName = "$userName@$hostName",
            platformName = "${System.getProperty("os.name", "Desktop")} ${System.getProperty("os.version", "")}".trim(),
            family = PlatformFamily.DesktopJvm,
        ),
        fileSystem = JvmPlatformFileSystem(),
        networkPermissions = JvmNetworkPermissionGateway(),
    )
}

private class JvmPlatformFileSystem : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    override fun roots(): List<FileEntry> =
        File.listRoots().map { root ->
            FileEntry(
                path = root.absolutePath,
                name = root.absolutePath.ifBlank { root.path },
                type = FileEntryType.Drive,
            )
        }

    override fun list(path: String): List<FileEntry> =
        runCatching {
            File(path)
                .listFiles()
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?.map { file ->
                    FileEntry(
                        path = file.absolutePath,
                        name = file.name.ifBlank { file.absolutePath },
                        type = when {
                            file.isDirectory -> FileEntryType.Directory
                            file.isFile -> FileEntryType.File
                            else -> FileEntryType.Unknown
                        },
                        sizeBytes = if (file.isFile) file.length() else null,
                        isBrowseable = file.isDirectory,
                    )
                }
                .orEmpty()
        }.getOrElse {
            emptyList()
        }
}

private class JvmNetworkPermissionGateway : NetworkPermissionGateway {
    override fun currentState(): NetworkPermissionState =
        NetworkPermissionState(
            supportsUdpDiscovery = true,
            requiresRuntimeApproval = false,
            statusLabel = "Desktop LAN access available",
        )
}
