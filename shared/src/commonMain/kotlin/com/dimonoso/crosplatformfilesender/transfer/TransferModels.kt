package com.dimonoso.crosplatformfilesender.transfer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class TransferDirection {
    Upload,
    Download,
}

enum class TransferStatus {
    Pending,
    Running,
    Paused,
    Completed,
    Failed,
    Cancelled,
}

data class TransferEndpoint(
    val deviceId: String,
    val displayName: String,
)

data class TransferItem(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
)

data class TransferTask(
    val id: String,
    val direction: TransferDirection,
    val source: TransferEndpoint,
    val target: TransferEndpoint,
    val item: TransferItem,
    val requestedAtEpochMillis: Long,
    val status: TransferStatus = TransferStatus.Pending,
    val progressBytes: Long = 0L,
    val totalBytes: Long? = item.sizeBytes,
)

interface TransferQueueService {
    val tasks: StateFlow<List<TransferTask>>

    fun enqueueUpload(item: TransferItem, target: TransferEndpoint): TransferTask

    fun enqueueDownload(item: TransferItem, source: TransferEndpoint, destinationPath: String): TransferTask

    fun pause(taskId: String)

    fun resume(taskId: String)

    fun cancel(taskId: String)
}

class InMemoryTransferQueueService(
    private val localEndpoint: TransferEndpoint,
    private val timeProvider: () -> Long = { 0L },
) : TransferQueueService {
    private val _tasks = MutableStateFlow<List<TransferTask>>(emptyList())
    private var nextId = 1

    override val tasks: StateFlow<List<TransferTask>> = _tasks

    override fun enqueueUpload(item: TransferItem, target: TransferEndpoint): TransferTask {
        val task = TransferTask(
            id = nextTaskId(),
            direction = TransferDirection.Upload,
            source = localEndpoint,
            target = target,
            item = item,
            requestedAtEpochMillis = timeProvider(),
        )
        _tasks.value = _tasks.value + task
        return task
    }

    override fun enqueueDownload(item: TransferItem, source: TransferEndpoint, destinationPath: String): TransferTask {
        val destinationItem = item.copy(path = destinationPath.trimEnd('/', '\\') + "/" + item.displayName)
        val task = TransferTask(
            id = nextTaskId(),
            direction = TransferDirection.Download,
            source = source,
            target = localEndpoint,
            item = destinationItem,
            requestedAtEpochMillis = timeProvider(),
        )
        _tasks.value = _tasks.value + task
        return task
    }

    override fun pause(taskId: String) {
        changeStatus(taskId, TransferStatus.Paused)
    }

    override fun resume(taskId: String) {
        changeStatus(taskId, TransferStatus.Pending)
    }

    override fun cancel(taskId: String) {
        changeStatus(taskId, TransferStatus.Cancelled)
    }

    private fun changeStatus(taskId: String, status: TransferStatus) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(status = status) else task
        }
    }

    private fun nextTaskId(): String = "transfer-${nextId++}"
}
