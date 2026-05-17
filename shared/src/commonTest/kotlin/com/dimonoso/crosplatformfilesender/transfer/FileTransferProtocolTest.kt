package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.settings.BackupMode
import kotlin.test.Test
import kotlin.test.assertEquals

class FileTransferProtocolTest {
    @Test
    fun requestRoundTripKeepsTransferMetadata() {
        val request = FileTransferRequest(
            type = FileTransferRequestType.Upload,
            taskId = "task-1",
            keywordFingerprint = "fingerprint=with%chars",
            sourcePath = "/source/folder/file.txt",
            destinationDirectoryPath = "/target/folder",
            displayName = "file.txt",
            originalDisplayName = "folder",
            sizeBytes = 12345L,
            payloadKind = TransferPayloadKind.ArchivePart,
            extractArchiveOnReceive = true,
            backupMode = BackupMode.BackupFolder,
            archiveDirectory = true,
        )

        val decoded = FileTransferProtocol.decodeRequest(FileTransferProtocol.encodeRequest(request))

        assertEquals(request, decoded)
    }

    @Test
    fun responseRoundTripKeepsDownloadMetadata() {
        val response = FileTransferResponse(
            success = true,
            message = "ok",
            sizeBytes = 42L,
            displayName = "folder.zip",
            extractArchiveOnReceive = true,
            originalDisplayName = "folder",
        )

        val decoded = FileTransferProtocol.decodeResponse(FileTransferProtocol.encodeResponse(response))

        assertEquals(response, decoded)
    }
}
