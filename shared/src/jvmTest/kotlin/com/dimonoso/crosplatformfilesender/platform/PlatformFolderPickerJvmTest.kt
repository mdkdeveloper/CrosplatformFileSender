package com.dimonoso.crosplatformfilesender.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformFolderPickerJvmTest {
    @Test
    fun absoluteDirectoryOutsideCurrentDirectoryNavigatesInsteadOfApproving() {
        val current = Files.createTempDirectory("cfs-picker-current").toFile()
        val target = Files.createTempDirectory("cfs-picker-target").toFile()
        try {
            assertTrue(shouldNavigateToTypedDirectory(current, target))
        } finally {
            current.deleteRecursively()
            target.deleteRecursively()
        }
    }

    @Test
    fun directoryInsideCurrentDirectoryApprovesSelection() {
        val current = Files.createTempDirectory("cfs-picker-current").toFile()
        val child = File(current, "child").apply { mkdirs() }
        try {
            assertFalse(shouldNavigateToTypedDirectory(current, child))
        } finally {
            current.deleteRecursively()
        }
    }

    @Test
    fun nonDirectoryPathApprovesSelectionFlow() {
        val current = Files.createTempDirectory("cfs-picker-current").toFile()
        val file = File(current, "file.txt").apply { writeText("test") }
        try {
            assertFalse(shouldNavigateToTypedDirectory(current, file))
        } finally {
            current.deleteRecursively()
        }
    }
}
