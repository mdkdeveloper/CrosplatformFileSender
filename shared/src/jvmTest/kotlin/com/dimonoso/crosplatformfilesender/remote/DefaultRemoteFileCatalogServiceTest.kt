package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.discovery.DiscoveryConfig
import com.dimonoso.crosplatformfilesender.discovery.discoveryKeywordFingerprint
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.InMemoryFileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultRemoteFileCatalogServiceTest {
    @Test
    fun serverReturnsWhitelistRootsAndNestedEntries() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val service = DefaultRemoteFileCatalogService(
            fileSystem = testFileSystem(),
            keywordProvider = { "local-secret" },
            transport = transport,
        )

        service.startServer(
            DiscoveryConfig(
                keyword = "local-secret",
                deviceAlias = "Test",
            ),
        )

        val roots = transport.request(
            host = "127.0.0.1",
            port = 47777,
            request = RemoteFileCatalogRequest(
                keywordFingerprint = discoveryKeywordFingerprint("local-secret"),
                path = null,
            ),
        )
        val nested = transport.request(
            host = "127.0.0.1",
            port = 47777,
            request = RemoteFileCatalogRequest(
                keywordFingerprint = discoveryKeywordFingerprint("local-secret"),
                path = "/shared",
            ),
        )

        assertEquals(listOf("/shared"), assertIs<RemoteFileCatalogResponse.Success>(roots).entries.map { it.path })
        assertEquals(listOf("/shared/a.txt"), assertIs<RemoteFileCatalogResponse.Success>(nested).entries.map { it.path })
    }

    @Test
    fun serverRejectsPathsOutsideWhitelist() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val service = DefaultRemoteFileCatalogService(
            fileSystem = testFileSystem(),
            keywordProvider = { "local-secret" },
            transport = transport,
        )

        service.startServer(
            DiscoveryConfig(
                keyword = "local-secret",
                deviceAlias = "Test",
            ),
        )

        val response = transport.request(
            host = "127.0.0.1",
            port = 47777,
            request = RemoteFileCatalogRequest(
                keywordFingerprint = discoveryKeywordFingerprint("local-secret"),
                path = "/private",
            ),
        )

        val failure = assertIs<RemoteFileCatalogResponse.Failure>(response)
        assertEquals(RemoteFileCatalogErrorCode.OutsideWhitelist, failure.error.code)
    }

    @Test
    fun serverRejectsWrongKeyword() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val service = DefaultRemoteFileCatalogService(
            fileSystem = testFileSystem(),
            keywordProvider = { "local-secret" },
            transport = transport,
        )

        service.startServer(
            DiscoveryConfig(
                keyword = "local-secret",
                deviceAlias = "Test",
            ),
        )

        val response = transport.request(
            host = "127.0.0.1",
            port = 47777,
            request = RemoteFileCatalogRequest(
                keywordFingerprint = discoveryKeywordFingerprint("other-secret"),
                path = null,
            ),
        )

        val failure = assertIs<RemoteFileCatalogResponse.Failure>(response)
        assertEquals(RemoteFileCatalogErrorCode.Unauthorized, failure.error.code)
    }

    private fun testFileSystem(): InMemoryFileSystemService {
        val service = InMemoryFileSystemService(
            object : PlatformFileSystem {
                override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

                override fun roots(): List<FileEntry> = emptyList()

                override fun list(path: String): List<FileEntry> =
                    when (path) {
                        "/shared" -> listOf(
                            FileEntry(
                                path = "/shared/a.txt",
                                name = "a.txt",
                                type = FileEntryType.File,
                            ),
                        )
                        else -> emptyList()
                    }
            },
        )
        service.addWhitelistFolder(
            WhitelistFolder(
                id = "shared",
                displayName = "Shared",
                path = "/shared",
            ),
        )
        return service
    }
}

private class InMemoryRemoteFileCatalogTransport : RemoteFileCatalogTransport {
    private var handler: (suspend (RemoteFileCatalogRequest) -> RemoteFileCatalogResponse)? = null

    override suspend fun request(
        host: String,
        port: Int,
        request: RemoteFileCatalogRequest,
    ): RemoteFileCatalogResponse =
        handler?.invoke(request)
            ?: RemoteFileCatalogResponse.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.ServerUnavailable,
                    message = "Server is not running.",
                ),
            )

    override fun startServer(
        port: Int,
        handler: suspend (RemoteFileCatalogRequest) -> RemoteFileCatalogResponse,
    ): RemoteFileCatalogServer {
        this.handler = handler
        return object : RemoteFileCatalogServer {
            override fun close() {
                this@InMemoryRemoteFileCatalogTransport.handler = null
            }
        }
    }
}
