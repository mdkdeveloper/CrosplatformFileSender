package com.dimonoso.crosplatformfilesender.remote

internal interface RemoteFileCatalogServer {
    fun close()
}

internal interface RemoteFileCatalogTransport {
    suspend fun request(
        host: String,
        port: Int,
        request: RemoteFileCatalogRequest,
    ): RemoteFileCatalogResponse

    fun startServer(
        port: Int,
        handler: suspend (RemoteFileCatalogRequest) -> RemoteFileCatalogResponse,
    ): RemoteFileCatalogServer
}

internal expect fun createRemoteFileCatalogTransport(): RemoteFileCatalogTransport
