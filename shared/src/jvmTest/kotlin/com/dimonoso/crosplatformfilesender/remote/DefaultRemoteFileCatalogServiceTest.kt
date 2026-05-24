package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.discovery.DiscoveryConfig
import com.dimonoso.crosplatformfilesender.discovery.discoveryKeywordFingerprint
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteStatus
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.InMemoryFileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistDeletePolicyOverride
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

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

    @Test
    fun serverDeletesEntriesWithWhitelistPermanentPolicy() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val platform = DeleteTestPlatformFileSystem()
        val fileSystem = deleteTestFileSystem(platform, WhitelistDeletePolicyOverride.Permanent)
        val service = DefaultRemoteFileCatalogService(
            fileSystem = fileSystem,
            keywordProvider = { "local-secret" },
            transport = transport,
        )
        service.startServer(DiscoveryConfig(keyword = "local-secret", deviceAlias = "Test"))

        val response = transport.request(
            host = "127.0.0.1",
            port = 47777,
            request = deleteRequest("/shared/a.txt"),
        )

        val deleteResponse = assertIs<RemoteFileCatalogResponse.DeleteCompleted>(response)
        assertEquals(FileDeleteStatus.Deleted, deleteResponse.result.results.single().status)
        assertEquals(listOf("/shared/a.txt" to true), platform.deletedPaths)
    }

    @Test
    fun askPolicyWaitsForOwnerDecision() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val platform = DeleteTestPlatformFileSystem()
        val service = DefaultRemoteFileCatalogService(
            fileSystem = deleteTestFileSystem(platform, WhitelistDeletePolicyOverride.Ask),
            keywordProvider = { "local-secret" },
            transport = transport,
        )
        service.startServer(DiscoveryConfig(keyword = "local-secret", deviceAlias = "Test"))

        val response = async { transport.request("127.0.0.1", 47777, deleteRequest("/shared/a.txt")) }
        yield()
        val prompt = assertNotNull(service.pendingDeletePrompt.value)

        service.resolvePendingDeletePrompt(prompt.id, RemoteDeletePromptDecision.Permanent)

        val deleteResponse = assertIs<RemoteFileCatalogResponse.DeleteCompleted>(response.await())
        assertEquals(FileDeleteStatus.Deleted, deleteResponse.result.results.single().status)
        assertEquals(listOf("/shared/a.txt" to true), platform.deletedPaths)
    }

    @Test
    fun askPolicyTimeoutRefusesDelete() = runBlocking {
        val transport = InMemoryRemoteFileCatalogTransport()
        val platform = DeleteTestPlatformFileSystem()
        val service = DefaultRemoteFileCatalogService(
            fileSystem = deleteTestFileSystem(platform, WhitelistDeletePolicyOverride.Ask),
            keywordProvider = { "local-secret" },
            transport = transport,
            deletePromptTimeoutMillis = 1L,
        )
        service.startServer(DiscoveryConfig(keyword = "local-secret", deviceAlias = "Test"))

        val response = transport.request("127.0.0.1", 47777, deleteRequest("/shared/a.txt"))

        val deleteResponse = assertIs<RemoteFileCatalogResponse.DeleteCompleted>(response)
        assertEquals(FileDeleteStatus.Refused, deleteResponse.result.results.single().status)
        assertEquals(emptyList(), platform.deletedPaths)
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

                override fun metadata(path: String): FileEntry? =
                    when (path) {
                        "/shared" -> FileEntry(path = "/shared", name = "Shared", type = FileEntryType.Directory)
                        "/shared/a.txt" -> FileEntry(path = "/shared/a.txt", name = "a.txt", type = FileEntryType.File)
                        else -> null
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

    private fun deleteTestFileSystem(
        platform: DeleteTestPlatformFileSystem,
        policy: WhitelistDeletePolicyOverride,
    ): InMemoryFileSystemService =
        InMemoryFileSystemService(platform).apply {
            addWhitelistFolder(
                WhitelistFolder(
                    id = "shared",
                    displayName = "Shared",
                    path = "/shared",
                    deletePolicyOverride = policy,
                ),
            )
        }

    private fun deleteRequest(path: String): RemoteFileCatalogRequest =
        RemoteFileCatalogRequest(
            keywordFingerprint = discoveryKeywordFingerprint("local-secret"),
            operation = RemoteFileCatalogOperation.Delete,
            deletePaths = listOf(path),
        )
}

private class DeleteTestPlatformFileSystem : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    val deletedPaths = mutableListOf<Pair<String, Boolean>>()

    override fun roots(): List<FileEntry> = emptyList()

    override fun list(path: String): List<FileEntry> =
        if (path == "/shared") {
            listOf(FileEntry(path = "/shared/a.txt", name = "a.txt", type = FileEntryType.File))
        } else {
            emptyList()
        }

    override fun metadata(path: String): FileEntry? =
        when (path) {
            "/shared" -> FileEntry(path, "Shared", FileEntryType.Directory)
            "/shared/a.txt" -> FileEntry(path, "a.txt", FileEntryType.File)
            else -> null
        }

    override fun delete(path: String, recursive: Boolean): Boolean {
        if (metadata(path) == null) return false
        deletedPaths += path to recursive
        return true
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
