package com.dimonoso.crosplatformfilesender

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

private const val FilePaneDragToken = "crosplatform-file-pane-transfer"

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun createFilePaneDragTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        transferable = DragAndDropTransferable(StringSelection(FilePaneDragToken)),
        supportedActions = listOf(DragAndDropTransferAction.Copy),
    )

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun isExternalFilePaneDropEvent(event: DragAndDropEvent): Boolean =
    event.externalFilesOrNull() != null

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun externalFilePaneDropEntries(
    event: DragAndDropEvent,
    fileSystem: PlatformFileSystem,
): List<FileEntry> =
    event.externalFilesOrNull()
        ?.readFiles()
        ?.let { uris -> externalFileEntriesFromUris(uris, fileSystem::metadata) }
        .orEmpty()

internal fun externalFileEntriesFromUris(
    uris: List<String>,
    metadata: (String) -> FileEntry?,
): List<FileEntry> =
    uris.mapNotNull { uri ->
        val path = uri.toExternalFilePath() ?: return@mapNotNull null
        metadata(path)?.takeIf(FileEntry::isExternalUploadEntry)
    }

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.externalFilesOrNull(): DragData.FilesList? =
    runCatching { dragData() as? DragData.FilesList }.getOrNull()

private fun String.toExternalFilePath(): String? =
    runCatching {
        val uri = URI(this)
        if (!uri.scheme.equals("file", ignoreCase = true)) {
            null
        } else {
            File(uri).absolutePath
        }
    }.getOrNull()

private fun FileEntry.isExternalUploadEntry(): Boolean =
    type == FileEntryType.File || type == FileEntryType.Directory || type == FileEntryType.Drive
