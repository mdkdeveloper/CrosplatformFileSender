package com.dimonoso.crosplatformfilesender.platform

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
        storageAccess = JvmStorageAccessGateway(),
    )
}

private class JvmPlatformFileSystem : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    override val supportsTrash: Boolean
        get() = runCatching {
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
        }.getOrDefault(false)

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

    override fun metadata(path: String): FileEntry? =
        File(path).takeIf { it.exists() }?.toFileEntry()

    override fun createDirectories(path: String): Boolean =
        File(path).let { file -> file.exists() && file.isDirectory || file.mkdirs() }

    override fun childPath(parentPath: String, childName: String): String =
        File(parentPath, childName).absolutePath

    override fun parentPath(path: String): String? =
        File(path).parentFile?.absolutePath

    override fun openRead(path: String): PlatformReadStream =
        JvmPlatformReadStream(FileInputStream(File(path)))

    override fun openWrite(path: String): PlatformWriteStream {
        File(path).parentFile?.mkdirs()
        return JvmPlatformWriteStream(FileOutputStream(File(path), false))
    }

    override fun move(sourcePath: String, targetPath: String, replace: Boolean): Boolean {
        val source = File(sourcePath)
        val target = File(targetPath)
        target.parentFile?.mkdirs()
        if (replace && target.exists()) {
            target.deleteRecursively()
        }
        return source.renameTo(target) || runCatching {
            source.copyRecursively(target, overwrite = replace)
            source.deleteRecursively()
            true
        }.getOrDefault(false)
    }

    override fun canMoveToTrash(path: String): Boolean =
        supportsTrash && File(path).exists()

    override fun moveToTrash(path: String): Boolean =
        runCatching {
            val file = File(path)
            !file.exists() || canMoveToTrash(path) && Desktop.getDesktop().moveToTrash(file)
        }.getOrDefault(false)

    override fun delete(path: String, recursive: Boolean): Boolean {
        val file = File(path)
        if (!file.exists()) return true
        return if (recursive) file.deleteRecursively() else file.delete()
    }

    override fun cacheDirectoryPath(): String =
        File(System.getProperty("java.io.tmpdir"), "CrosplatformFileSender").absolutePath

    private fun File.toFileEntry(): FileEntry =
        FileEntry(
            path = absolutePath,
            name = name.ifBlank { absolutePath },
            type = when {
                isDirectory -> FileEntryType.Directory
                isFile -> FileEntryType.File
                else -> FileEntryType.Unknown
            },
            sizeBytes = if (isFile) length() else null,
            isBrowseable = isDirectory,
        )
}

private class JvmPlatformReadStream(
    private val input: FileInputStream,
) : PlatformReadStream {
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        input.read(buffer, offset, length)

    override fun close() {
        input.close()
    }
}

private class JvmPlatformWriteStream(
    private val output: FileOutputStream,
) : PlatformWriteStream {
    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        output.write(buffer, offset, length)
    }

    override fun close() {
        output.close()
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

private class JvmStorageAccessGateway : StorageAccessGateway {
    private val grantedState = MutableStateFlow(
        StorageAccessState(
            requiresRuntimeApproval = false,
            isGranted = true,
            statusLabel = "Desktop filesystem access available",
        ),
    )

    override val state: StateFlow<StorageAccessState> = grantedState

    override fun refresh() = Unit

    override fun requestAccess() = Unit
}
