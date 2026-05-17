package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import java.net.ServerSocket
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RemoteFileCatalogTransportJvmTest {
    @Test
    fun transportRequestsEntriesFromServer() = runBlocking {
        val transport = createRemoteFileCatalogTransport()
        val port = freePort()
        val server = transport.startServer(port) {
            RemoteFileCatalogResponse.Success(
                listOf(
                    FileEntry(
                        path = "/shared",
                        name = "Shared",
                        type = FileEntryType.Directory,
                    ),
                ),
            )
        }

        try {
            val response = transport.request(
                host = "127.0.0.1",
                port = port,
                request = RemoteFileCatalogRequest(
                    keywordFingerprint = "fingerprint",
                    path = null,
                ),
            )

            assertEquals(listOf("/shared"), assertIs<RemoteFileCatalogResponse.Success>(response).entries.map { it.path })
        } finally {
            server.close()
        }
    }

    private fun freePort(): Int =
        ServerSocket(0).use { socket -> socket.localPort }
}
