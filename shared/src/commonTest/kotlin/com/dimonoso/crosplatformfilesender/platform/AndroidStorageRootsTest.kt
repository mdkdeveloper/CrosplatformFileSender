package com.dimonoso.crosplatformfilesender.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidStorageRootsTest {
    @Test
    fun primaryEmulatedStorageIsRetainedFirst() {
        val entries = androidStorageRootEntries(
            listOf(
                AndroidStorageRootCandidate(
                    path = "/storage/1234-5678",
                    displayName = "SD card",
                ),
                AndroidStorageRootCandidate(
                    path = "/storage/emulated/0",
                    displayName = "Internal storage",
                    isPrimary = true,
                ),
            ),
        )

        assertEquals(listOf("/storage/emulated/0", "/storage/1234-5678"), entries.map { it.path })
    }

    @Test
    fun appSpecificExternalDirectoryIsTrimmedToVolumeRoot() {
        val root = androidSharedStorageRootPath(
            path = "/storage/1234-5678/Android/data/com.example/files",
            packageName = "com.example",
        )

        assertEquals("/storage/1234-5678", root)
    }

    @Test
    fun duplicateRootsAreRemoved() {
        val appSpecificRoot = androidSharedStorageRootPath(
            path = "/storage/emulated/0/Android/data/com.example/files",
            packageName = "com.example",
        )

        assertEquals("/storage/emulated/0", appSpecificRoot)

        val entries = androidStorageRootEntries(
            listOf(
                AndroidStorageRootCandidate(
                    path = "/storage/emulated/0",
                    displayName = "Internal storage",
                    isPrimary = true,
                ),
                AndroidStorageRootCandidate(
                    path = appSpecificRoot ?: "",
                    displayName = "Duplicate",
                ),
            ),
        )

        assertEquals(listOf("/storage/emulated/0"), entries.map { it.path })
    }

    @Test
    fun missingAndUnmountedRootsAreExcluded() {
        val entries = androidStorageRootEntries(
            listOf(
                AndroidStorageRootCandidate(path = "/storage/emulated/0", isPrimary = true),
                AndroidStorageRootCandidate(path = "/storage/missing", exists = false),
                AndroidStorageRootCandidate(path = "/storage/unmounted", isMounted = false),
            ),
        )

        assertEquals(listOf("/storage/emulated/0"), entries.map { it.path })
    }
}
