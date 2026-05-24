package com.dimonoso.crosplatformfilesender

import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.isLocalWhitelistRootPath
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import com.dimonoso.crosplatformfilesender.transfer.TransferItem

internal const val ParentEntryRowId = "__parent__"

internal enum class FilePaneSide {
    Local,
    Remote,
}

internal data class FilePaneDragPayload(
    val source: FilePaneSide,
    val entries: List<FileEntry>,
)

internal sealed interface FilePaneDropDecision {
    data object Ignore : FilePaneDropDecision

    data object InvalidDestination : FilePaneDropDecision

    data class Upload(
        val items: List<TransferItem>,
        val destinationDirectoryPath: String,
    ) : FilePaneDropDecision

    data class Download(
        val items: List<TransferItem>,
        val destinationDirectoryPath: String,
    ) : FilePaneDropDecision

    data class ConfirmDirectoryDownload(
        val items: List<TransferItem>,
        val destinationDirectoryPath: String,
    ) : FilePaneDropDecision
}

internal fun filePaneDragEntries(
    entries: List<FileEntry>,
    selectedRowIds: Set<String>,
    draggedRowId: String,
): List<FileEntry> {
    val draggableEntries = entries.filterNot { entry -> isLocalWhitelistRootPath(entry.path) }
    val draggedEntry = draggableEntries.firstOrNull { entry -> entry.path == draggedRowId } ?: return emptyList()
    if (draggedEntry.path !in selectedRowIds) return listOf(draggedEntry)

    return draggableEntries.filter { entry -> entry.path in selectedRowIds }
}

internal fun FilePaneDragPayload.canDropOn(target: FilePaneSide): Boolean =
    entries.isNotEmpty() && source != target

internal fun filePaneDropDecision(
    payload: FilePaneDragPayload,
    target: FilePaneSide,
    destinationDirectoryPath: String?,
): FilePaneDropDecision {
    if (!payload.canDropOn(target)) return FilePaneDropDecision.Ignore

    val destination = destinationDirectoryPath?.takeIf { path -> path.isNotBlank() }
        ?: return FilePaneDropDecision.InvalidDestination
    if (isLocalWhitelistRootPath(destination)) return FilePaneDropDecision.InvalidDestination

    val items = payload.entries.map(FileEntry::toTransferItem)

    return when (payload.source to target) {
        FilePaneSide.Local to FilePaneSide.Remote -> FilePaneDropDecision.Upload(items, destination)
        FilePaneSide.Remote to FilePaneSide.Local -> {
            if (items.any { item -> item.isDirectory }) {
                FilePaneDropDecision.ConfirmDirectoryDownload(items, destination)
            } else {
                FilePaneDropDecision.Download(items, destination)
            }
        }
        else -> FilePaneDropDecision.Ignore
    }
}

internal fun FileEntry.toTransferItem(): TransferItem =
    TransferItem(
        path = path,
        displayName = name,
        isDirectory = type == FileEntryType.Directory || type == FileEntryType.Drive,
        sizeBytes = sizeBytes,
    )

internal expect fun createFilePaneDragTransferData(): DragAndDropTransferData

internal expect fun isExternalFilePaneDropEvent(event: DragAndDropEvent): Boolean

internal expect fun externalFilePaneDropEntries(
    event: DragAndDropEvent,
    fileSystem: PlatformFileSystem,
): List<FileEntry>
