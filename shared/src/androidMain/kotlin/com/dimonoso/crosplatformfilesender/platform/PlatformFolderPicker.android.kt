package com.dimonoso.crosplatformfilesender.platform

import android.app.Activity
import android.content.Intent
import android.net.Uri

private const val FolderPickerRequestCode = 47778

fun bindAndroidFolderPickerActivity(activity: Activity) {
    AndroidFolderPickerBridge.activity = activity
}

fun handleAndroidFolderPickerResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
): Boolean = AndroidFolderPickerBridge.handleResult(requestCode, resultCode, data)

actual fun createPlatformFolderPicker(): PlatformFolderPicker = AndroidPlatformFolderPicker()

private class AndroidPlatformFolderPicker : PlatformFolderPicker {
    override val isAvailable: Boolean
        get() = AndroidFolderPickerBridge.activity != null

    override fun pickFolder(onPicked: (PickedFolder) -> Unit) {
        AndroidFolderPickerBridge.pickFolder(onPicked)
    }
}

private object AndroidFolderPickerBridge {
    var activity: Activity? = null
    private var pendingCallback: ((PickedFolder) -> Unit)? = null

    fun pickFolder(onPicked: (PickedFolder) -> Unit) {
        val currentActivity = activity ?: return
        pendingCallback = onPicked

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }

        currentActivity.startActivityForResult(intent, FolderPickerRequestCode)
    }

    fun handleResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != FolderPickerRequestCode) return false

        val callback = pendingCallback
        pendingCallback = null

        val uri = data?.data
        if (resultCode == Activity.RESULT_OK && uri != null && callback != null) {
            persistReadPermission(uri, data, activity)
            callback(
                PickedFolder(
                    path = uri.toString(),
                    displayName = uri.lastPathSegment?.substringAfterLast(':')?.ifBlank { uri.toString() }
                        ?: uri.toString(),
                ),
            )
        }

        return true
    }

    private fun persistReadPermission(
        uri: Uri,
        data: Intent,
        activity: Activity?,
    ) {
        val flags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        runCatching {
            activity?.contentResolver?.takePersistableUriPermission(uri, flags)
        }
    }
}
