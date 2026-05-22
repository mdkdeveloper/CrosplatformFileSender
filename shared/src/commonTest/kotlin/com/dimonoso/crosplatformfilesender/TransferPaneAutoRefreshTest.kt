package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.transfer.TransferDirection
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferItem
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus
import com.dimonoso.crosplatformfilesender.transfer.TransferTask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransferPaneAutoRefreshTest {
    @Test
    fun completedUploadRefreshesSelectedRemoteDestinationFolder() {
        val request = completedTransferPaneRefreshRequest(
            tasks = listOf(completedTask(TransferDirection.Upload, "/remote/inbox")),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = "/downloads",
            remotePath = "/remote/inbox",
        )

        assertTrue(request.remote)
        assertFalse(request.local)
    }

    @Test
    fun completedUploadForAnotherDeviceOrFolderDoesNotRefreshRemotePane() {
        val request = completedTransferPaneRefreshRequest(
            tasks = listOf(
                completedTask(TransferDirection.Upload, "/remote/inbox").copy(target = OtherRemoteEndpoint),
                completedTask(TransferDirection.Upload, "/remote/archive"),
            ),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = null,
            remotePath = "/remote/inbox",
        )

        assertFalse(request.remote)
    }

    @Test
    fun completedReceiveAndDownloadRefreshLocalDestinationFolder() {
        val request = completedTransferPaneRefreshRequest(
            tasks = listOf(
                completedTask(TransferDirection.Receive, "/downloads"),
                completedTask(TransferDirection.Download, "/downloads/nested"),
            ),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = "/downloads",
            remotePath = "/remote/inbox",
        )

        assertTrue(request.local)
        assertFalse(request.remote)
    }

    @Test
    fun destinationDescendantRefreshesOpenAncestor() {
        assertTrue(folderContainsDestination("/remote/inbox", "/remote/inbox/folder/subfolder"))
        assertTrue(folderContainsDestination("C:\\Downloads", "C:\\Downloads\\folder"))
        assertFalse(folderContainsDestination("/remote/in", "/remote/inbox"))
    }

    @Test
    fun unfinishedAndFailedTasksDoNotRefresh() {
        val request = completedTransferPaneRefreshRequest(
            tasks = listOf(
                completedTask(TransferDirection.Upload, "/remote/inbox").copy(status = TransferStatus.Running),
                completedTask(TransferDirection.Receive, "/downloads").copy(status = TransferStatus.Failed),
                completedTask(TransferDirection.Download, "/downloads").copy(status = TransferStatus.Cancelled),
            ),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = "/downloads",
            remotePath = "/remote/inbox",
        )

        assertEquals(TransferPaneRefreshRequest(), request)
    }

    @Test
    fun trackerConsumesCompletedTaskOnlyOnce() {
        val tracker = TransferPaneAutoRefreshTracker()
        val task = completedTask(TransferDirection.Upload, "/remote/inbox")

        val first = tracker.consume(
            tasks = listOf(task),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = null,
            remotePath = "/remote/inbox",
        )
        val second = tracker.consume(
            tasks = listOf(task),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = null,
            remotePath = "/remote/inbox",
        )

        assertTrue(first.remote)
        assertEquals(TransferPaneRefreshRequest(), second)
    }

    @Test
    fun trackerCoalescesCompletedBurstPerPane() {
        val request = TransferPaneAutoRefreshTracker().consume(
            tasks = listOf(
                completedTask(TransferDirection.Upload, "/remote/inbox"),
                completedTask(TransferDirection.Upload, "/remote/inbox/folder"),
                completedTask(TransferDirection.Receive, "/downloads"),
                completedTask(TransferDirection.Download, "/downloads/nested"),
            ),
            selectedRemoteDeviceId = RemoteEndpoint.deviceId,
            localPath = "/downloads",
            remotePath = "/remote/inbox",
        )

        assertEquals(TransferPaneRefreshRequest(local = true, remote = true), request)
    }

    private fun completedTask(
        direction: TransferDirection,
        destinationDirectoryPath: String,
    ): TransferTask =
        TransferTask(
            id = "$direction-$destinationDirectoryPath",
            direction = direction,
            source = if (direction == TransferDirection.Upload) LocalEndpoint else RemoteEndpoint,
            target = if (direction == TransferDirection.Upload) RemoteEndpoint else LocalEndpoint,
            item = TransferItem(
                path = "/source/$direction",
                displayName = "item",
                isDirectory = false,
                destinationDirectoryPath = destinationDirectoryPath,
            ),
            requestedAtEpochMillis = 1L,
            status = TransferStatus.Completed,
        )

    private companion object {
        val LocalEndpoint = TransferEndpoint(deviceId = "local", displayName = "Local")
        val RemoteEndpoint = TransferEndpoint(deviceId = "remote", displayName = "Remote")
        val OtherRemoteEndpoint = TransferEndpoint(deviceId = "other", displayName = "Other")
    }
}
