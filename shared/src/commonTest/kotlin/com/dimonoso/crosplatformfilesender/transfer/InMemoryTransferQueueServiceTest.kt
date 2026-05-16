package com.dimonoso.crosplatformfilesender.transfer

import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryTransferQueueServiceTest {
    @Test
    fun queueAddsUploadDownloadAndChangesStatuses() {
        val local = TransferEndpoint(deviceId = "local", displayName = "Local")
        val remote = TransferEndpoint(deviceId = "remote", displayName = "Remote")
        val service = InMemoryTransferQueueService(
            localEndpoint = local,
            timeProvider = { 42L },
        )

        val upload = service.enqueueUpload(
            item = TransferItem(
                path = "/local/file.zip",
                displayName = "file.zip",
                isDirectory = false,
            ),
            target = remote,
        )
        val download = service.enqueueDownload(
            item = TransferItem(
                path = "/remote/photos",
                displayName = "photos",
                isDirectory = true,
            ),
            source = remote,
            destinationPath = "/downloads",
        )

        assertEquals(listOf(upload.id, download.id), service.tasks.value.map { it.id })
        assertEquals(TransferDirection.Upload, service.tasks.value[0].direction)
        assertEquals(TransferDirection.Download, service.tasks.value[1].direction)

        service.pause(upload.id)
        assertEquals(TransferStatus.Paused, service.tasks.value.first { it.id == upload.id }.status)

        service.resume(upload.id)
        assertEquals(TransferStatus.Pending, service.tasks.value.first { it.id == upload.id }.status)

        service.cancel(download.id)
        assertEquals(TransferStatus.Cancelled, service.tasks.value.first { it.id == download.id }.status)
    }
}
