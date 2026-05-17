package com.dimonoso.crosplatformfilesender.archive

import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import com.dimonoso.crosplatformfilesender.platform.PlatformReadStream
import com.dimonoso.crosplatformfilesender.platform.PlatformWriteStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual fun createArchiveService(platformFileSystem: PlatformFileSystem): ArchiveService =
    JavaZipArchiveService(platformFileSystem)

private class JavaZipArchiveService(
    private val fileSystem: PlatformFileSystem,
) : ArchiveService {
    override val supportedFormats: Set<ArchiveFormat> = setOf(ArchiveFormat.ZIP)

    override fun createArchive(request: CreateArchiveRequest): ArchiveOperationResult {
        if (request.format != ArchiveFormat.ZIP) {
            return ArchiveOperationResult.Failure(request.format, "Unsupported archive format.")
        }

        return runCatching {
            fileSystem.parentPath(request.destinationArchivePath)?.let(fileSystem::createDirectories)
            fileSystem.openWrite(request.destinationArchivePath).usePlatformOutput { output ->
                ZipOutputStream(output).use { zip ->
                    zip.setLevel(0)
                    request.sourcePaths.forEach { sourcePath ->
                        val source = fileSystem.metadata(sourcePath) ?: return@forEach
                        val relativeRoot = relativeArchivePath(sourcePath, source.name, request.baseDirectoryPath)
                        addEntry(zip, sourcePath, relativeRoot)
                    }
                }
            }
            ArchiveOperationResult.Success(request.destinationArchivePath)
        }.getOrElse { exception ->
            ArchiveOperationResult.Failure(request.format, exception.readableMessage())
        }
    }

    override fun extractArchive(request: ExtractArchiveRequest): ArchiveOperationResult {
        if (request.format != ArchiveFormat.ZIP) {
            return ArchiveOperationResult.Failure(request.format, "Unsupported archive format.")
        }

        return runCatching {
            fileSystem.createDirectories(request.destinationDirectoryPath)
            fileSystem.openRead(request.archivePath).usePlatformInput { input ->
                ZipInputStream(input).use { zip ->
                    val buffer = ByteArray(CopyBufferSize)
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val safeName = entry.name.safeZipEntryName() ?: continue
                        val targetPath = safeName.split('/')
                            .filter { it.isNotBlank() }
                            .fold(request.destinationDirectoryPath) { current, segment ->
                                fileSystem.childPath(current, segment)
                            }
                        if (entry.isDirectory) {
                            fileSystem.createDirectories(targetPath)
                        } else {
                            fileSystem.parentPath(targetPath)?.let(fileSystem::createDirectories)
                            fileSystem.openWrite(targetPath).usePlatformOutput { output ->
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            ArchiveOperationResult.Success(request.destinationDirectoryPath)
        }.getOrElse { exception ->
            ArchiveOperationResult.Failure(request.format, exception.readableMessage())
        }
    }

    private fun addEntry(zip: ZipOutputStream, sourcePath: String, relativePath: String) {
        val entry = fileSystem.metadata(sourcePath) ?: return
        val normalizedRelativePath = relativePath.normalizeZipName()
        when (entry.type) {
            FileEntryType.Directory, FileEntryType.Drive -> {
                if (normalizedRelativePath.isNotBlank()) {
                    zip.putNextEntry(ZipEntry("$normalizedRelativePath/"))
                    zip.closeEntry()
                }
                fileSystem.list(sourcePath).forEach { child ->
                    addEntry(zip, child.path, listOf(normalizedRelativePath, child.name).filter { it.isNotBlank() }.joinToString("/"))
                }
            }
            FileEntryType.File, FileEntryType.Unknown -> {
                val stats = fileStats(sourcePath)
                val zipEntry = ZipEntry(normalizedRelativePath).apply {
                    method = ZipEntry.STORED
                    size = stats.sizeBytes
                    compressedSize = stats.sizeBytes
                    crc = stats.crc
                }
                zip.putNextEntry(zipEntry)
                fileSystem.openRead(sourcePath).usePlatformInput { input ->
                    input.copyTo(zip)
                }
                zip.closeEntry()
            }
        }
    }

    private fun fileStats(path: String): StoredFileStats {
        val crc = CRC32()
        var total = 0L
        val buffer = ByteArray(CopyBufferSize)
        fileSystem.openRead(path).usePlatformInput { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                crc.update(buffer, 0, read)
                total += read
            }
        }
        return StoredFileStats(sizeBytes = total, crc = crc.value)
    }

    private fun relativeArchivePath(path: String, fallbackName: String, baseDirectoryPath: String?): String {
        val normalizedPath = path.normalizePath()
        val normalizedBase = baseDirectoryPath?.normalizePath()?.trimEnd('/')
        return if (normalizedBase != null && normalizedPath.startsWith("$normalizedBase/")) {
            normalizedPath.removePrefix("$normalizedBase/").ifBlank { fallbackName }
        } else {
            fallbackName
        }
    }
}

private data class StoredFileStats(
    val sizeBytes: Long,
    val crc: Long,
)

private fun PlatformReadStream.usePlatformInput(block: (InputStream) -> Unit) {
    PlatformInputStream(this).use(block)
}

private fun PlatformWriteStream.usePlatformOutput(block: (OutputStream) -> Unit) {
    PlatformOutputStream(this).use(block)
}

private class PlatformInputStream(
    private val stream: PlatformReadStream,
) : InputStream() {
    override fun read(): Int {
        val one = ByteArray(1)
        val read = read(one, 0, 1)
        return if (read < 0) -1 else one[0].toInt() and 0xff
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        stream.read(buffer, offset, length)

    override fun close() {
        stream.close()
    }
}

private class PlatformOutputStream(
    private val stream: PlatformWriteStream,
) : OutputStream() {
    override fun write(value: Int) {
        write(byteArrayOf(value.toByte()), 0, 1)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        stream.write(buffer, offset, length)
    }

    override fun close() {
        stream.close()
    }
}

private fun InputStream.copyTo(output: OutputStream) {
    val buffer = ByteArray(CopyBufferSize)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        output.write(buffer, 0, read)
    }
}

private fun String.normalizeZipName(): String =
    replace('\\', '/').trim('/').safeZipEntryName().orEmpty()

private fun String.safeZipEntryName(): String? {
    val normalized = replace('\\', '/').trimStart('/')
    if (normalized.isBlank()) return ""
    if (normalized.split('/').any { it == ".." }) return null
    return normalized
}

private fun String.normalizePath(): String =
    replace('\\', '/').trimEnd('/')

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"

private const val CopyBufferSize = 1024 * 1024
