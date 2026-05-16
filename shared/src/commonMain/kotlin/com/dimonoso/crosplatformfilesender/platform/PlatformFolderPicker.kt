package com.dimonoso.crosplatformfilesender.platform

data class PickedFolder(
    val path: String,
    val displayName: String,
)

interface PlatformFolderPicker {
    val isAvailable: Boolean

    fun pickFolder(onPicked: (PickedFolder) -> Unit)
}

expect fun createPlatformFolderPicker(): PlatformFolderPicker
