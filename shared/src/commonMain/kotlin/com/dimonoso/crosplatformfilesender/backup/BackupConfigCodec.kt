package com.dimonoso.crosplatformfilesender.backup

import com.dimonoso.crosplatformfilesender.settings.BackupMode

internal object BackupConfigCodec {
    private const val CountKey = "backup.count"

    fun encode(records: List<BackupRecord>): String =
        buildString {
            appendLine("# CrosplatformFileSender backups")
            appendLine("$CountKey=${records.size}")
            records.forEachIndexed { index, record ->
                val prefix = "backup.$index"
                appendLine("$prefix.id=${record.id.escapeConfigValue()}")
                appendLine("$prefix.archivePath=${record.archivePath.escapeConfigValue()}")
                appendLine("$prefix.originalTargetPath=${record.originalTargetPath.escapeConfigValue()}")
                appendLine("$prefix.createdAt=${record.createdAtEpochMillis}")
                appendLine("$prefix.mode=${record.mode.configValue}")
                appendLine("$prefix.sizeBytes=${record.sizeBytes?.toString().orEmpty()}")
                appendLine("$prefix.sourceTaskId=${record.sourceTaskId.orEmpty().escapeConfigValue()}")
            }
        }

    fun decode(contents: String): List<BackupRecord> {
        val properties = contents
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).unescapeConfigValue() ?: return@mapNotNull null
                key to value
            }
            .toMap()
        val count = properties[CountKey]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return (0 until count).mapNotNull { index ->
            val prefix = "backup.$index"
            val id = properties["$prefix.id"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val archivePath = properties["$prefix.archivePath"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val originalTargetPath = properties["$prefix.originalTargetPath"]?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            BackupRecord(
                id = id,
                archivePath = archivePath,
                originalTargetPath = originalTargetPath,
                createdAtEpochMillis = properties["$prefix.createdAt"]?.toLongOrNull() ?: 0L,
                mode = BackupMode.fromConfigValue(properties["$prefix.mode"]),
                sizeBytes = properties["$prefix.sizeBytes"]?.toLongOrNull(),
                sourceTaskId = properties["$prefix.sourceTaskId"]?.takeIf { it.isNotBlank() },
            )
        }
    }
}

private fun String.escapeConfigValue(): String =
    buildString(length) {
        for (char in this@escapeConfigValue) {
            when (char) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                else -> append(char)
            }
        }
    }

private fun String.unescapeConfigValue(): String? =
    buildString(length) {
        var index = 0
        while (index < this@unescapeConfigValue.length) {
            val char = this@unescapeConfigValue[index]
            if (char != '%') {
                append(char)
                index += 1
                continue
            }

            if (index + 2 >= this@unescapeConfigValue.length) return null
            val hex = this@unescapeConfigValue.substring(index + 1, index + 3)
            val code = hex.toIntOrNull(16) ?: return null
            append(code.toChar())
            index += 3
        }
    }
