package com.dimonoso.crosplatformfilesender.filesystem

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import kotlinx.coroutines.flow.StateFlow

enum class FileEntryType {
    Drive,
    Directory,
    File,
    Unknown,
}

data class FileEntry(
    val path: String,
    val name: String,
    val type: FileEntryType,
    val sizeBytes: Long? = null,
    val isBrowseable: Boolean = type == FileEntryType.Drive || type == FileEntryType.Directory,
)

data class WhitelistFolder(
    val id: String,
    val displayName: String,
    val path: String,
    val enabled: Boolean = true,
)

sealed interface WhitelistedBrowseResult {
    data class Success(
        val entries: List<FileEntry>,
    ) : WhitelistedBrowseResult

    data class Failure(
        val message: String,
    ) : WhitelistedBrowseResult
}

interface FileSystemService {
    val whitelist: StateFlow<List<WhitelistFolder>>

    fun localRoots(): List<FileEntry>

    fun browseLocal(path: String): List<FileEntry>

    fun addWhitelistFolder(folder: WhitelistFolder)

    fun removeWhitelistFolder(folderId: String)

    fun setWhitelistEnabled(folderId: String, enabled: Boolean)

    fun browseWhitelisted(path: String? = null): List<FileEntry>

    fun browseWhitelistedResult(path: String? = null): WhitelistedBrowseResult

    fun browseRemote(device: DiscoveredDevice, path: String? = null): List<FileEntry>
}

class InMemoryFileSystemService(
    private val platformFileSystem: PlatformFileSystem,
    initialWhitelist: List<WhitelistFolder> = emptyList(),
    private val onWhitelistChanged: (List<WhitelistFolder>) -> Unit = {},
) : FileSystemService {
    private val _whitelist = kotlinx.coroutines.flow.MutableStateFlow(initialWhitelist)

    override val whitelist: StateFlow<List<WhitelistFolder>> = _whitelist

    override fun localRoots(): List<FileEntry> = platformFileSystem.roots()

    override fun browseLocal(path: String): List<FileEntry> = platformFileSystem.list(path)

    override fun addWhitelistFolder(folder: WhitelistFolder) {
        val existing = _whitelist.value.filterNot { it.id == folder.id || samePath(it.path, folder.path) }
        replaceWhitelist(existing + folder)
    }

    override fun removeWhitelistFolder(folderId: String) {
        replaceWhitelist(_whitelist.value.filterNot { it.id == folderId })
    }

    override fun setWhitelistEnabled(folderId: String, enabled: Boolean) {
        replaceWhitelist(_whitelist.value.map { folder ->
            if (folder.id == folderId) folder.copy(enabled = enabled) else folder
        })
    }

    override fun browseWhitelisted(path: String?): List<FileEntry> =
        when (val result = browseWhitelistedResult(path)) {
            is WhitelistedBrowseResult.Success -> result.entries
            is WhitelistedBrowseResult.Failure -> emptyList()
        }

    override fun browseWhitelistedResult(path: String?): WhitelistedBrowseResult {
        val enabledFolders = _whitelist.value.filter { it.enabled }
        if (path == null) {
            return WhitelistedBrowseResult.Success(
                enabledFolders.map { folder ->
                    FileEntry(
                        path = folder.path,
                        name = folder.displayName,
                        type = FileEntryType.Directory,
                    )
                },
            )
        }

        if (enabledFolders.none { isInside(path, it.path) }) {
            return WhitelistedBrowseResult.Failure("Requested path is outside the whitelist.")
        }

        return WhitelistedBrowseResult.Success(
            platformFileSystem.list(path)
                .filter { entry -> enabledFolders.any { folder -> isInside(entry.path, folder.path) } },
        )
    }

    override fun browseRemote(device: DiscoveredDevice, path: String?): List<FileEntry> = browseWhitelisted(path)

    private fun samePath(left: String, right: String): Boolean =
        normalizePath(left).equals(normalizePath(right), ignoreCase = true)

    private fun isInside(path: String, root: String): Boolean {
        val normalizedPath = normalizePath(path)
        val normalizedRoot = normalizePath(root)
        return normalizedPath == normalizedRoot || normalizedPath.startsWith("$normalizedRoot/")
    }

    private fun normalizePath(path: String): String {
        val replaced = path.replace('\\', '/').trim()
        val withoutTrailingSlash = replaced.trimEnd('/')
        return withoutTrailingSlash.ifBlank { "/" }
    }

    private fun replaceWhitelist(folders: List<WhitelistFolder>) {
        _whitelist.value = folders
        onWhitelistChanged(folders)
    }
}
