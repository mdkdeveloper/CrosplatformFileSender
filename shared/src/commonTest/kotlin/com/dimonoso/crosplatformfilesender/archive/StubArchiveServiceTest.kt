package com.dimonoso.crosplatformfilesender.archive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StubArchiveServiceTest {
    @Test
    fun archiveServiceExposesZipAsTheBaseFormat() {
        val service = StubArchiveService()

        assertEquals(setOf(ArchiveFormat.ZIP), service.supportedFormats)

        val result = service.createArchive(
            CreateArchiveRequest(
                sourcePaths = listOf("/input"),
                destinationArchivePath = "/output/archive.zip",
            ),
        )

        assertTrue(result is ArchiveOperationResult.NotImplementedYet)
        assertEquals(ArchiveFormat.ZIP, result.format)
    }
}
