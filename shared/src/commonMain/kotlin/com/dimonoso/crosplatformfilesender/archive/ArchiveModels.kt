package com.dimonoso.crosplatformfilesender.archive

import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem

enum class ArchiveFormat {
    ZIP,
}

data class CreateArchiveRequest(
    val sourcePaths: List<String>,
    val destinationArchivePath: String,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val baseDirectoryPath: String? = null,
)

data class ExtractArchiveRequest(
    val archivePath: String,
    val destinationDirectoryPath: String,
    val format: ArchiveFormat = ArchiveFormat.ZIP,
)

sealed interface ArchiveOperationResult {
    data class Success(val outputPath: String) : ArchiveOperationResult

    data class Failure(
        val format: ArchiveFormat,
        val message: String,
    ) : ArchiveOperationResult

    data class NotImplementedYet(
        val format: ArchiveFormat,
        val message: String,
    ) : ArchiveOperationResult
}

interface ArchiveService {
    val supportedFormats: Set<ArchiveFormat>

    fun createArchive(request: CreateArchiveRequest): ArchiveOperationResult

    fun extractArchive(request: ExtractArchiveRequest): ArchiveOperationResult
}

class StubArchiveService : ArchiveService {
    override val supportedFormats: Set<ArchiveFormat> = setOf(ArchiveFormat.ZIP)

    override fun createArchive(request: CreateArchiveRequest): ArchiveOperationResult =
        ArchiveOperationResult.NotImplementedYet(
            format = request.format,
            message = "ZIP archive creation is scaffolded only.",
        )

    override fun extractArchive(request: ExtractArchiveRequest): ArchiveOperationResult =
        ArchiveOperationResult.NotImplementedYet(
            format = request.format,
            message = "ZIP archive extraction is scaffolded only.",
        )
}

expect fun createArchiveService(platformFileSystem: PlatformFileSystem): ArchiveService
