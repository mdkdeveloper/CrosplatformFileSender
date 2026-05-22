package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.platform.createPlatformServices
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExternalFilePaneDropJvmTest {
    private val fileSystem = createPlatformServices().fileSystem

    @Test
    fun fileUriConvertsToUploadFileEntry() {
        val file = Files.createTempFile("cfs-external-drop", ".txt").toFile()
        file.writeText("payload")

        val entries = externalFileEntriesFromUris(listOf(file.toURI().toString()), fileSystem::metadata)

        assertEquals(listOf(file.absolutePath), entries.map { entry -> entry.path })
        assertEquals(listOf(FileEntryType.File), entries.map { entry -> entry.type })
        file.delete()
    }

    @Test
    fun directoryUriConvertsToUploadDirectoryEntry() {
        val directory = Files.createTempDirectory("cfs-external-drop").toFile()

        val entries = externalFileEntriesFromUris(listOf(directory.toURI().toString()), fileSystem::metadata)

        assertEquals(listOf(directory.absolutePath), entries.map { entry -> entry.path })
        assertEquals(listOf(FileEntryType.Directory), entries.map { entry -> entry.type })
        directory.delete()
    }

    @Test
    fun missingAndUnsupportedUrisAreIgnored() {
        val missing = File(System.getProperty("java.io.tmpdir"), "cfs-missing-external-drop-${System.nanoTime()}")

        val entries = externalFileEntriesFromUris(
            uris = listOf(missing.toURI().toString(), "https://example.invalid/drop.txt", "not a uri"),
            metadata = fileSystem::metadata,
        )

        assertTrue(entries.isEmpty())
    }
}
