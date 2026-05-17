package com.dimonoso.crosplatformfilesender.backup

import java.io.File

internal actual fun createBackupStore(): BackupStore =
    FileBackupStore(File(System.getProperty("user.dir"), "backups.config"))

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
