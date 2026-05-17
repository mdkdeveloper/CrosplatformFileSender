package com.dimonoso.crosplatformfilesender.backup

import android.content.Context
import java.io.File

fun bindAndroidBackupContext(context: Context) {
    AndroidBackupContext.context = context.applicationContext
}

internal actual fun createBackupStore(): BackupStore {
    val context = AndroidBackupContext.context
    return if (context == null) {
        InMemoryBackupStore()
    } else {
        FileBackupStore(File(context.filesDir, "backups.config"))
    }
}

private object AndroidBackupContext {
    var context: Context? = null
}

private class FileBackupStore(
    private val file: File,
) : BackupStore {
    override fun readConfig(): String? =
        file.takeIf { it.exists() && it.isFile }?.readText()

    override fun writeConfig(contents: String) {
        file.parentFile?.mkdirs()
        file.writeText(contents)
    }
}

private class InMemoryBackupStore : BackupStore {
    private var contents: String? = null

    override fun readConfig(): String? = contents

    override fun writeConfig(contents: String) {
        this.contents = contents
    }
}
