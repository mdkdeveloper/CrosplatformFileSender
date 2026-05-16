package com.dimonoso.crosplatformfilesender.settings

import java.io.File

internal actual fun createSettingsStore(): SettingsStore =
    FileSettingsStore(File(System.getProperty("user.dir"), "settings.config"))

private class FileSettingsStore(
    private val file: File,
) : SettingsStore {
    override fun readConfig(): String? =
        file.takeIf { it.exists() && it.isFile }?.readText()

    override fun writeConfig(contents: String) {
        file.parentFile?.mkdirs()
        file.writeText(contents)
    }
}
