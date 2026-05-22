package com.dimonoso.crosplatformfilesender

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import java.awt.datatransfer.StringSelection

private const val FilePaneDragToken = "crosplatform-file-pane-transfer"

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun createFilePaneDragTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        transferable = DragAndDropTransferable(StringSelection(FilePaneDragToken)),
        supportedActions = listOf(DragAndDropTransferAction.Copy),
    )
