package com.dimonoso.crosplatformfilesender.backup

import com.dimonoso.crosplatformfilesender.settings.BackupMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupConfigCodecTest {
    @Test
    fun backupRecordsRoundTripKeepsRestoreMetadata() {
        val records = listOf(
            BackupRecord(
                id = "backup-1",
                archivePath = "/cache/backups/backup-1.zip",
                originalTargetPath = "/downloads/report.txt",
                createdAtEpochMillis = 123L,
                mode = BackupMode.BackupOnlyFile,
                sizeBytes = 456L,
                sourceTaskId = "transfer-1",
            ),
        )

        val decoded = BackupConfigCodec.decode(BackupConfigCodec.encode(records))

        assertEquals(records, decoded)
    }
}
