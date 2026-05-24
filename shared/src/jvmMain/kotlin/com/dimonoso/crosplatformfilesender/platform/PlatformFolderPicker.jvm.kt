package com.dimonoso.crosplatformfilesender.platform

import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual fun createPlatformFolderPicker(): PlatformFolderPicker = JvmPlatformFolderPicker()

private class JvmPlatformFolderPicker : PlatformFolderPicker {
    override val isAvailable: Boolean = true

    override fun pickFolder(onPicked: (PickedFolder) -> Unit) {
        val pick = {
            val chooser = NavigatingFolderChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Select folder"
                isAcceptAllFileFilterUsed = false
            }

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.toPickedFolder()?.let(onPicked)
            }
        }

        if (SwingUtilities.isEventDispatchThread()) {
            pick()
        } else {
            SwingUtilities.invokeLater(pick)
        }
    }

    private fun File.toPickedFolder(): PickedFolder =
        PickedFolder(
            path = absolutePath,
            displayName = name.ifBlank { absolutePath },
        )
}

private class NavigatingFolderChooser : JFileChooser() {
    override fun approveSelection() {
        val selected = selectedFile
        if (selected != null && shouldNavigateToTypedDirectory(currentDirectory, selected)) {
            currentDirectory = selected
            selectedFile = null
            return
        }

        super.approveSelection()
    }
}

internal fun shouldNavigateToTypedDirectory(
    currentDirectory: File?,
    selectedFile: File,
): Boolean {
    if (!selectedFile.isAbsolute || !selectedFile.isDirectory) return false

    val current = currentDirectory?.canonicalOrAbsoluteFile() ?: return true
    val selectedParent = selectedFile.parentFile?.canonicalOrAbsoluteFile() ?: return true

    return selectedParent != current
}

private fun File.canonicalOrAbsoluteFile(): File =
    runCatching { canonicalFile }.getOrDefault(absoluteFile)
