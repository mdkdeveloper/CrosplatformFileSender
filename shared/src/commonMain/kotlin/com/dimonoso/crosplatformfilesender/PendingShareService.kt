package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PendingShareService {
    val files: StateFlow<List<FileEntry>>

    fun replaceFiles(files: List<FileEntry>)

    fun clearFiles()
}

class InMemoryPendingShareService : PendingShareService {
    private val mutableFiles = MutableStateFlow<List<FileEntry>>(emptyList())

    override val files: StateFlow<List<FileEntry>> = mutableFiles

    override fun replaceFiles(files: List<FileEntry>) {
        mutableFiles.value = files
    }

    override fun clearFiles() {
        mutableFiles.value = emptyList()
    }
}

expect fun createPendingShareService(): PendingShareService
