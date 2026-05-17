package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DefaultTransferPortOffset = 1
const val LargeFileThresholdBytes = 500L * 1024L * 1024L
const val ArchivePartTargetBytes = 500L * 1024L * 1024L
const val TransferChunkBytes = 1024 * 1024

enum class TransferDirection {
    Upload,
    Download,
    Receive,
}

enum class TransferStatus {
    Pending,
    Preparing,
    Running,
    Paused,
    Completed,
    Failed,
    Cancelled,
}

enum class TransferPayloadKind {
    File,
    DirectoryArchive,
    ArchivePart,
}

data class TransferEndpoint(
    val deviceId: String,
    val displayName: String,
    val host: String = "",
    val catalogPort: Int = 0,
    val transferPort: Int = 0,
)

data class TransferItem(
    val path: String,
    val displayName: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val payloadKind: TransferPayloadKind = TransferPayloadKind.File,
    val destinationDirectoryPath: String? = null,
    val extractArchiveOnReceive: Boolean = false,
    val originalDisplayName: String = displayName,
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
    val errorMessage: String? = null,
)

interface TransferQueueService {
    val tasks: StateFlow<List<TransferTask>>

    fun startServer(port: Int)

    fun stopServer()

    fun enqueueUpload(
        items: List<TransferItem>,
        target: TransferEndpoint,
        destinationDirectoryPath: String,
    ): List<TransferTask>

    fun enqueueDownload(
        items: List<TransferItem>,
        source: TransferEndpoint,
        destinationDirectoryPath: String,
        archiveDirectories: Boolean,
    ): List<TransferTask>

    fun enqueueUpload(item: TransferItem, target: TransferEndpoint): TransferTask =
        enqueueUpload(listOf(item), target, item.destinationDirectoryPath.orEmpty()).first()

    fun enqueueDownload(item: TransferItem, source: TransferEndpoint, destinationPath: String): TransferTask =
        enqueueDownload(listOf(item), source, destinationPath, archiveDirectories = true).first()

    fun pause(taskId: String)

    fun resume(taskId: String)

    fun cancel(taskId: String)
}

fun DiscoveredDevice.toTransferEndpoint(): TransferEndpoint =
    TransferEndpoint(
        deviceId = id,
        displayName = displayName,
        host = host,
        catalogPort = port,
        transferPort = transferPort,
    )

class InMemoryTransferQueueService(
    private val localEndpoint: TransferEndpoint,
    private val timeProvider: () -> Long = { 0L },
) : TransferQueueService {
    private val _tasks = MutableStateFlow<List<TransferTask>>(emptyList())
    private var nextId = 1

    override val tasks: StateFlow<List<TransferTask>> = _tasks

    override fun startServer(port: Int) = Unit

    override fun stopServer() = Unit

    override fun enqueueUpload(
        items: List<TransferItem>,
        target: TransferEndpoint,
        destinationDirectoryPath: String,
    ): List<TransferTask> {
        val tasks = items.map { item ->
            TransferTask(
                id = nextTaskId(),
                direction = TransferDirection.Upload,
                source = localEndpoint,
                target = target,
                item = item.copy(destinationDirectoryPath = destinationDirectoryPath.ifBlank { item.destinationDirectoryPath }),
                requestedAtEpochMillis = timeProvider(),
            )
        }
        _tasks.value = _tasks.value + tasks
        return tasks
    }

    override fun enqueueDownload(
        items: List<TransferItem>,
        source: TransferEndpoint,
        destinationDirectoryPath: String,
        archiveDirectories: Boolean,
    ): List<TransferTask> {
        val tasks = items.map { item ->
            TransferTask(
                id = nextTaskId(),
                direction = TransferDirection.Download,
                source = source,
                target = localEndpoint,
                item = item.copy(
                    path = destinationDirectoryPath.trimEnd('/', '\\') + "/" + item.displayName,
                    destinationDirectoryPath = destinationDirectoryPath,
                    extractArchiveOnReceive = archiveDirectories && item.isDirectory,
                ),
                requestedAtEpochMillis = timeProvider(),
            )
        }
        _tasks.value = _tasks.value + tasks
        return tasks
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
