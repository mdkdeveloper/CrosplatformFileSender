package com.dimonoso.crosplatformfilesender.settings

import android.content.Context
import java.io.File

fun bindAndroidSettingsContext(context: Context) {
    AndroidSettingsContext.context = context.applicationContext
}

internal actual fun createSettingsStore(): SettingsStore {
    val context = AndroidSettingsContext.context
    return if (context == null) {
        InMemorySettingsStore()
    } else {
        FileSettingsStore(File(context.filesDir, "settings.config"))
    }
}

private object AndroidSettingsContext {
    var context: Context? = null
}

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

private class InMemorySettingsStore : SettingsStore {
    private var contents: String? = null

    override fun readConfig(): String? = contents

    override fun writeConfig(contents: String) {
        this.contents = contents
    }
}
