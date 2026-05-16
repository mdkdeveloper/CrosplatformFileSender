package com.dimonoso.crosplatformfilesender.platform

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry

enum class PlatformFamily {
    Android,
    DesktopJvm,
}

enum class FileSystemAccessPolicy {
    FullFileSystem,
    ScopedStorage,
}

data class PlatformDeviceInfo(
    val id: String,
    val displayName: String,
    val platformName: String,
    val family: PlatformFamily,
)

data class NetworkPermissionState(
    val supportsUdpDiscovery: Boolean,
    val requiresRuntimeApproval: Boolean,
    val statusLabel: String,
)

interface PlatformFileSystem {
    val accessPolicy: FileSystemAccessPolicy

    fun roots(): List<FileEntry>

    fun list(path: String): List<FileEntry>
}

interface NetworkPermissionGateway {
    fun currentState(): NetworkPermissionState
}

data class PlatformServices(
    val deviceInfo: PlatformDeviceInfo,
    val fileSystem: PlatformFileSystem,
    val networkPermissions: NetworkPermissionGateway,
)

expect fun createPlatformServices(): PlatformServices
