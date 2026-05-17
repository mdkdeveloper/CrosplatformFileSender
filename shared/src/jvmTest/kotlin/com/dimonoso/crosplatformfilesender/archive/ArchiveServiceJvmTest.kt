package com.dimonoso.crosplatformfilesender.archive

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import com.dimonoso.crosplatformfilesender.platform.PlatformReadStream
import com.dimonoso.crosplatformfilesender.platform.PlatformWriteStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArchiveServiceJvmTest {
    @Test
    fun zipStoreArchiveRoundTripKeepsNestedFiles() {
        val root = Files.createTempDirectory("cfs-archive-test").toFile()
        val source = File(root, "source").apply { mkdirs() }
        File(source, "a.txt").writeText("alpha")
        File(source, "nested").mkdirs()
        File(source, "nested/b.txt").writeText("beta")
        val archive = File(root, "out.zip")
        val extract = File(root, "extract")
        val fileSystem = TestFileSystem()
        val service = createArchiveService(fileSystem)

        val created = service.createArchive(
            CreateArchiveRequest(
                sourcePaths = listOf(source.absolutePath),
                destinationArchivePath = archive.absolutePath,
                baseDirectoryPath = source.parentFile.absolutePath,
            ),
        )

        assertIs<ArchiveOperationResult.Success>(created)
        ZipFile(archive).use { zip ->
            assertEquals(ZipEntry.STORED, zip.getEntry("source/a.txt").method)
            assertEquals(ZipEntry.STORED, zip.getEntry("source/nested/b.txt").method)
        }

        val extracted = service.extractArchive(
            ExtractArchiveRequest(
                archivePath = archive.absolutePath,
                destinationDirectoryPath = extract.absolutePath,
            ),
        )

        assertIs<ArchiveOperationResult.Success>(extracted)
        assertEquals("alpha", File(extract, "source/a.txt").readText())
        assertEquals("beta", File(extract, "source/nested/b.txt").readText())
    }
}

private class TestFileSystem : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    override fun roots(): List<FileEntry> = emptyList()

    override fun list(path: String): List<FileEntry> =
        File(path).listFiles()?.map { file -> file.toEntry() }.orEmpty()

    override fun metadata(path: String): FileEntry? =
        File(path).takeIf { it.exists() }?.toEntry()

    override fun createDirectories(path: String): Boolean =
        File(path).let { it.exists() && it.isDirectory || it.mkdirs() }

    override fun childPath(parentPath: String, childName: String): String =
        File(parentPath, childName).absolutePath

    override fun parentPath(path: String): String? =
        File(path).parentFile?.absolutePath

    override fun openRead(path: String): PlatformReadStream =
        TestReadStream(FileInputStream(path))

    override fun openWrite(path: String): PlatformWriteStream {
        File(path).parentFile?.mkdirs()
        return TestWriteStream(FileOutputStream(path))
    }

    override fun move(sourcePath: String, targetPath: String, replace: Boolean): Boolean =
        File(sourcePath).renameTo(File(targetPath))

    override fun delete(path: String, recursive: Boolean): Boolean =
        if (recursive) File(path).deleteRecursively() else File(path).delete()

    override fun cacheDirectoryPath(): String =
        System.getProperty("java.io.tmpdir")

    private fun File.toEntry(): FileEntry =
        FileEntry(
            path = absolutePath,
            name = name.ifBlank { absolutePath },
            type = if (isDirectory) FileEntryType.Directory else FileEntryType.File,
            sizeBytes = if (isFile) length() else null,
            isBrowseable = isDirectory,
        )
}

private class TestReadStream(
    private val input: FileInputStream,
) : PlatformReadStream {
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        input.read(buffer, offset, length)

    override fun close() {
        input.close()
    }
}

private class TestWriteStream(
    private val output: FileOutputStream,
) : PlatformWriteStream {
    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        output.write(buffer, offset, length)
    }

    override fun close() {
        output.close()
    }
}
