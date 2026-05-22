package com.dimonoso.crosplatformfilesender.remote

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
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

internal actual fun createRemoteFileCatalogTransport(): RemoteFileCatalogTransport =
    AndroidRemoteFileCatalogTransport()

private class AndroidRemoteFileCatalogTransport : RemoteFileCatalogTransport {
    override suspend fun request(
        host: String,
        port: Int,
        request: RemoteFileCatalogRequest,
    ): RemoteFileCatalogResponse =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.soTimeout = request.readTimeoutMillis()
                socket.connect(InetSocketAddress(host, port), ConnectTimeoutMillis)

                val writer = socket.writer()
                writer.write(RemoteFileCatalogProtocol.encodeRequest(request))
                writer.flush()
                socket.shutdownOutput()

                val payload = socket.reader().readMessage()
                RemoteFileCatalogProtocol.decodeResponse(payload)
                    ?: RemoteFileCatalogResponse.Failure(
                        RemoteFileCatalogError(
                            code = RemoteFileCatalogErrorCode.BadRequest,
                            message = "Malformed catalog response.",
                        ),
                    )
            }
        }

    override fun startServer(
        port: Int,
        handler: suspend (RemoteFileCatalogRequest) -> RemoteFileCatalogResponse,
    ): RemoteFileCatalogServer {
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
                        val response = accepted.reader()
                            .readMessage()
                            .let(RemoteFileCatalogProtocol::decodeRequest)
                            ?.let { request ->
                                runCatching { handler(request) }.getOrElse { exception ->
                                    RemoteFileCatalogResponse.Failure(
                                        RemoteFileCatalogError(
                                            code = RemoteFileCatalogErrorCode.ServerUnavailable,
                                            message = exception.readableMessage(),
                                        ),
                                    )
                                }
                            }
                            ?: RemoteFileCatalogResponse.Failure(
                                RemoteFileCatalogError(
                                    code = RemoteFileCatalogErrorCode.BadRequest,
                                    message = "Malformed catalog request.",
                                ),
                            )

                        accepted.writer().use { writer ->
                            writer.write(RemoteFileCatalogProtocol.encodeResponse(response))
                            writer.flush()
                        }
                    }
                }
            }
        }

        return object : RemoteFileCatalogServer {
            override fun close() {
                runCatching { socket.close() }
                scope.cancel()
            }
        }
    }
}

private fun Socket.reader(): BufferedReader =
    BufferedReader(InputStreamReader(getInputStream(), Charsets.UTF_8))

private fun Socket.writer(): PrintWriter =
    PrintWriter(OutputStreamWriter(getOutputStream(), Charsets.UTF_8))

private fun BufferedReader.readMessage(): String =
    buildString {
        while (true) {
            val line = readLine() ?: break
            if (line.isEmpty()) break
            appendLine(line)
        }
    }

private fun Throwable.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"

private const val ConnectTimeoutMillis = 3_000
private const val ReadTimeoutMillis = 5_000
private const val DeleteReadTimeoutMillis = 65_000

private fun RemoteFileCatalogRequest.readTimeoutMillis(): Int =
    if (operation == RemoteFileCatalogOperation.Delete) DeleteReadTimeoutMillis else ReadTimeoutMillis
