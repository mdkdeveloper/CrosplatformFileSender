package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry

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

internal data class RemoteFileCatalogRequest(
    val keywordFingerprint: String,
    val path: String?,
)

internal sealed interface RemoteFileCatalogResponse {
    data class Success(
        val entries: List<FileEntry>,
    ) : RemoteFileCatalogResponse

    data class Failure(
        val error: RemoteFileCatalogError,
    ) : RemoteFileCatalogResponse
}
