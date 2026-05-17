package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem

internal interface TransferInput {
    suspend fun read(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Int
}

internal interface FileTransferServerHandler {
    suspend fun receiveUpload(request: FileTransferRequest, input: TransferInput): FileTransferResponse

    suspend fun prepareDownload(request: FileTransferRequest): PreparedDownload
}

internal data class PreparedDownload(
    val response: FileTransferResponse,
    val sourcePath: String? = null,
)

internal interface FileTransferServer {
    fun close()
}

internal interface FileTransferTransport {
    suspend fun upload(
        host: String,
        port: Int,
        request: FileTransferRequest,
        fileSystem: PlatformFileSystem,
        onProgress: (Long) -> Unit,
    ): FileTransferResponse

    suspend fun download(
        host: String,
        port: Int,
        request: FileTransferRequest,
        fileSystem: PlatformFileSystem,
        destinationPath: String,
        onProgress: (Long) -> Unit,
    ): FileTransferResponse

    fun startServer(
        port: Int,
        fileSystem: PlatformFileSystem,
        handler: FileTransferServerHandler,
    ): FileTransferServer
}

internal expect fun createFileTransferTransport(): FileTransferTransport
