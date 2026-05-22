package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FilePaneDragDropTest {
    @Test
    fun dragSelectedRowIncludesCurrentFileSelection() {
        val entries = testEntries()

        val dragged = filePaneDragEntries(
            entries = entries,
            selectedRowIds = setOf(entries[0].path, entries[2].path),
            draggedRowId = entries[2].path,
        )

        assertEquals(listOf(entries[0], entries[2]), dragged)
    }

    @Test
    fun dragUnselectedRowIncludesOnlyThatEntry() {
        val entries = testEntries()

        val dragged = filePaneDragEntries(
            entries = entries,
            selectedRowIds = setOf(entries[0].path, entries[2].path),
            draggedRowId = entries[1].path,
        )

        assertEquals(listOf(entries[1]), dragged)
    }

    @Test
    fun parentRowDoesNotCreateDragEntries() {
        val dragged = filePaneDragEntries(
            entries = testEntries(),
            selectedRowIds = setOf(ParentEntryRowId),
            draggedRowId = ParentEntryRowId,
        )

        assertTrue(dragged.isEmpty())
    }

    @Test
    fun uploadDropWithoutRemoteDestinationIsInvalid() {
        val decision = filePaneDropDecision(
            payload = FilePaneDragPayload(FilePaneSide.Local, listOf(testEntries()[0])),
            target = FilePaneSide.Remote,
            destinationDirectoryPath = null,
        )

        assertIs<FilePaneDropDecision.InvalidDestination>(decision)
    }

    @Test
    fun externalLocalEntriesUploadToRemotePane() {
        val decision = filePaneDropDecision(
            payload = FilePaneDragPayload(FilePaneSide.Local, listOf(testEntries()[0], testEntries()[1])),
            target = FilePaneSide.Remote,
            destinationDirectoryPath = "/remote/inbox",
        )

        val upload = assertIs<FilePaneDropDecision.Upload>(decision)
        assertEquals("/remote/inbox", upload.destinationDirectoryPath)
        assertEquals(listOf("/shared/report.txt", "/shared/photos"), upload.items.map { item -> item.path })
    }

    @Test
    fun emptyExternalEntryListIsIgnored() {
        val decision = filePaneDropDecision(
            payload = FilePaneDragPayload(FilePaneSide.Local, emptyList()),
            target = FilePaneSide.Remote,
            destinationDirectoryPath = "/remote/inbox",
        )

        assertIs<FilePaneDropDecision.Ignore>(decision)
    }

    @Test
    fun remoteFileDropDownloadsDirectly() {
        val decision = filePaneDropDecision(
            payload = FilePaneDragPayload(FilePaneSide.Remote, listOf(testEntries()[0])),
            target = FilePaneSide.Local,
            destinationDirectoryPath = "/downloads",
        )

        val download = assertIs<FilePaneDropDecision.Download>(decision)
        assertEquals("/downloads", download.destinationDirectoryPath)
        assertEquals(listOf("/shared/report.txt"), download.items.map { item -> item.path })
    }

    @Test
    fun remoteDirectoryDropRequestsArchiveChoice() {
        val decision = filePaneDropDecision(
            payload = FilePaneDragPayload(FilePaneSide.Remote, listOf(testEntries()[1])),
            target = FilePaneSide.Local,
            destinationDirectoryPath = "/downloads",
        )

        val confirm = assertIs<FilePaneDropDecision.ConfirmDirectoryDownload>(decision)
        assertEquals(listOf("/shared/photos"), confirm.items.map { item -> item.path })
    }

    private fun testEntries(): List<FileEntry> =
        listOf(
            FileEntry(
                path = "/shared/report.txt",
                name = "report.txt",
                type = FileEntryType.File,
                sizeBytes = 100L,
            ),
            FileEntry(
                path = "/shared/photos",
                name = "photos",
                type = FileEntryType.Directory,
            ),
            FileEntry(
                path = "/shared/notes.txt",
                name = "notes.txt",
                type = FileEntryType.File,
                sizeBytes = 12L,
            ),
        )
}
