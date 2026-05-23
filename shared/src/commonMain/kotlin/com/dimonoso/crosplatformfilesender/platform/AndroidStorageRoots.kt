package com.dimonoso.crosplatformfilesender.platform

import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType

internal data class AndroidStorageRootCandidate(
    val path: String,
    val displayName: String? = null,
    val isPrimary: Boolean = false,
    val isMounted: Boolean = true,
    val exists: Boolean = true,
)

internal fun androidStorageRootEntries(candidates: List<AndroidStorageRootCandidate>): List<FileEntry> {
    val byPath = linkedMapOf<String, AndroidStorageRootCandidate>()
    candidates
        .filter { candidate -> candidate.isMounted && candidate.exists }
        .mapNotNull { candidate ->
            candidate.path.normalizedAndroidStoragePath()?.let { path -> candidate.copy(path = path) }
        }
        .forEach { candidate ->
            val key = candidate.path.normalizedAndroidStorageKey()
            val existing = byPath[key]
            if (existing == null || candidate.isPrimary && !existing.isPrimary) {
                byPath[key] = candidate
            }
        }

    val roots = byPath.values.toList()
    val primaryRoots = roots.filter { it.isPrimary }
    val otherRoots = roots
        .filterNot { it.isPrimary }
        .sortedWith(compareBy<AndroidStorageRootCandidate> { it.displayName.orEmpty().lowercase() }.thenBy { it.path })

    return (primaryRoots + otherRoots).map { candidate ->
        FileEntry(
            path = candidate.path,
            name = candidate.resolvedDisplayName(),
            type = FileEntryType.Drive,
        )
    }
}

internal fun androidSharedStorageRootPath(path: String, packageName: String): String? {
    val normalized = path.normalizedAndroidStoragePath() ?: return null
    val packageMarkers = listOf(
        "/Android/data/$packageName",
        "/Android/media/$packageName",
    )
    val markerIndex = packageMarkers
        .map { marker -> normalized.indexOf(marker) }
        .filter { index -> index > 0 }
        .minOrNull()

    return markerIndex
        ?.let { index -> normalized.substring(0, index).normalizedAndroidStoragePath() }
        ?: normalized
}

private fun AndroidStorageRootCandidate.resolvedDisplayName(): String =
    displayName?.takeIf { it.isNotBlank() }
        ?: if (isPrimary) {
            "Internal storage"
        } else {
            path.substringAfterLast('/').ifBlank { path }
        }

private fun String.normalizedAndroidStoragePath(): String? {
    val normalized = replace('\\', '/').trim().trimEnd('/')
    return normalized.ifBlank { null }
}

private fun String.normalizedAndroidStorageKey(): String =
    lowercase().trimEnd('/')
