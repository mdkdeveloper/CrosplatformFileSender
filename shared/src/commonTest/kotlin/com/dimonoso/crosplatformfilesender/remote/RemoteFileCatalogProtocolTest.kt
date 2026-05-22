package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteBatchResult
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteResult
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteStatus
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteFileCatalogProtocolTest {
    @Test
    fun requestRoundTripKeepsKeywordFingerprintAndPath() {
        val request = RemoteFileCatalogRequest(
            keywordFingerprint = "fingerprint=with%chars",
            path = "/shared/folder",
        )

        val decoded = RemoteFileCatalogProtocol.decodeRequest(
            RemoteFileCatalogProtocol.encodeRequest(request),
        )

        assertEquals(request, decoded)
    }

    @Test
    fun deleteRequestRoundTripKeepsSelectedPaths() {
        val request = RemoteFileCatalogRequest(
            keywordFingerprint = "fingerprint=with%chars",
            operation = RemoteFileCatalogOperation.Delete,
            deletePaths = listOf("/shared/a.txt", "/shared/folder"),
        )

        val decoded = RemoteFileCatalogProtocol.decodeRequest(RemoteFileCatalogProtocol.encodeRequest(request))

        assertEquals(request, decoded)
    }

    @Test
    fun successResponseRoundTripKeepsEntries() {
        val response = RemoteFileCatalogResponse.Success(
            entries = listOf(
                FileEntry(
                    path = "/shared/file.txt",
                    name = "file.txt",
                    type = FileEntryType.File,
                    sizeBytes = 123L,
                    isBrowseable = false,
                ),
                FileEntry(
                    path = "/shared/folder",
                    name = "folder",
                    type = FileEntryType.Directory,
                    isBrowseable = true,
                ),
            ),
        )

        val decoded = RemoteFileCatalogProtocol.decodeResponse(
            RemoteFileCatalogProtocol.encodeResponse(response),
        )

        assertEquals(response, decoded)
    }

    @Test
    fun errorResponseRoundTripKeepsError() {
        val response = RemoteFileCatalogResponse.Failure(
            RemoteFileCatalogError(
                code = RemoteFileCatalogErrorCode.OutsideWhitelist,
                message = "outside",
            ),
        )

        val decoded = RemoteFileCatalogProtocol.decodeResponse(
            RemoteFileCatalogProtocol.encodeResponse(response),
        )

        assertEquals(response, decoded)
    }

    @Test
    fun deleteResponseRoundTripKeepsPerPathResults() {
        val response = RemoteFileCatalogResponse.DeleteCompleted(
            FileDeleteBatchResult(
                listOf(
                    FileDeleteResult("/shared/a.txt", FileDeleteStatus.Deleted),
                    FileDeleteResult("/shared/folder", FileDeleteStatus.Refused, "not allowed"),
                ),
            ),
        )

        val decoded = RemoteFileCatalogProtocol.decodeResponse(RemoteFileCatalogProtocol.encodeResponse(response))

        assertEquals(response, decoded)
    }
}
