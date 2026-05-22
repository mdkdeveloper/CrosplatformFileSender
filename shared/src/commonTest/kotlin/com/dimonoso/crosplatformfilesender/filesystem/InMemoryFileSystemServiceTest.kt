package com.dimonoso.crosplatformfilesender.filesystem

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.platform.PlatformFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun deletePolicyUsesNearestEnabledExplicitWhitelistOverride() {
        val service = InMemoryFileSystemService(FakePlatformFileSystem(emptyList(), emptyMap()))
        service.addWhitelistFolder(
            WhitelistFolder(
                id = "outer",
                displayName = "Outer",
                path = "/shared",
                deletePolicyOverride = WhitelistDeletePolicyOverride.Ask,
            ),
        )
        service.addWhitelistFolder(
            WhitelistFolder(
                id = "inner",
                displayName = "Inner",
                path = "/shared/projects",
            ),
        )

        assertEquals(RemoteDeletePolicy.Ask, service.effectiveRemoteDeletePolicy("/shared/projects/a.txt", RemoteDeletePolicy.DoNothing))

        service.setWhitelistDeletePolicyOverride("inner", WhitelistDeletePolicyOverride.Permanent)

        assertEquals(RemoteDeletePolicy.Permanent, service.effectiveRemoteDeletePolicy("/shared/projects/a.txt", RemoteDeletePolicy.DoNothing))

        service.setWhitelistEnabled("inner", enabled = false)

        assertEquals(RemoteDeletePolicy.Ask, service.effectiveRemoteDeletePolicy("/shared/projects/a.txt", RemoteDeletePolicy.DoNothing))
        assertEquals(null, service.effectiveRemoteDeletePolicy("/private/a.txt", RemoteDeletePolicy.Permanent))
    }

    @Test
    fun trashDeleteRefusesWhitelistRootsAndUnsupportedItemsWithoutPermanentFallback() {
        val platform = FakePlatformFileSystem(
            roots = listOf(FileEntry(path = "/", name = "/", type = FileEntryType.Drive)),
            listings = mapOf(
                "/shared" to listOf(FileEntry(path = "/shared/a.txt", name = "a.txt", type = FileEntryType.File)),
                "/local" to listOf(FileEntry(path = "/local/a.txt", name = "a.txt", type = FileEntryType.File)),
            ),
            trashablePaths = setOf("/local/a.txt"),
        )
        val service = InMemoryFileSystemService(platform)
        service.addWhitelistFolder(WhitelistFolder(id = "shared", displayName = "Shared", path = "/shared"))

        val result = service.deleteLocalToTrash(listOf("/shared", "/local/a.txt", "/shared/a.txt"))

        assertEquals(
            listOf(FileDeleteStatus.Refused, FileDeleteStatus.Deleted, FileDeleteStatus.Refused),
            result.results.map { it.status },
        )
        assertEquals(listOf("/local/a.txt"), platform.trashedPaths)
        assertTrue(platform.deletedPaths.isEmpty())
    }

    @Test
    fun permanentRemoteDeleteIsRecursiveInsideWhitelistOnly() {
        val platform = FakePlatformFileSystem(
            roots = emptyList(),
            listings = mapOf(
                "/shared" to listOf(FileEntry(path = "/shared/folder", name = "folder", type = FileEntryType.Directory)),
            ),
        )
        val service = InMemoryFileSystemService(platform)
        service.addWhitelistFolder(WhitelistFolder(id = "shared", displayName = "Shared", path = "/shared"))

        val result = service.deleteWhitelisted(listOf("/shared/folder", "/private/folder", "/shared"), FileDeleteMode.Permanent)

        assertEquals(FileDeleteStatus.Deleted, result.results[0].status)
        assertEquals(FileDeleteStatus.Refused, result.results[1].status)
        assertEquals(FileDeleteStatus.Refused, result.results[2].status)
        assertEquals(listOf("/shared/folder" to true), platform.deletedPaths)
        assertFalse(result.results[1].message.isBlank())
    }
}

private class FakePlatformFileSystem(
    private val roots: List<FileEntry>,
    private val listings: Map<String, List<FileEntry>>,
    private val trashablePaths: Set<String> = emptySet(),
) : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.FullFileSystem

    val trashedPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<Pair<String, Boolean>>()

    override fun roots(): List<FileEntry> = roots

    override fun list(path: String): List<FileEntry> = listings[path].orEmpty()

    override fun metadata(path: String): FileEntry? =
        roots.firstOrNull { it.path == path }
            ?: listings.values.flatten().firstOrNull { it.path == path }
            ?: listings.keys.firstOrNull { it == path }?.let { folderPath ->
                FileEntry(path = folderPath, name = folderPath.substringAfterLast('/'), type = FileEntryType.Directory)
            }

    override fun canMoveToTrash(path: String): Boolean = path in trashablePaths

    override fun moveToTrash(path: String): Boolean {
        if (!canMoveToTrash(path)) return false
        trashedPaths += path
        return true
    }

    override fun delete(path: String, recursive: Boolean): Boolean {
        if (metadata(path) == null) return false
        deletedPaths += path to recursive
        return true
    }
}
