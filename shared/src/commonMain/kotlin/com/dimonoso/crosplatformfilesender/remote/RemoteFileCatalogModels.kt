package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteBatchResult

enum class RemoteFileCatalogErrorCode {
    Unauthorized,
    OutsideWhitelist,
    BadRequest,
    Network,
    ServerUnavailable,
}

data class RemoteFileCatalogError(
    val code: RemoteFileCatalogErrorCode,
    val message: String,
)

sealed interface RemoteFileCatalogResult {
    data class Success(
        val entries: List<FileEntry>,
    ) : RemoteFileCatalogResult

    data class Failure(
        val error: RemoteFileCatalogError,
    ) : RemoteFileCatalogResult
}

sealed interface RemoteFileDeleteResult {
    data class Completed(
        val result: FileDeleteBatchResult,
    ) : RemoteFileDeleteResult

    data class Failure(
        val error: RemoteFileCatalogError,
    ) : RemoteFileDeleteResult
}

data class PendingRemoteDeletePrompt(
    val id: String,
    val paths: List<String>,
    val canMoveAllToTrash: Boolean,
)

enum class RemoteDeletePromptDecision {
    Trash,
    Permanent,
    Cancel,
}

internal enum class RemoteFileCatalogOperation {
    Browse,
    Delete,
}

internal data class RemoteFileCatalogRequest(
    val keywordFingerprint: String,
    val path: String? = null,
    val operation: RemoteFileCatalogOperation = RemoteFileCatalogOperation.Browse,
    val deletePaths: List<String> = emptyList(),
)

internal sealed interface RemoteFileCatalogResponse {
    data class Success(
        val entries: List<FileEntry>,
    ) : RemoteFileCatalogResponse

    data class DeleteCompleted(
        val result: FileDeleteBatchResult,
    ) : RemoteFileCatalogResponse

    data class Failure(
        val error: RemoteFileCatalogError,
    ) : RemoteFileCatalogResponse
}
