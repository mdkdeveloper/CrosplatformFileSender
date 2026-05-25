package com.dimonoso.crosplatformfilesender

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual fun createPendingShareService(): PendingShareService = AndroidPendingShareService

fun handleAndroidShareIntent(context: Context, intent: Intent?) {
    AndroidPendingShareService.acceptIntent(context, intent)
}

private object AndroidPendingShareService : PendingShareService {
    private val mutableFiles = MutableStateFlow<List<FileEntry>>(emptyList())

    override val files: StateFlow<List<FileEntry>> = mutableFiles

    override fun replaceFiles(files: List<FileEntry>) {
        mutableFiles.value = files
    }

    override fun clearFiles() {
        mutableFiles.value = emptyList()
    }

    fun acceptIntent(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val files = intent.streamUris()
            .distinctBy { uri -> uri.toString() }
            .mapIndexed { index, uri ->
                context.takeReadPermissionIfPersistable(intent, uri)
                context.sharedUriFileEntry(uri, index)
            }

        if (files.isNotEmpty()) {
            replaceFiles(files)
        }
    }
}

private fun Intent.streamUris(): List<Uri> =
    buildList {
        streamUriExtra()?.let(::add)
        streamUriListExtra().forEach(::add)
        val data = clipData
        if (data != null) {
            for (index in 0 until data.itemCount) {
                data.getItemAt(index).uri?.let(::add)
            }
        }
    }

@Suppress("DEPRECATION")
private fun Intent.streamUriExtra(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }

@Suppress("DEPRECATION")
private fun Intent.streamUriListExtra(): List<Uri> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }

private fun Context.takeReadPermissionIfPersistable(intent: Intent, uri: Uri) {
    val hasReadPermission = intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
    val isPersistable = intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
    if (!hasReadPermission || !isPersistable) return

    runCatching {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun Context.sharedUriFileEntry(uri: Uri, index: Int): FileEntry {
    var displayName: String? = null
    var sizeBytes: Long? = null
    runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME)
                sizeBytes = cursor.longOrNull(OpenableColumns.SIZE)
            }
        }
    }

    val fallbackName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { value -> value.isNotBlank() }
        ?: "shared-file-${index + 1}"

    return FileEntry(
        path = uri.toString(),
        name = displayName?.takeIf { value -> value.isNotBlank() } ?: fallbackName,
        type = FileEntryType.File,
        sizeBytes = sizeBytes,
        isBrowseable = false,
    )
}

private fun android.database.Cursor.stringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getString(index) else null
}

private fun android.database.Cursor.longOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getLong(index) else null
}
