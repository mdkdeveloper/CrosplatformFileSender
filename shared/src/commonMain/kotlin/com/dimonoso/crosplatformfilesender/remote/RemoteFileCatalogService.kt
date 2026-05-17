package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryConfig
import com.dimonoso.crosplatformfilesender.discovery.discoveryKeywordFingerprint
import com.dimonoso.crosplatformfilesender.filesystem.FileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistedBrowseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface RemoteFileCatalogService {
    val serverError: StateFlow<RemoteFileCatalogError?>

    fun startServer(config: DiscoveryConfig)

    fun stopServer()

    suspend fun browse(
        device: DiscoveredDevice,
        path: String?,
    ): RemoteFileCatalogResult
}

fun createRemoteFileCatalogService(
    fileSystem: FileSystemService,
    keywordProvider: () -> String,
): RemoteFileCatalogService =
    DefaultRemoteFileCatalogService(
        fileSystem = fileSystem,
        keywordProvider = keywordProvider,
        transport = createRemoteFileCatalogTransport(),
    )

internal class DefaultRemoteFileCatalogService(
    private val fileSystem: FileSystemService,
    private val keywordProvider: () -> String,
    private val transport: RemoteFileCatalogTransport,
) : RemoteFileCatalogService {
    private val _serverError = MutableStateFlow<RemoteFileCatalogError?>(null)
    private var server: RemoteFileCatalogServer? = null
    private var serverKeywordFingerprint: String? = null

    override val serverError: StateFlow<RemoteFileCatalogError?> = _serverError

    override fun startServer(config: DiscoveryConfig) {
        stopServer()
        serverKeywordFingerprint = discoveryKeywordFingerprint(config.keyword)
        _serverError.value = null
        server = runCatching {
            transport.startServer(config.udpPort, ::handleRequest)
        }.getOrElse { exception ->
            serverKeywordFingerprint = null
            _serverError.value = RemoteFileCatalogError(
                code = RemoteFileCatalogErrorCode.ServerUnavailable,
                message = exception.readableMessage(),
            )
            null
        }
    }

    override fun stopServer() {
        server?.close()
        server = null
        serverKeywordFingerprint = null
        _serverError.value = null
    }

    override suspend fun browse(
        device: DiscoveredDevice,
        path: String?,
    ): RemoteFileCatalogResult {
        val response = runCatching {
            transport.request(
                host = device.host,
                port = device.port,
                request = RemoteFileCatalogRequest(
                    keywordFingerprint = discoveryKeywordFingerprint(keywordProvider()),
                    path = path,
                ),
            )
        }.getOrElse { exception ->
            return RemoteFileCatalogResult.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.Network,
                    message = exception.readableMessage(),
                ),
            )
        }

        return when (response) {
            is RemoteFileCatalogResponse.Success -> RemoteFileCatalogResult.Success(response.entries)
            is RemoteFileCatalogResponse.Failure -> RemoteFileCatalogResult.Failure(response.error)
        }
    }

    private suspend fun handleRequest(request: RemoteFileCatalogRequest): RemoteFileCatalogResponse {
        if (request.keywordFingerprint != serverKeywordFingerprint) {
            return RemoteFileCatalogResponse.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.Unauthorized,
                    message = "Unauthorized catalog request.",
                ),
            )
        }

        return when (val result = fileSystem.browseWhitelistedResult(request.path)) {
            is WhitelistedBrowseResult.Success -> RemoteFileCatalogResponse.Success(result.entries)
            is WhitelistedBrowseResult.Failure -> RemoteFileCatalogResponse.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.OutsideWhitelist,
                    message = result.message,
                ),
            )
        }
    }

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"
}
