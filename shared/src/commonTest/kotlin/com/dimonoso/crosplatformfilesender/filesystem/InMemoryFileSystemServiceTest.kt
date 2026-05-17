package com.dimonoso.crosplatformfilesender.filesystem

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InMemoryFileSystemServiceTest {
    @Test
    fun whitelistFiltersRemoteBrowseToEnabledFolders() {
        val service = InMemoryFileSystemService(
            FakePlatformFileSystem(
                roots = listOf(
                    FileEntry(path = "/shared", name = "shared", type = FileEntryType.Directory),
                    FileEntry(path = "/private", name = "private", type = FileEntryType.Directory),
                ),
                listings = mapOf(
                    "/shared" to listOf(
                        FileEntry(path = "/shared/a.txt", name = "a.txt", type = FileEntryType.File),
                    ),
                    "/private" to listOf(
                        FileEntry(path = "/private/secret.txt", name = "secret.txt", type = FileEntryType.File),
                    ),
                ),
            ),
        )
        val peer = DiscoveredDevice(
            id = "peer-1",
            displayName = "Peer",
            platformName = "Desktop",
            host = "192.168.1.5",
            port = 47777,
            lastSeenEpochMillis = 1L,
        )

        service.addWhitelistFolder(
            WhitelistFolder(
                id = "shared",
                displayName = "Shared",
                path = "/shared",
            ),
        )

        assertEquals(listOf("/shared"), service.browseRemote(peer).map { it.path })
        assertEquals(listOf("/shared/a.txt"), service.browseRemote(peer, "/shared").map { it.path })
        assertTrue(service.browseRemote(peer, "/private").isEmpty())

        service.setWhitelistEnabled("shared", enabled = false)

        assertTrue(service.browseRemote(peer).isEmpty())
    }

    @Test
    fun whitelistedBrowseResultFailsForPathsOutsideWhitelist() {
        val service = InMemoryFileSystemService(
            FakePlatformFileSystem(
                roots = emptyList(),
                listings = mapOf(
                    "/shared" to listOf(
                        FileEntry(path = "/shared/a.txt", name = "a.txt", type = FileEntryType.File),
                    ),
                ),
            ),
        )

        service.addWhitelistFolder(
            WhitelistFolder(
                id = "shared",
                displayName = "Shared",
                path = "/shared",
            ),
        )

        assertIs<WhitelistedBrowseResult.Success>(service.browseWhitelistedResult("/shared"))
        assertIs<WhitelistedBrowseResult.Failure>(service.browseWhitelistedResult("/private"))
    }
}

private class FakePlatformFileSystem(
    private val roots: List<FileEntry>,
    private val listings: Map<String, List<FileEntry>>,
) : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    override fun roots(): List<FileEntry> = roots

    override fun list(path: String): List<FileEntry> = listings[path].orEmpty()
}
