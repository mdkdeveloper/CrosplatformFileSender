package com.dimonoso.crosplatformfilesender.platform

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import kotlinx.coroutines.flow.StateFlow

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

data class StorageAccessState(
    val requiresRuntimeApproval: Boolean,
    val isGranted: Boolean,
    val statusLabel: String,
)

interface PlatformFileSystem {
    val accessPolicy: FileSystemAccessPolicy

    val supportsTrash: Boolean
        get() = false

    fun roots(): List<FileEntry>

    fun list(path: String): List<FileEntry>

    fun metadata(path: String): FileEntry? = null

    fun exists(path: String): Boolean = metadata(path) != null

    fun createDirectories(path: String): Boolean = false

    fun childPath(parentPath: String, childName: String): String =
        parentPath.trimEnd('/', '\\') + "/" + childName

    fun parentPath(path: String): String? =
        path.replace('\\', '/').substringBeforeLast('/', missingDelimiterValue = "").ifBlank { null }

    fun tempPathFor(targetPath: String): String = "$targetPath.cfs-part"

    fun openRead(path: String): PlatformReadStream =
        error("Read streams are not available for this platform filesystem.")

    fun openWrite(path: String): PlatformWriteStream =
        error("Write streams are not available for this platform filesystem.")

    fun move(sourcePath: String, targetPath: String, replace: Boolean = true): Boolean = false

    fun canMoveToTrash(path: String): Boolean = false

    fun moveToTrash(path: String): Boolean = false

    fun delete(path: String, recursive: Boolean = false): Boolean = false

    fun cacheDirectoryPath(): String = "."
}

interface PlatformReadStream : AutoCloseable {
    fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Int
}

interface PlatformWriteStream : AutoCloseable {
    fun write(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size)
}

fun PlatformFileSystem.isDirectory(path: String): Boolean =
    metadata(path)?.type == FileEntryType.Directory

interface NetworkPermissionGateway {
    fun currentState(): NetworkPermissionState
}

interface StorageAccessGateway {
    val state: StateFlow<StorageAccessState>

    fun refresh()

    fun requestAccess()
}

data class PlatformServices(
    val deviceInfo: PlatformDeviceInfo,
    val fileSystem: PlatformFileSystem,
    val networkPermissions: NetworkPermissionGateway,
    val storageAccess: StorageAccessGateway,
)

expect fun createPlatformServices(): PlatformServices
