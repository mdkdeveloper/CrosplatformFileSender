package com.dimonoso.crosplatformfilesender.transfer

import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal actual fun createFileTransferTransport(): FileTransferTransport =
    JavaFileTransferTransport()

private class JavaFileTransferTransport : FileTransferTransport {
    override suspend fun upload(
        host: String,
        port: Int,
        request: FileTransferRequest,
        fileSystem: PlatformFileSystem,
        onProgress: (Long) -> Unit,
    ): FileTransferResponse =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.soTimeout = ReadTimeoutMillis
                socket.connect(InetSocketAddress(host, port), ConnectTimeoutMillis)
                val output = socket.getOutputStream()
                output.write(FileTransferProtocol.encodeRequest(request).encodeToByteArray())
                fileSystem.openRead(request.sourcePath).use { input ->
                    val buffer = ByteArray(TransferChunkBytes)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        onProgress(total)
                    }
                }
                output.flush()
                socket.shutdownOutput()
                FileTransferProtocol.decodeResponse(socket.getInputStream().readHeader())
                    ?: FileTransferResponse(success = false, message = "Malformed transfer response.")
            }
        }

    override suspend fun download(
        host: String,
        port: Int,
        request: FileTransferRequest,
        fileSystem: PlatformFileSystem,
        destinationPath: String,
        onProgress: (Long) -> Unit,
    ): FileTransferResponse =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.soTimeout = ReadTimeoutMillis
                socket.connect(InetSocketAddress(host, port), ConnectTimeoutMillis)
                val output = socket.getOutputStream()
                output.write(FileTransferProtocol.encodeRequest(request).encodeToByteArray())
                output.flush()
                socket.shutdownOutput()
                val input = socket.getInputStream()
                val response = FileTransferProtocol.decodeResponse(input.readHeader())
                    ?: return@withContext FileTransferResponse(success = false, message = "Malformed transfer response.")
                if (!response.success) return@withContext response

                fileSystem.openWrite(destinationPath).use { output ->
                    input.copyToPlatform(output, response.sizeBytes, onProgress)
                }
                response
            }
        }

    override fun startServer(
        port: Int,
        fileSystem: PlatformFileSystem,
        handler: FileTransferServerHandler,
    ): FileTransferServer {
        val socket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(port))
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            while (isActive) {
                val client = try {
                    socket.accept()
                } catch (_: SocketException) {
                    break
                }
                launch {
                    client.use { accepted ->
                        handleClient(accepted, fileSystem, handler)
                    }
                }
            }
        }

        return object : FileTransferServer {
            override fun close() {
                runCatching { socket.close() }
                scope.cancel()
            }
        }
    }

    private suspend fun handleClient(
        socket: Socket,
        fileSystem: PlatformFileSystem,
        handler: FileTransferServerHandler,
    ) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val request = FileTransferProtocol.decodeRequest(input.readHeader())
        if (request == null) {
            output.write(FileTransferProtocol.encodeResponse(FileTransferResponse(false, "Malformed transfer request.")).encodeToByteArray())
            output.flush()
            return
        }

        when (request.type) {
            FileTransferRequestType.Upload -> {
                val response = runCatching {
                    handler.receiveUpload(request, StreamTransferInput(input))
                }.getOrElse { exception ->
                    FileTransferResponse(false, exception.readableMessage())
                }
                output.write(FileTransferProtocol.encodeResponse(response).encodeToByteArray())
                output.flush()
            }
            FileTransferRequestType.Download -> {
                val prepared = runCatching {
                    handler.prepareDownload(request)
                }.getOrElse { exception ->
                    PreparedDownload(FileTransferResponse(false, exception.readableMessage()))
                }
                output.write(FileTransferProtocol.encodeResponse(prepared.response).encodeToByteArray())
                if (prepared.response.success && prepared.sourcePath != null) {
                    fileSystem.openRead(prepared.sourcePath).use { source ->
                        val buffer = ByteArray(TransferChunkBytes)
                        while (true) {
                            val read = source.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                        }
                    }
                }
                output.flush()
            }
        }
    }
}

private class StreamTransferInput(
    private val input: InputStream,
) : TransferInput {
    override suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        input.read(buffer, offset, length)
}

private fun InputStream.readHeader(): String {
    val bytes = mutableListOf<Byte>()
    var previousWasNewLine = false
    while (true) {
        val value = read()
        if (value < 0) break
        if (value.toChar() != '\r') {
            bytes += value.toByte()
        }
        if (value.toChar() == '\n') {
            if (previousWasNewLine) break
            previousWasNewLine = true
        } else if (value.toChar() != '\r') {
            previousWasNewLine = false
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun InputStream.copyToPlatform(
    output: com.dimonoso.crosplatformfilesender.platform.PlatformWriteStream,
    expectedBytes: Long,
    onProgress: (Long) -> Unit,
) {
    val buffer = ByteArray(TransferChunkBytes)
    var total = 0L
    while (expectedBytes <= 0L || total < expectedBytes) {
        val remaining = if (expectedBytes <= 0L) buffer.size else minOf(buffer.size.toLong(), expectedBytes - total).toInt()
        val read = read(buffer, 0, remaining)
        if (read < 0) break
        output.write(buffer, 0, read)
        total += read
        onProgress(total)
    }
}

private const val ConnectTimeoutMillis = 3_000
private const val ReadTimeoutMillis = 60_000

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"
