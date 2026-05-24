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

internal const val LocalWhitelistRootPath = "cfs://local-whitelist"

internal fun isLocalWhitelistRootPath(path: String): Boolean =
    path == LocalWhitelistRootPath

data class WhitelistFolder(
    val id: String,
    val displayName: String,
    val path: String,
    val enabled: Boolean = true,
    val deletePolicyOverride: WhitelistDeletePolicyOverride = WhitelistDeletePolicyOverride.UseDefault,
)

enum class RemoteDeletePolicy(
    val configValue: String,
) {
    DoNothing("none"),
    Ask("ask"),
    Trash("trash"),
    Permanent("permanent");

    companion object {
        fun fromConfigValue(value: String?): RemoteDeletePolicy =
            entries.firstOrNull { it.configValue == value } ?: DoNothing
    }
}

enum class WhitelistDeletePolicyOverride(
    val configValue: String,
) {
    UseDefault("default"),
    DoNothing("none"),
    Ask("ask"),
    Trash("trash"),
    Permanent("permanent");

    fun explicitPolicy(): RemoteDeletePolicy? =
        when (this) {
            UseDefault -> null
            DoNothing -> RemoteDeletePolicy.DoNothing
            Ask -> RemoteDeletePolicy.Ask
            Trash -> RemoteDeletePolicy.Trash
            Permanent -> RemoteDeletePolicy.Permanent
        }

    companion object {
        fun fromConfigValue(value: String?): WhitelistDeletePolicyOverride =
            entries.firstOrNull { it.configValue == value } ?: UseDefault
    }
}

enum class FileDeleteMode {
    Trash,
    Permanent,
}

enum class FileDeleteStatus {
    Deleted,
    Refused,
    Failed,
}

data class FileDeleteResult(
    val path: String,
    val status: FileDeleteStatus,
    val message: String = "",
)

data class FileDeleteBatchResult(
    val results: List<FileDeleteResult>,
) {
    val hasDeletedEntries: Boolean
        get() = results.any { it.status == FileDeleteStatus.Deleted }

    val firstProblem: FileDeleteResult?
        get() = results.firstOrNull { it.status != FileDeleteStatus.Deleted }
}

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

    fun isWhitelistFolderPathAvailable(folderId: String): Boolean

    fun setWhitelistDeletePolicyOverride(folderId: String, deletePolicyOverride: WhitelistDeletePolicyOverride)

    fun browseWhitelisted(path: String? = null): List<FileEntry>

    fun browseWhitelistedResult(path: String? = null): WhitelistedBrowseResult

    fun browseRemote(device: DiscoveredDevice, path: String? = null): List<FileEntry>

    fun canMoveToTrash(path: String): Boolean

    fun deleteLocalToTrash(paths: List<String>): FileDeleteBatchResult

    fun effectiveRemoteDeletePolicy(path: String, defaultPolicy: RemoteDeletePolicy): RemoteDeletePolicy?

    fun deleteWhitelisted(paths: List<String>, mode: FileDeleteMode): FileDeleteBatchResult
}

class InMemoryFileSystemService(
    private val platformFileSystem: PlatformFileSystem,
    initialWhitelist: List<WhitelistFolder> = emptyList(),
    private val onWhitelistChanged: (List<WhitelistFolder>) -> Unit = {},
) : FileSystemService {
    private val _whitelist = kotlinx.coroutines.flow.MutableStateFlow(initialWhitelist.disableUnavailableFolders())

    override val whitelist: StateFlow<List<WhitelistFolder>> = _whitelist

    init {
        if (_whitelist.value != initialWhitelist) {
            onWhitelistChanged(_whitelist.value)
        }
    }

    override fun localRoots(): List<FileEntry> =
        if (localWhitelistEntries().isEmpty()) {
            platformFileSystem.roots()
        } else {
            platformFileSystem.roots() + FileEntry(
                path = LocalWhitelistRootPath,
                name = "Whitelist",
                type = FileEntryType.Directory,
            )
        }

    override fun browseLocal(path: String): List<FileEntry> =
        if (isLocalWhitelistRootPath(path)) {
            localWhitelistEntries()
        } else {
            platformFileSystem.list(path)
        }

    override fun addWhitelistFolder(folder: WhitelistFolder) {
        val existing = _whitelist.value.filterNot { it.id == folder.id || samePath(it.path, folder.path) }
        replaceWhitelist(existing + folder.withUnavailablePathDisabled())
    }

    override fun removeWhitelistFolder(folderId: String) {
        replaceWhitelist(_whitelist.value.filterNot { it.id == folderId })
    }

    override fun setWhitelistEnabled(folderId: String, enabled: Boolean) {
        replaceWhitelist(_whitelist.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(enabled = enabled && folder.isPathAvailable())
            } else {
                folder
            }
        })
    }

    override fun isWhitelistFolderPathAvailable(folderId: String): Boolean =
        _whitelist.value.firstOrNull { it.id == folderId }?.isPathAvailable() == true

    override fun setWhitelistDeletePolicyOverride(folderId: String, deletePolicyOverride: WhitelistDeletePolicyOverride) {
        replaceWhitelist(_whitelist.value.map { folder ->
            if (folder.id == folderId) folder.copy(deletePolicyOverride = deletePolicyOverride) else folder
        })
    }

    override fun browseWhitelisted(path: String?): List<FileEntry> =
        when (val result = browseWhitelistedResult(path)) {
            is WhitelistedBrowseResult.Success -> result.entries
            is WhitelistedBrowseResult.Failure -> emptyList()
        }

    override fun browseWhitelistedResult(path: String?): WhitelistedBrowseResult {
        val enabledFolders = enabledAvailableWhitelistFolders()
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

    override fun canMoveToTrash(path: String): Boolean =
        !isProtectedRoot(path) && !isLocalFileSystemRoot(path) && platformFileSystem.canMoveToTrash(path)

    override fun deleteLocalToTrash(paths: List<String>): FileDeleteBatchResult =
        FileDeleteBatchResult(paths.distinct().map(::moveLocalPathToTrash))

    override fun effectiveRemoteDeletePolicy(path: String, defaultPolicy: RemoteDeletePolicy): RemoteDeletePolicy? {
        val containingFolders = enabledAvailableWhitelistFolders()
            .filter { folder -> isInside(path, folder.path) }
            .sortedByDescending { folder -> normalizePath(folder.path).length }
        if (containingFolders.isEmpty()) return null

        return containingFolders
            .firstNotNullOfOrNull { folder -> folder.deletePolicyOverride.explicitPolicy() }
            ?: defaultPolicy
    }

    override fun deleteWhitelisted(paths: List<String>, mode: FileDeleteMode): FileDeleteBatchResult =
        FileDeleteBatchResult(paths.distinct().map { path -> deleteWhitelistedPath(path, mode) })

    private fun moveLocalPathToTrash(path: String): FileDeleteResult {
        localDeleteRefusal(path)?.let { message -> return path.refused(message) }
        if (!platformFileSystem.canMoveToTrash(path)) {
            return path.refused("The selected item cannot be moved to trash.")
        }
        return if (platformFileSystem.moveToTrash(path)) {
            path.deleted()
        } else {
            path.failed("Could not move the selected item to trash.")
        }
    }

    private fun deleteWhitelistedPath(path: String, mode: FileDeleteMode): FileDeleteResult {
        remoteDeleteRefusal(path)?.let { message -> return path.refused(message) }
        return when (mode) {
            FileDeleteMode.Trash -> {
                if (!platformFileSystem.canMoveToTrash(path)) {
                    path.refused("The selected item cannot be moved to trash.")
                } else if (platformFileSystem.moveToTrash(path)) {
                    path.deleted()
                } else {
                    path.failed("Could not move the selected item to trash.")
                }
            }
            FileDeleteMode.Permanent -> {
                if (platformFileSystem.delete(path, recursive = true)) {
                    path.deleted()
                } else {
                    path.failed("Could not delete the selected item.")
                }
            }
        }
    }

    private fun localDeleteRefusal(path: String): String? =
        when {
            isProtectedRoot(path) -> "Whitelist folders cannot be deleted."
            isLocalFileSystemRoot(path) -> "Filesystem roots cannot be deleted."
            platformFileSystem.metadata(path) == null -> "The selected item was not found."
            else -> null
        }

    private fun remoteDeleteRefusal(path: String): String? =
        when {
            enabledAvailableWhitelistFolders().none { folder -> isInside(path, folder.path) } ->
                "The selected item is outside the whitelist."
            isProtectedRoot(path) -> "Whitelist folders cannot be deleted."
            platformFileSystem.metadata(path) == null -> "The selected item was not found."
            else -> null
        }

    private fun isProtectedRoot(path: String): Boolean =
        _whitelist.value.any { folder -> folder.path.isNotBlank() && samePath(path, folder.path) }

    private fun isLocalFileSystemRoot(path: String): Boolean =
        isLocalWhitelistRootPath(path) ||
            platformFileSystem.roots().any { root -> samePath(path, root.path) } ||
            platformFileSystem.metadata(path)?.type == FileEntryType.Drive

    private fun samePath(left: String, right: String): Boolean =
        if (left.isBlank() || right.isBlank()) {
            left.isBlank() && right.isBlank()
        } else {
            normalizePath(left).equals(normalizePath(right), ignoreCase = true)
        }

    private fun isInside(path: String, root: String): Boolean {
        if (root.isBlank()) return false
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
        val normalized = folders.disableUnavailableFolders()
        _whitelist.value = normalized
        onWhitelistChanged(normalized)
    }

    private fun List<WhitelistFolder>.disableUnavailableFolders(): List<WhitelistFolder> =
        map { folder -> folder.withUnavailablePathDisabled() }

    private fun WhitelistFolder.withUnavailablePathDisabled(): WhitelistFolder =
        if (enabled && !isPathAvailable()) copy(enabled = false) else this

    private fun enabledAvailableWhitelistFolders(): List<WhitelistFolder> =
        _whitelist.value.filter { folder -> folder.enabled && folder.isPathAvailable() }

    private fun localWhitelistEntries(): List<FileEntry> =
        _whitelist.value.mapNotNull { folder -> folder.toLocalWhitelistEntry() }

    private fun WhitelistFolder.toLocalWhitelistEntry(): FileEntry? {
        val metadata = pathMetadata() ?: return null
        return FileEntry(
            path = path,
            name = displayName.ifBlank { metadata.name },
            type = metadata.type,
            sizeBytes = metadata.sizeBytes,
            isBrowseable = metadata.isBrowseable,
        )
    }

    private fun WhitelistFolder.isPathAvailable(): Boolean =
        pathMetadata() != null

    private fun WhitelistFolder.pathMetadata(): FileEntry? =
        path.takeIf { it.isNotBlank() }?.let(platformFileSystem::metadata)
}

private fun String.deleted(): FileDeleteResult =
    FileDeleteResult(path = this, status = FileDeleteStatus.Deleted)

private fun String.refused(message: String): FileDeleteResult =
    FileDeleteResult(path = this, status = FileDeleteStatus.Refused, message = message)

private fun String.failed(message: String): FileDeleteResult =
    FileDeleteResult(path = this, status = FileDeleteStatus.Failed, message = message)
