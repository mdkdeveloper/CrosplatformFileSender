package com.dimonoso.crosplatformfilesender.backup

import com.dimonoso.crosplatformfilesender.archive.ArchiveOperationResult
import com.dimonoso.crosplatformfilesender.archive.ArchiveService
import com.dimonoso.crosplatformfilesender.archive.CreateArchiveRequest
import com.dimonoso.crosplatformfilesender.archive.ExtractArchiveRequest
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import com.dimonoso.crosplatformfilesender.settings.BackupMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BackupRecord(
    val id: String,
    val archivePath: String,
    val originalTargetPath: String,
    val createdAtEpochMillis: Long,
    val mode: BackupMode,
    val sizeBytes: Long? = null,
    val sourceTaskId: String? = null,
)

sealed interface BackupOperationResult {
    data class Success(val record: BackupRecord? = null) : BackupOperationResult

    data class Failure(val message: String) : BackupOperationResult
}

interface BackupService {
    val records: StateFlow<List<BackupRecord>>

    fun createBackup(
        targetPath: String,
        mode: BackupMode,
        sourceTaskId: String?,
    ): BackupOperationResult

    fun restore(recordId: String): BackupOperationResult

    fun delete(recordId: String): BackupOperationResult
}

internal class PersistentBackupService(
    private val fileSystem: PlatformFileSystem,
    private val archiveService: ArchiveService,
    private val store: BackupStore,
    private val timeProvider: () -> Long = { 0L },
) : BackupService {
    private val _records = MutableStateFlow(loadRecords())

    override val records: StateFlow<List<BackupRecord>> = _records

    override fun createBackup(
        targetPath: String,
        mode: BackupMode,
        sourceTaskId: String?,
    ): BackupOperationResult {
        if (mode == BackupMode.DoNotBackup || !fileSystem.exists(targetPath)) {
            return BackupOperationResult.Success(null)
        }

        val backupRoot = fileSystem.childPath(fileSystem.cacheDirectoryPath(), "backups")
        fileSystem.createDirectories(backupRoot)
        val id = "backup-${timeProvider()}-${targetPath.hashCode()}"
        val archivePath = fileSystem.childPath(backupRoot, "$id.zip")
        val result = archiveService.createArchive(
            CreateArchiveRequest(
                sourcePaths = listOf(targetPath),
                destinationArchivePath = archivePath,
                baseDirectoryPath = fileSystem.parentPath(targetPath),
            ),
        )
        if (result !is ArchiveOperationResult.Success) {
            return BackupOperationResult.Failure(result.messageOrFallback())
        }

        val record = BackupRecord(
            id = id,
            archivePath = archivePath,
            originalTargetPath = targetPath,
            createdAtEpochMillis = timeProvider(),
            mode = mode,
            sizeBytes = fileSystem.metadata(archivePath)?.sizeBytes,
            sourceTaskId = sourceTaskId,
        )
        replace(_records.value + record)
        return BackupOperationResult.Success(record)
    }

    override fun restore(recordId: String): BackupOperationResult {
        val record = _records.value.firstOrNull { it.id == recordId }
            ?: return BackupOperationResult.Failure("Backup record was not found.")
        if (!fileSystem.exists(record.archivePath)) {
            return BackupOperationResult.Failure("Backup archive was not found.")
        }

        fileSystem.parentPath(record.originalTargetPath)?.let(fileSystem::createDirectories)
        val destination = fileSystem.parentPath(record.originalTargetPath)
            ?: return BackupOperationResult.Failure("Cannot resolve backup target parent.")
        return when (val result = archiveService.extractArchive(ExtractArchiveRequest(record.archivePath, destination))) {
            is ArchiveOperationResult.Success -> BackupOperationResult.Success(record)
            else -> BackupOperationResult.Failure(result.messageOrFallback())
        }
    }

    override fun delete(recordId: String): BackupOperationResult {
        val record = _records.value.firstOrNull { it.id == recordId }
            ?: return BackupOperationResult.Failure("Backup record was not found.")
        fileSystem.delete(record.archivePath, recursive = false)
        replace(_records.value.filterNot { it.id == recordId })
        return BackupOperationResult.Success(record)
    }

    private fun loadRecords(): List<BackupRecord> =
        store.readConfig()?.let(BackupConfigCodec::decode).orEmpty()

    private fun replace(records: List<BackupRecord>) {
        _records.value = records
        store.writeConfig(BackupConfigCodec.encode(records))
    }
}

internal interface BackupStore {
    fun readConfig(): String?

    fun writeConfig(contents: String)
}

internal expect fun createBackupStore(): BackupStore

private fun ArchiveOperationResult.messageOrFallback(): String =
    when (this) {
        is ArchiveOperationResult.Failure -> message
        is ArchiveOperationResult.NotImplementedYet -> message
        is ArchiveOperationResult.Success -> "Archive operation succeeded."
    }
