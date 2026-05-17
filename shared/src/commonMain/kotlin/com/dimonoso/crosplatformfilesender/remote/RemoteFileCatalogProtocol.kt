package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType

private const val Header = "CFS_FILE_CATALOG_V1"
private const val RequestType = "request"
private const val ResponseType = "response"
private const val EmptyPath = "-"

internal object RemoteFileCatalogProtocol {
    fun encodeRequest(request: RemoteFileCatalogRequest): String =
        buildString {
            appendLine(Header)
            appendLine("type=$RequestType")
            appendLine("keywordFingerprint=${request.keywordFingerprint.escapeWireValue()}")
            appendLine("path=${(request.path ?: EmptyPath).escapeWireValue()}")
            appendLine()
        }

    fun decodeRequest(payload: String): RemoteFileCatalogRequest? {
        val fields = payload.decodeFields(expectedType = RequestType) ?: return null
        val encodedPath = fields["path"] ?: return null
        return RemoteFileCatalogRequest(
            keywordFingerprint = fields["keywordFingerprint"]?.takeIf { it.isNotBlank() } ?: return null,
            path = encodedPath.takeUnless { it == EmptyPath },
        )
    }

    fun encodeResponse(response: RemoteFileCatalogResponse): String =
        buildString {
            appendLine(Header)
            appendLine("type=$ResponseType")
            when (response) {
                is RemoteFileCatalogResponse.Success -> {
                    appendLine("status=ok")
                    appendLine("count=${response.entries.size}")
                    response.entries.forEachIndexed { index, entry ->
                        val prefix = "entry.$index"
                        appendLine("$prefix.path=${entry.path.escapeWireValue()}")
                        appendLine("$prefix.name=${entry.name.escapeWireValue()}")
                        appendLine("$prefix.type=${entry.type.name}")
                        appendLine("$prefix.size=${entry.sizeBytes?.toString().orEmpty().escapeWireValue()}")
                        appendLine("$prefix.browseable=${entry.isBrowseable}")
                    }
                }
                is RemoteFileCatalogResponse.Failure -> {
                    appendLine("status=error")
                    appendLine("code=${response.error.code.name}")
                    appendLine("message=${response.error.message.escapeWireValue()}")
                }
            }
            appendLine()
        }

    fun decodeResponse(payload: String): RemoteFileCatalogResponse? {
        val fields = payload.decodeFields(expectedType = ResponseType) ?: return null
        return when (fields["status"]) {
            "ok" -> {
                val count = fields["count"]?.toIntOrNull()?.coerceAtLeast(0) ?: return null
                val entries = (0 until count).mapNotNull { index ->
                    val prefix = "entry.$index"
                    val path = fields["$prefix.path"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    FileEntry(
                        path = path,
                        name = fields["$prefix.name"]?.takeIf { it.isNotBlank() } ?: path,
                        type = fields["$prefix.type"]?.let(::decodeFileEntryType) ?: FileEntryType.Unknown,
                        sizeBytes = fields["$prefix.size"]?.toLongOrNull(),
                        isBrowseable = fields["$prefix.browseable"]?.toBooleanStrictOrNull() ?: false,
                    )
                }
                RemoteFileCatalogResponse.Success(entries)
            }
            "error" -> {
                val code = fields["code"]?.let(::decodeErrorCode) ?: RemoteFileCatalogErrorCode.BadRequest
                RemoteFileCatalogResponse.Failure(
                    RemoteFileCatalogError(
                        code = code,
                        message = fields["message"].orEmpty(),
                    ),
                )
            }
            else -> null
        }
    }

    private fun String.decodeFields(expectedType: String): Map<String, String>? {
        val lines = lineSequence().toList()
        if (lines.firstOrNull() != Header) return null

        val fields = lines
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

        return fields.takeIf { it["type"] == expectedType }
    }

    private fun decodeFileEntryType(value: String): FileEntryType? =
        FileEntryType.entries.firstOrNull { it.name == value }

    private fun decodeErrorCode(value: String): RemoteFileCatalogErrorCode? =
        RemoteFileCatalogErrorCode.entries.firstOrNull { it.name == value }
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
