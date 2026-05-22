package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.transfer.TransferDirection
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus
import com.dimonoso.crosplatformfilesender.transfer.TransferTask

internal data class TransferPaneRefreshRequest(
    val local: Boolean = false,
    val remote: Boolean = false,
)

internal class TransferPaneAutoRefreshTracker {
    private val completedTaskIds = mutableSetOf<String>()

    fun consume(
        tasks: List<TransferTask>,
        selectedRemoteDeviceId: String?,
        localPath: String?,
        remotePath: String?,
    ): TransferPaneRefreshRequest {
        val newlyCompleted = tasks.filter { task ->
            task.status == TransferStatus.Completed && completedTaskIds.add(task.id)
        }

        return completedTransferPaneRefreshRequest(
            tasks = newlyCompleted,
            selectedRemoteDeviceId = selectedRemoteDeviceId,
            localPath = localPath,
            remotePath = remotePath,
        )
    }
}

internal fun completedTransferPaneRefreshRequest(
    tasks: List<TransferTask>,
    selectedRemoteDeviceId: String?,
    localPath: String?,
    remotePath: String?,
): TransferPaneRefreshRequest {
    var localRefresh = false
    var remoteRefresh = false

    tasks.forEach { task ->
        if (task.status != TransferStatus.Completed) return@forEach
        val destinationPath = task.item.destinationDirectoryPath ?: return@forEach

        when (task.direction) {
            TransferDirection.Upload -> {
                if (
                    task.target.deviceId == selectedRemoteDeviceId &&
                    folderContainsDestination(remotePath, destinationPath)
                ) {
                    remoteRefresh = true
                }
            }
            TransferDirection.Download,
            TransferDirection.Receive -> {
                if (folderContainsDestination(localPath, destinationPath)) {
                    localRefresh = true
                }
            }
        }
    }

    return TransferPaneRefreshRequest(
        local = localRefresh,
        remote = remoteRefresh,
    )
}

internal fun folderContainsDestination(
    openFolderPath: String?,
    destinationDirectoryPath: String?,
): Boolean {
    val openFolder = openFolderPath.normalizeRefreshPath() ?: return false
    val destination = destinationDirectoryPath.normalizeRefreshPath() ?: return false
    return destination.equals(openFolder, ignoreCase = true) ||
        destination.startsWith("$openFolder/", ignoreCase = true)
}

private fun String?.normalizeRefreshPath(): String? =
    this
        ?.replace('\\', '/')
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { path -> path.isNotBlank() }
