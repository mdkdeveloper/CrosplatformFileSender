package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.archive.ArchiveOperationResult
import com.dimonoso.crosplatformfilesender.archive.ArchiveService
import com.dimonoso.crosplatformfilesender.archive.CreateArchiveRequest
import com.dimonoso.crosplatformfilesender.archive.ExtractArchiveRequest
import com.dimonoso.crosplatformfilesender.backup.BackupOperationResult
import com.dimonoso.crosplatformfilesender.backup.BackupService
import com.dimonoso.crosplatformfilesender.discovery.discoveryKeywordFingerprint
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import com.dimonoso.crosplatformfilesender.settings.BackupMode
import com.dimonoso.crosplatformfilesender.settings.SettingsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class NetworkTransferQueueService(
    private val localEndpoint: TransferEndpoint,
    private val platformFileSystem: PlatformFileSystem,
    private val archiveService: ArchiveService,
    private val backupService: BackupService,
    private val settingsService: SettingsService,
    private val keywordProvider: () -> String,
    private val transport: FileTransferTransport = createFileTransferTransport(),
    private val timeProvider: () -> Long = { 0L },
) : TransferQueueService, FileTransferServerHandler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _tasks = MutableStateFlow<List<TransferTask>>(emptyList())
    private val activeOutgoing = mutableSetOf<String>()
    private val activeIncoming = mutableSetOf<String>()
    private var nextId = 1
    private var server: FileTransferServer? = null

    override val tasks: StateFlow<List<TransferTask>> = _tasks

    override fun startServer(port: Int) {
        stopServer()
        server = runCatching {
            transport.startServer(port, platformFileSystem, this)
        }.getOrNull()
    }

    override fun stopServer() {
        server?.close()
        server = null
    }

    override fun enqueueUpload(
        items: List<TransferItem>,
        target: TransferEndpoint,
        destinationDirectoryPath: String,
    ): List<TransferTask> {
        if (destinationDirectoryPath.isBlank()) {
            return addFailedTasks(items, TransferDirection.Upload, localEndpoint, target, "Select a destination folder first.")
        }

        val tasks = items.flatMap { item ->
            createUploadTasks(item, target, destinationDirectoryPath)
        }
        appendTasks(tasks)
        scheduleOutgoing()
        return tasks
    }

    override fun enqueueDownload(
        items: List<TransferItem>,
        source: TransferEndpoint,
        destinationDirectoryPath: String,
        archiveDirectories: Boolean,
    ): List<TransferTask> {
        if (destinationDirectoryPath.isBlank()) {
            return addFailedTasks(items, TransferDirection.Download, source, localEndpoint, "Select a destination folder first.")
        }

        val tasks = items.map { item ->
            val itemDestination = item.destinationDirectoryPath ?: destinationDirectoryPath
            newTask(
                direction = TransferDirection.Download,
                source = source,
                target = localEndpoint,
                item = item.copy(
                    destinationDirectoryPath = itemDestination,
                    extractArchiveOnReceive = item.isDirectory && archiveDirectories,
                    payloadKind = if (item.isDirectory) TransferPayloadKind.DirectoryArchive else TransferPayloadKind.File,
                ),
            )
        }
        appendTasks(tasks)
        scheduleOutgoing()
        return tasks
    }

    override fun pause(taskId: String) {
        changeTask(taskId) { task ->
            if (task.status == TransferStatus.Pending || task.status == TransferStatus.Running) {
                task.copy(status = TransferStatus.Paused)
            } else {
                task
            }
        }
    }

    override fun resume(taskId: String) {
        changeTask(taskId) { task ->
            if (task.status == TransferStatus.Paused) task.copy(status = TransferStatus.Pending) else task
        }
        scheduleOutgoing()
    }

    override fun cancel(taskId: String) {
        changeTask(taskId) { task -> task.copy(status = TransferStatus.Cancelled) }
    }

    override suspend fun receiveUpload(request: FileTransferRequest, input: TransferInput): FileTransferResponse {
        if (!request.isAuthorized()) {
            drainInput(input, request.sizeBytes)
            return FileTransferResponse(success = false, message = "Unauthorized transfer request.")
        }
        waitForIncomingSlot()
        val receiveTask = newTask(
            id = "receive-${request.taskId}",
            direction = TransferDirection.Receive,
            source = TransferEndpoint("remote", request.displayName),
            target = localEndpoint,
            item = TransferItem(
                path = request.sourcePath,
                displayName = request.displayName,
                isDirectory = request.extractArchiveOnReceive,
                sizeBytes = request.sizeBytes,
                payloadKind = request.payloadKind,
                destinationDirectoryPath = request.destinationDirectoryPath,
                extractArchiveOnReceive = request.extractArchiveOnReceive,
                originalDisplayName = request.originalDisplayName,
            ),
            status = TransferStatus.Running,
        )
        appendTasks(listOf(receiveTask))
        activeIncoming += receiveTask.id

        return try {
            runCatching {
                if (request.extractArchiveOnReceive) {
                    receiveArchive(request, input, receiveTask.id)
                } else {
                    receiveFile(request, input, receiveTask.id)
                }
            }.getOrElse { exception ->
                FileTransferResponse(false, exception.readableMessage())
            }.also { response ->
                completeFromResponse(receiveTask.id, response)
            }
        } finally {
            activeIncoming -= receiveTask.id
        }
    }

    override suspend fun prepareDownload(request: FileTransferRequest): PreparedDownload {
        if (!request.isAuthorized()) {
            return PreparedDownload(FileTransferResponse(success = false, message = "Unauthorized transfer request."))
        }
        val entry = platformFileSystem.metadata(request.sourcePath)
            ?: return PreparedDownload(FileTransferResponse(success = false, message = "Source item was not found."))

        return if (entry.type == FileEntryType.Directory || entry.type == FileEntryType.Drive) {
            if (!request.archiveDirectory) {
                return PreparedDownload(FileTransferResponse(success = false, message = "Directory downloads without archive are not available yet."))
            }
            val archivePath = transferCachePath("${request.taskId}-${entry.name}.zip")
            when (val result = archiveService.createArchive(
                CreateArchiveRequest(
                    sourcePaths = listOf(request.sourcePath),
                    destinationArchivePath = archivePath,
                    baseDirectoryPath = platformFileSystem.parentPath(request.sourcePath),
                ),
            )) {
                is ArchiveOperationResult.Success -> {
                    val size = platformFileSystem.metadata(archivePath)?.sizeBytes ?: 0L
                    PreparedDownload(
                        response = FileTransferResponse(
                            success = true,
                            sizeBytes = size,
                            displayName = entry.name + ".zip",
                            originalDisplayName = entry.name,
                            extractArchiveOnReceive = true,
                        ),
                        sourcePath = archivePath,
                    )
                }
                else -> PreparedDownload(FileTransferResponse(success = false, message = result.messageOrFallback()))
            }
        } else {
            PreparedDownload(
                response = FileTransferResponse(
                    success = true,
                    sizeBytes = entry.sizeBytes ?: 0L,
                    displayName = entry.name,
                    originalDisplayName = entry.name,
                    extractArchiveOnReceive = false,
                ),
                sourcePath = request.sourcePath,
            )
        }
    }

    private fun createUploadTasks(
        item: TransferItem,
        target: TransferEndpoint,
        destinationDirectoryPath: String,
    ): List<TransferTask> {
        if (!item.isDirectory) {
            val resolved = item.copy(destinationDirectoryPath = destinationDirectoryPath)
            return listOf(newTask(TransferDirection.Upload, localEndpoint, target, resolved))
        }

        val directory = platformFileSystem.metadata(item.path) ?: return listOf(
            newTask(TransferDirection.Upload, localEndpoint, target, item).copy(
                status = TransferStatus.Failed,
                errorMessage = "Source directory was not found.",
            ),
        )
        val files = collectFiles(directory.path)
        val largeFiles = files.filter { (it.sizeBytes ?: 0L) >= LargeFileThresholdBytes }
        val smallFiles = files - largeFiles.toSet()
        val tasks = mutableListOf<TransferTask>()

        smallFiles.chunkBySize(ArchivePartTargetBytes).forEachIndexed { index, part ->
            val archiveName = "${directory.name}-part-${index + 1}.zip"
            val archivePath = transferCachePath(archiveName)
            val result = archiveService.createArchive(
                CreateArchiveRequest(
                    sourcePaths = part.map { it.path },
                    destinationArchivePath = archivePath,
                    baseDirectoryPath = platformFileSystem.parentPath(directory.path),
                ),
            )
            val archiveItem = TransferItem(
                path = archivePath,
                displayName = archiveName,
                isDirectory = false,
                sizeBytes = platformFileSystem.metadata(archivePath)?.sizeBytes,
                payloadKind = if (index == 0) TransferPayloadKind.DirectoryArchive else TransferPayloadKind.ArchivePart,
                destinationDirectoryPath = destinationDirectoryPath,
                extractArchiveOnReceive = true,
                originalDisplayName = directory.name,
            )
            tasks += newTask(TransferDirection.Upload, localEndpoint, target, archiveItem).let { task ->
                if (result is ArchiveOperationResult.Success) task else task.copy(
                    status = TransferStatus.Failed,
                    errorMessage = result.messageOrFallback(),
                )
            }
        }

        largeFiles.forEach { file ->
            val relativeParent = relativeParentDirectory(directory.path, file.path)
            val targetDirectory = relativeParent
                .split('/')
                .filter { it.isNotBlank() }
                .fold(platformFileSystem.childPath(destinationDirectoryPath, directory.name)) { current, segment ->
                    platformFileSystem.childPath(current, segment)
                }
            tasks += newTask(
                TransferDirection.Upload,
                localEndpoint,
                target,
                TransferItem(
                    path = file.path,
                    displayName = file.name,
                    isDirectory = false,
                    sizeBytes = file.sizeBytes,
                    destinationDirectoryPath = targetDirectory,
                    originalDisplayName = file.name,
                ),
            )
        }

        return tasks.ifEmpty {
            listOf(
                newTask(
                    TransferDirection.Upload,
                    localEndpoint,
                    target,
                    item.copy(destinationDirectoryPath = destinationDirectoryPath),
                ).copy(status = TransferStatus.Completed),
            )
        }
    }

    private fun scheduleOutgoing() {
        val maxOutgoing = settingsService.settings.value.maxOutgoingTransfers
        val available = (maxOutgoing - activeOutgoing.size).coerceAtLeast(0)
        if (available == 0) return
        _tasks.value
            .filter { it.status == TransferStatus.Pending && it.direction != TransferDirection.Receive }
            .take(available)
            .forEach { task ->
                activeOutgoing += task.id
                changeTask(task.id) { it.copy(status = TransferStatus.Running, errorMessage = null) }
                scope.launch {
                    try {
                        runOutgoingTask(task.id)
                    } finally {
                        activeOutgoing -= task.id
                        scheduleOutgoing()
                    }
                }
            }
    }

    private suspend fun runOutgoingTask(taskId: String) {
        val task = task(taskId) ?: return
        if (task.status == TransferStatus.Cancelled) return
        val response = runCatching {
            when (task.direction) {
                TransferDirection.Upload -> runUpload(task)
                TransferDirection.Download -> runDownload(task)
                TransferDirection.Receive -> FileTransferResponse(false, "Receive tasks are handled by the server.")
            }
        }.getOrElse { exception ->
            FileTransferResponse(false, exception.readableMessage())
        }
        completeFromResponse(taskId, response)
    }

    private suspend fun runUpload(task: TransferTask): FileTransferResponse {
        val source = platformFileSystem.metadata(task.item.path)
            ?: return FileTransferResponse(false, "Source file was not found.")
        val request = FileTransferRequest(
            type = FileTransferRequestType.Upload,
            taskId = task.id,
            keywordFingerprint = discoveryKeywordFingerprint(keywordProvider()),
            sourcePath = task.item.path,
            destinationDirectoryPath = task.item.destinationDirectoryPath.orEmpty(),
            displayName = task.item.displayName,
            originalDisplayName = task.item.originalDisplayName,
            sizeBytes = source.sizeBytes ?: task.item.sizeBytes ?: 0L,
            payloadKind = task.item.payloadKind,
            extractArchiveOnReceive = task.item.extractArchiveOnReceive,
            backupMode = settingsService.settings.value.backupMode,
        )
        return transport.upload(
            host = task.target.host,
            port = task.target.transferPort,
            request = request,
            fileSystem = platformFileSystem,
            onProgress = { progress -> updateProgress(task.id, progress, request.sizeBytes) },
        )
    }

    private suspend fun runDownload(task: TransferTask): FileTransferResponse {
        val destinationDirectory = task.item.destinationDirectoryPath.orEmpty()
        val request = FileTransferRequest(
            type = FileTransferRequestType.Download,
            taskId = task.id,
            keywordFingerprint = discoveryKeywordFingerprint(keywordProvider()),
            sourcePath = task.item.path,
            destinationDirectoryPath = destinationDirectory,
            displayName = task.item.displayName,
            originalDisplayName = task.item.displayName,
            sizeBytes = 0L,
            payloadKind = task.item.payloadKind,
            extractArchiveOnReceive = task.item.extractArchiveOnReceive,
            backupMode = settingsService.settings.value.backupMode,
            archiveDirectory = task.item.extractArchiveOnReceive,
        )
        val targetPath = if (task.item.extractArchiveOnReceive) {
            transferCachePath("${task.id}-${task.item.displayName}.zip")
        } else {
            platformFileSystem.tempPathFor(platformFileSystem.childPath(destinationDirectory, task.item.displayName))
        }
        val response = transport.download(
            host = task.source.host,
            port = task.source.transferPort,
            request = request,
            fileSystem = platformFileSystem,
            destinationPath = targetPath,
            onProgress = { progress -> updateProgress(task.id, progress, null) },
        )
        if (!response.success) return response

        return if (response.extractArchiveOnReceive) {
            val conflictTarget = platformFileSystem.childPath(destinationDirectory, response.originalDisplayName)
            createBackupIfNeeded(conflictTarget, settingsService.settings.value.backupMode, task.id)
            when (val extracted = archiveService.extractArchive(ExtractArchiveRequest(targetPath, destinationDirectory))) {
                is ArchiveOperationResult.Success -> response
                else -> response.copy(success = false, message = extracted.messageOrFallback())
            }
        } else {
            val finalPath = platformFileSystem.childPath(destinationDirectory, response.displayName.ifBlank { task.item.displayName })
            createBackupIfNeeded(finalPath, settingsService.settings.value.backupMode, task.id)
            platformFileSystem.move(targetPath, finalPath, replace = true)
            response
        }
    }

    private suspend fun receiveFile(
        request: FileTransferRequest,
        input: TransferInput,
        taskId: String,
    ): FileTransferResponse {
        val destinationDirectory = request.destinationDirectoryPath
        platformFileSystem.createDirectories(destinationDirectory)
        val targetPath = platformFileSystem.childPath(destinationDirectory, request.displayName)
        val tempPath = platformFileSystem.tempPathFor(targetPath)
        writeInputToPath(input, tempPath, request.sizeBytes, taskId)
        createBackupIfNeeded(targetPath, request.backupMode, taskId)
        platformFileSystem.move(tempPath, targetPath, replace = true)
        return FileTransferResponse(success = true, message = "Transfer completed.")
    }

    private suspend fun receiveArchive(
        request: FileTransferRequest,
        input: TransferInput,
        taskId: String,
    ): FileTransferResponse {
        val destinationDirectory = request.destinationDirectoryPath
        platformFileSystem.createDirectories(destinationDirectory)
        val archivePath = transferCachePath("${taskId}-${request.displayName}")
        writeInputToPath(input, archivePath, request.sizeBytes, taskId)
        val conflictTarget = platformFileSystem.childPath(destinationDirectory, request.originalDisplayName)
        createBackupIfNeeded(conflictTarget, request.backupMode, taskId)
        return when (val extracted = archiveService.extractArchive(ExtractArchiveRequest(archivePath, destinationDirectory))) {
            is ArchiveOperationResult.Success -> FileTransferResponse(success = true, message = "Transfer completed.")
            else -> FileTransferResponse(success = false, message = extracted.messageOrFallback())
        }
    }

    private suspend fun writeInputToPath(
        input: TransferInput,
        path: String,
        expectedBytes: Long,
        taskId: String,
    ) {
        platformFileSystem.parentPath(path)?.let(platformFileSystem::createDirectories)
        platformFileSystem.openWrite(path).useClosing { output ->
            val buffer = ByteArray(TransferChunkBytes)
            var total = 0L
            while (expectedBytes <= 0L || total < expectedBytes) {
                val remaining = if (expectedBytes <= 0L) buffer.size else minOf(buffer.size.toLong(), expectedBytes - total).toInt()
                val read = input.read(buffer, 0, remaining)
                if (read < 0) break
                output.write(buffer, 0, read)
                total += read
                updateProgress(taskId, total, expectedBytes)
            }
        }
    }

    private fun createBackupIfNeeded(targetPath: String, mode: BackupMode, taskId: String) {
        if (!platformFileSystem.exists(targetPath)) return
        when (val result = backupService.createBackup(targetPath, mode, taskId)) {
            is BackupOperationResult.Failure -> error(result.message)
            is BackupOperationResult.Success -> Unit
        }
    }

    private suspend fun waitForIncomingSlot() {
        while (activeIncoming.size >= settingsService.settings.value.maxIncomingTransfers) {
            delay(100L)
        }
    }

    private fun collectFiles(path: String): List<FileEntry> {
        val entry = platformFileSystem.metadata(path) ?: return emptyList()
        if (entry.type != FileEntryType.Directory && entry.type != FileEntryType.Drive) return listOf(entry)
        return platformFileSystem.list(path).flatMap { child ->
            if (child.type == FileEntryType.Directory || child.type == FileEntryType.Drive) {
                collectFiles(child.path)
            } else {
                listOf(child)
            }
        }
    }

    private fun transferCachePath(fileName: String): String {
        val root = platformFileSystem.childPath(platformFileSystem.cacheDirectoryPath(), "transfers")
        platformFileSystem.createDirectories(root)
        return platformFileSystem.childPath(root, fileName.sanitizeFileName())
    }

    private fun addFailedTasks(
        items: List<TransferItem>,
        direction: TransferDirection,
        source: TransferEndpoint,
        target: TransferEndpoint,
        message: String,
    ): List<TransferTask> {
        val tasks = items.map { item ->
            newTask(direction, source, target, item).copy(status = TransferStatus.Failed, errorMessage = message)
        }
        appendTasks(tasks)
        return tasks
    }

    private fun appendTasks(tasks: List<TransferTask>) {
        if (tasks.isEmpty()) return
        _tasks.value = _tasks.value + tasks
    }

    private fun newTask(
        direction: TransferDirection,
        source: TransferEndpoint,
        target: TransferEndpoint,
        item: TransferItem,
    ): TransferTask =
        newTask(nextTaskId(), direction, source, target, item)

    private fun newTask(
        id: String = nextTaskId(),
        direction: TransferDirection,
        source: TransferEndpoint,
        target: TransferEndpoint,
        item: TransferItem,
        status: TransferStatus = TransferStatus.Pending,
    ): TransferTask =
        TransferTask(
            id = id,
            direction = direction,
            source = source,
            target = target,
            item = item,
            requestedAtEpochMillis = timeProvider(),
            status = status,
            totalBytes = item.sizeBytes,
        )

    private fun nextTaskId(): String = "transfer-${nextId++}"

    private fun changeTask(taskId: String, transform: (TransferTask) -> TransferTask) {
        _tasks.value = _tasks.value.map { task -> if (task.id == taskId) transform(task) else task }
    }

    private fun updateProgress(taskId: String, progressBytes: Long, totalBytes: Long?) {
        changeTask(taskId) { task ->
            task.copy(progressBytes = progressBytes, totalBytes = totalBytes ?: task.totalBytes)
        }
    }

    private fun completeFromResponse(taskId: String, response: FileTransferResponse) {
        changeTask(taskId) { task ->
            if (task.status == TransferStatus.Cancelled) {
                task
            } else if (response.success) {
                task.copy(status = TransferStatus.Completed, progressBytes = task.totalBytes ?: task.progressBytes)
            } else {
                task.copy(status = TransferStatus.Failed, errorMessage = response.message)
            }
        }
    }

    private fun task(taskId: String): TransferTask? =
        _tasks.value.firstOrNull { it.id == taskId }

    private fun FileTransferRequest.isAuthorized(): Boolean =
        keywordFingerprint == discoveryKeywordFingerprint(keywordProvider())
}

private suspend fun drainInput(input: TransferInput, expectedBytes: Long) {
    val buffer = ByteArray(TransferChunkBytes)
    var total = 0L
    while (expectedBytes <= 0L || total < expectedBytes) {
        val remaining = if (expectedBytes <= 0L) buffer.size else minOf(buffer.size.toLong(), expectedBytes - total).toInt()
        val read = input.read(buffer, 0, remaining)
        if (read < 0) break
        total += read
    }
}

private fun List<FileEntry>.chunkBySize(maxBytes: Long): List<List<FileEntry>> {
    val chunks = mutableListOf<List<FileEntry>>()
    var current = mutableListOf<FileEntry>()
    var currentSize = 0L
    forEach { file ->
        val fileSize = file.sizeBytes ?: 0L
        if (current.isNotEmpty() && currentSize + fileSize > maxBytes) {
            chunks += current
            current = mutableListOf()
            currentSize = 0L
        }
        current += file
        currentSize += fileSize
    }
    if (current.isNotEmpty()) chunks += current
    return chunks
}

private fun relativeParentDirectory(rootPath: String, filePath: String): String {
    val normalizedRoot = rootPath.normalizePath().trimEnd('/')
    val normalizedFile = filePath.normalizePath()
    val relative = normalizedFile.removePrefix("$normalizedRoot/").substringBeforeLast('/', missingDelimiterValue = "")
    return relative
}

private fun String.normalizePath(): String =
    replace('\\', '/').trimEnd('/')

private fun String.sanitizeFileName(): String =
    map { char ->
        when (char) {
            '/', '\\', ':', '*', '?', '"', '<', '>', '|' -> '_'
            else -> char
        }
    }.joinToString("")

private fun ArchiveOperationResult.messageOrFallback(): String =
    when (this) {
        is ArchiveOperationResult.Failure -> message
        is ArchiveOperationResult.NotImplementedYet -> message
        is ArchiveOperationResult.Success -> "Archive operation succeeded."
    }

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"

private inline fun <T : AutoCloseable, R> T.useClosing(block: (T) -> R): R {
    var failure: Throwable? = null
    try {
        return block(this)
    } catch (throwable: Throwable) {
        failure = throwable
        throw throwable
    } finally {
        try {
            close()
        } catch (closeFailure: Throwable) {
            if (failure == null) throw closeFailure
        }
    }
}
