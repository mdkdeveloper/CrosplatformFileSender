package com.dimonoso.crosplatformfilesender

import android.content.ClipData
import androidx.compose.ui.draganddrop.DragAndDropTransferData

private const val FilePaneDragTokenLabel = "Crossplatform File Sender file pane transfer"
private const val FilePaneDragToken = "crosplatform-file-pane-transfer"

internal actual fun createFilePaneDragTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        clipData = ClipData.newPlainText(FilePaneDragTokenLabel, FilePaneDragToken),
        localState = FilePaneDragToken,
    )
