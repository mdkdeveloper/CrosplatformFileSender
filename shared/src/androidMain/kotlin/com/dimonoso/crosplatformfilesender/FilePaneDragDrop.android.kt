package com.dimonoso.crosplatformfilesender

import android.content.ClipData
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem

private const val FilePaneDragTokenLabel = "Local File Sender file pane transfer"
private const val FilePaneDragToken = "crosplatform-file-pane-transfer"

internal actual fun createFilePaneDragTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        clipData = ClipData.newPlainText(FilePaneDragTokenLabel, FilePaneDragToken),
        localState = FilePaneDragToken,
    )

internal actual fun isExternalFilePaneDropEvent(event: DragAndDropEvent): Boolean = false

internal actual fun externalFilePaneDropEntries(
    event: DragAndDropEvent,
    fileSystem: PlatformFileSystem,
): List<FileEntry> = emptyList()
