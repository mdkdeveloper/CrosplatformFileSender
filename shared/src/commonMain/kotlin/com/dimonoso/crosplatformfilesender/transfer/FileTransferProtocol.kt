package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.settings.BackupMode

private const val Header = "CFS_TRANSFER_V1"

enum class FileTransferRequestType {
    Upload,
    Download,
}

data class FileTransferRequest(
    val type: FileTransferRequestType,
    val taskId: String,
    val keywordFingerprint: String,
    val sourcePath: String,
    val destinationDirectoryPath: String,
    val displayName: String,
    val originalDisplayName: String,
    val sizeBytes: Long,
    val payloadKind: TransferPayloadKind,
    val extractArchiveOnReceive: Boolean,
    val backupMode: BackupMode,
    val archiveDirectory: Boolean = true,
)

data class FileTransferResponse(
    val success: Boolean,
    val message: String = "",
    val sizeBytes: Long = 0L,
    val displayName: String = "",
    val extractArchiveOnReceive: Boolean = false,
    val originalDisplayName: String = displayName,
)

internal object FileTransferProtocol {
    fun encodeRequest(request: FileTransferRequest): String =
        encodeFields(
            mapOf(
                "message" to "request",
                "type" to request.type.name,
                "taskId" to request.taskId,
                "keywordFingerprint" to request.keywordFingerprint,
                "sourcePath" to request.sourcePath,
                "destinationDirectoryPath" to request.destinationDirectoryPath,
                "displayName" to request.displayName,
                "originalDisplayName" to request.originalDisplayName,
                "sizeBytes" to request.sizeBytes.toString(),
                "payloadKind" to request.payloadKind.name,
                "extractArchiveOnReceive" to request.extractArchiveOnReceive.toString(),
                "backupMode" to request.backupMode.configValue,
                "archiveDirectory" to request.archiveDirectory.toString(),
            ),
        )

    fun decodeRequest(payload: String): FileTransferRequest? {
        val fields = payload.decodeFields() ?: return null
        if (fields["message"] != "request") return null
        return FileTransferRequest(
            type = fields["type"]?.let { value -> FileTransferRequestType.entries.firstOrNull { it.name == value } }
                ?: return null,
            taskId = fields["taskId"]?.takeIf { it.isNotBlank() } ?: return null,
            keywordFingerprint = fields["keywordFingerprint"]?.takeIf { it.isNotBlank() } ?: return null,
            sourcePath = fields["sourcePath"].orEmpty(),
            destinationDirectoryPath = fields["destinationDirectoryPath"].orEmpty(),
            displayName = fields["displayName"]?.takeIf { it.isNotBlank() } ?: return null,
            originalDisplayName = fields["originalDisplayName"]?.takeIf { it.isNotBlank() }
                ?: fields["displayName"].orEmpty(),
            sizeBytes = fields["sizeBytes"]?.toLongOrNull()?.coerceAtLeast(0L) ?: return null,
            payloadKind = fields["payloadKind"]?.let { value -> TransferPayloadKind.entries.firstOrNull { it.name == value } }
                ?: TransferPayloadKind.File,
            extractArchiveOnReceive = fields["extractArchiveOnReceive"]?.toBooleanStrictOrNull() ?: false,
            backupMode = BackupMode.fromConfigValue(fields["backupMode"]),
            archiveDirectory = fields["archiveDirectory"]?.toBooleanStrictOrNull() ?: true,
        )
    }

    fun encodeResponse(response: FileTransferResponse): String =
        encodeFields(
            mapOf(
                "message" to "response",
                "success" to response.success.toString(),
                "detail" to response.message,
                "sizeBytes" to response.sizeBytes.toString(),
                "displayName" to response.displayName,
                "originalDisplayName" to response.originalDisplayName,
                "extractArchiveOnReceive" to response.extractArchiveOnReceive.toString(),
            ),
        )

    fun decodeResponse(payload: String): FileTransferResponse? {
        val fields = payload.decodeFields() ?: return null
        if (fields["message"] != "response") return null
        return FileTransferResponse(
            success = fields["success"]?.toBooleanStrictOrNull() ?: false,
            message = fields["detail"].orEmpty(),
            sizeBytes = fields["sizeBytes"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            displayName = fields["displayName"].orEmpty(),
            originalDisplayName = fields["originalDisplayName"].orEmpty(),
            extractArchiveOnReceive = fields["extractArchiveOnReceive"]?.toBooleanStrictOrNull() ?: false,
        )
    }

    private fun encodeFields(fields: Map<String, String>): String =
        buildString {
            appendLine(Header)
            fields.forEach { (key, value) -> appendLine("$key=${value.escapeWireValue()}") }
            appendLine()
        }

    private fun String.decodeFields(): Map<String, String>? {
        val lines = lineSequence().toList()
        if (lines.firstOrNull() != Header) return null
        return lines
            .drop(1)
            .takeWhile { it.isNotBlank() }
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null
                val key = line.substring(0, separatorIndex)
                val value = line.substring(separatorIndex + 1).unescapeWireValue() ?: return null
                key to value
            }
            .toMap()
    }
}

private fun String.escapeWireValue(): String =
    buildString(length) {
        for (char in this@escapeWireValue) {
            when (char) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                '=' -> append("%3D")
                else -> append(char)
            }
        }
    }

private fun String.unescapeWireValue(): String? =
    buildString(length) {
        var index = 0
        while (index < this@unescapeWireValue.length) {
            val char = this@unescapeWireValue[index]
            if (char != '%') {
                append(char)
                index += 1
                continue
            }

            if (index + 2 >= this@unescapeWireValue.length) return null
            val hex = this@unescapeWireValue.substring(index + 1, index + 3)
            val code = hex.toIntOrNull(16) ?: return null
            append(code.toChar())
            index += 3
        }
    }
