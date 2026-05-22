package com.dimonoso.crosplatformfilesender.platform

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

fun bindAndroidPlatformContext(context: Context) {
    AndroidPlatformContext.context = context.applicationContext
}

actual fun createPlatformServices(): PlatformServices =
    PlatformServices(
        deviceInfo = PlatformDeviceInfo(
            id = "android-${Build.MANUFACTURER}-${Build.MODEL}",
            displayName = Build.MODEL.ifBlank { "Android device" },
            platformName = "Android ${Build.VERSION.SDK_INT}",
            family = PlatformFamily.Android,
        ),
        fileSystem = AndroidPlatformFileSystem(AndroidPlatformContext.context),
        networkPermissions = AndroidNetworkPermissionGateway(),
    )

private object AndroidPlatformContext {
    var context: Context? = null
}

private class AndroidPlatformFileSystem(
    private val context: Context?,
) : PlatformFileSystem {
    override val accessPolicy: FileSystemAccessPolicy = FileSystemAccessPolicy.ScopedStorage

    override val supportsTrash: Boolean
        get() = hasTrashDocumentMethod()

    override fun roots(): List<FileEntry> =
        listOf(
            FileEntry(
                path = "content://system-file-picker",
                name = "System file picker",
                type = FileEntryType.Directory,
                isBrowseable = false,
            ),
            FileEntry(
                path = "app://sandbox",
                name = "App sandbox",
                type = FileEntryType.Directory,
            ),
        )

    override fun list(path: String): List<FileEntry> {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.list(path)
        val resolver = context?.contentResolver ?: return emptyList()
        val parentUri = resolveDocumentUri(path) ?: return emptyList()
        val parentDocumentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        return runCatching {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                buildList {
                    val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        val documentId = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex).orEmpty()
                        val mimeType = cursor.getString(mimeIndex)
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, documentId)
                        val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                        add(
                            FileEntry(
                                path = childUri.toString(),
                                name = name.ifBlank { childUri.toString() },
                                type = if (isDirectory) FileEntryType.Directory else FileEntryType.File,
                                sizeBytes = cursor.takeIf { !it.isNull(sizeIndex) }?.getLong(sizeIndex),
                                isBrowseable = isDirectory,
                            ),
                        )
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun metadata(path: String): FileEntry? {
        val synthetic = path.syntheticChild()
        if (synthetic != null) {
            return list(synthetic.parentPath).firstOrNull { it.name == synthetic.childName }
        }
        if (!path.isContentPath()) return JvmLikeAndroidFiles.metadata(path)
        val resolver = context?.contentResolver ?: return null
        val uri = resolveDocumentUri(path) ?: return null
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        return runCatching {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)).orEmpty()
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE))
                val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                FileEntry(
                    path = uri.toString(),
                    name = name.ifBlank { uri.toString() },
                    type = if (isDirectory) FileEntryType.Directory else FileEntryType.File,
                    sizeBytes = cursor.takeIf { !it.isNull(sizeIndex) }?.getLong(sizeIndex),
                    isBrowseable = isDirectory,
                )
            }
        }.getOrNull()
    }

    override fun createDirectories(path: String): Boolean {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.createDirectories(path)
        val synthetic = path.syntheticChild() ?: return exists(path)
        if (exists(path)) return true
        val resolver = context?.contentResolver ?: return false
        val parentUri = resolveDocumentUri(synthetic.parentPath) ?: return false
        return runCatching {
            DocumentsContract.createDocument(
                resolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                synthetic.childName,
            ) != null
        }.getOrDefault(false)
    }

    override fun childPath(parentPath: String, childName: String): String {
        if (!parentPath.isContentPath()) return JvmLikeAndroidFiles.childPath(parentPath, childName)
        return list(parentPath).firstOrNull { it.name == childName }?.path
            ?: "$parentPath$SyntheticChildMarker${Uri.encode(childName)}"
    }

    override fun parentPath(path: String): String? {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.parentPath(path)
        return path.syntheticChild()?.parentPath
    }

    override fun tempPathFor(targetPath: String): String {
        val parent = parentPath(targetPath)
        val name = metadata(targetPath)?.name
            ?: targetPath.syntheticChild()?.childName
            ?: Uri.parse(targetPath).lastPathSegment.orEmpty().ifBlank { "transfer" }
        return if (parent != null) childPath(parent, "$name.cfs-part") else "$targetPath.cfs-part"
    }

    override fun openRead(path: String): PlatformReadStream {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.openRead(path)
        val resolver = context?.contentResolver ?: error("Android context is not bound.")
        val uri = resolveExistingUri(path) ?: error("Document does not exist: $path")
        return AndroidReadStream(resolver.openInputStream(uri) ?: error("Cannot open document for read: $path"))
    }

    override fun openWrite(path: String): PlatformWriteStream {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.openWrite(path)
        val resolver = context?.contentResolver ?: error("Android context is not bound.")
        val uri = ensureWritableDocumentUri(path) ?: error("Cannot create document: $path")
        return AndroidWriteStream(resolver.openOutputStream(uri, "wt") ?: error("Cannot open document for write: $path"))
    }

    override fun move(sourcePath: String, targetPath: String, replace: Boolean): Boolean {
        if (!sourcePath.isContentPath() && !targetPath.isContentPath()) {
            return JvmLikeAndroidFiles.move(sourcePath, targetPath, replace)
        }
        val resolver = context?.contentResolver ?: return false
        val sourceUri = resolveExistingUri(sourcePath) ?: return false
        if (replace) {
            resolveExistingUri(targetPath)?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        }
        val targetName = targetPath.syntheticChild()?.childName
            ?: metadata(targetPath)?.name
            ?: Uri.parse(targetPath).lastPathSegment.orEmpty().substringAfterLast('/')
        return runCatching {
            DocumentsContract.renameDocument(resolver, sourceUri, targetName) != null
        }.getOrDefault(false)
    }

    override fun canMoveToTrash(path: String): Boolean {
        if (!path.isContentPath()) return false
        val flags = documentFlags(path) ?: return false
        return flags and SupportsTrashFlag != 0L && hasTrashDocumentMethod()
    }

    override fun moveToTrash(path: String): Boolean {
        if (!canMoveToTrash(path)) return false
        val resolver = context?.contentResolver ?: return false
        val uri = resolveExistingUri(path) ?: return false
        return runCatching {
            DocumentsContract::class.java
                .getMethod("trashDocument", android.content.ContentResolver::class.java, Uri::class.java)
                .invoke(null, resolver, uri) != null
        }.getOrDefault(false)
    }

    override fun delete(path: String, recursive: Boolean): Boolean {
        if (!path.isContentPath()) return JvmLikeAndroidFiles.delete(path, recursive)
        val resolver = context?.contentResolver ?: return false
        val entry = metadata(path) ?: return true
        if (recursive && entry.type == FileEntryType.Directory) {
            list(path).forEach { child -> delete(child.path, recursive = true) }
        }
        val uri = resolveExistingUri(path) ?: return true
        return runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
    }

    override fun cacheDirectoryPath(): String =
        File(context?.cacheDir ?: File(System.getProperty("java.io.tmpdir")), "CrosplatformFileSender").absolutePath

    private fun resolveDocumentUri(path: String): Uri? {
        val uri = Uri.parse(path.syntheticChild()?.parentPath ?: path)
        val encodedPath = uri.encodedPath.orEmpty()
        return when {
            "/tree/" in encodedPath && "/document/" !in encodedPath -> {
                val treeDocumentId = DocumentsContract.getTreeDocumentId(uri)
                DocumentsContract.buildDocumentUriUsingTree(uri, treeDocumentId)
            }
            else -> uri
        }
    }

    private fun resolveExistingUri(path: String): Uri? {
        path.syntheticChild()?.let { child ->
            return list(child.parentPath).firstOrNull { it.name == child.childName }?.path?.let(Uri::parse)
        }
        return resolveDocumentUri(path)
    }

    private fun ensureWritableDocumentUri(path: String): Uri? {
        path.syntheticChild()?.let { child ->
            val existing = resolveExistingUri(path)
            if (existing != null) return existing
            val resolver = context?.contentResolver ?: return null
            val parentUri = resolveDocumentUri(child.parentPath) ?: return null
            return DocumentsContract.createDocument(
                resolver,
                parentUri,
                "application/octet-stream",
                child.childName,
            )
        }
        return resolveExistingUri(path)
    }

    private fun documentFlags(path: String): Long? {
        val resolver = context?.contentResolver ?: return null
        val uri = resolveExistingUri(path) ?: return null
        return runCatching {
            resolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val flagsIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS)
                cursor.getLong(flagsIndex)
            }
        }.getOrNull()
    }

    private fun hasTrashDocumentMethod(): Boolean =
        runCatching {
            DocumentsContract::class.java.getMethod(
                "trashDocument",
                android.content.ContentResolver::class.java,
                Uri::class.java,
            )
        }.isSuccess
}

private const val SyntheticChildMarker = "#cfs-child="
private const val SupportsTrashFlag = 65_536L

private data class SyntheticChild(
    val parentPath: String,
    val childName: String,
)

private fun String.syntheticChild(): SyntheticChild? {
    val markerIndex = indexOf(SyntheticChildMarker)
    if (markerIndex < 0) return null
    return SyntheticChild(
        parentPath = substring(0, markerIndex),
        childName = Uri.decode(substring(markerIndex + SyntheticChildMarker.length)),
    )
}

private fun String.isContentPath(): Boolean = startsWith("content://")

private object JvmLikeAndroidFiles {
    fun list(path: String): List<FileEntry> =
        File(path).listFiles()
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { file -> file.toFileEntry() }
            .orEmpty()

    fun metadata(path: String): FileEntry? =
        File(path).takeIf { it.exists() }?.toFileEntry()

    fun createDirectories(path: String): Boolean =
        File(path).let { it.exists() && it.isDirectory || it.mkdirs() }

    fun childPath(parentPath: String, childName: String): String =
        File(parentPath, childName).absolutePath

    fun parentPath(path: String): String? =
        File(path).parentFile?.absolutePath

    fun openRead(path: String): PlatformReadStream =
        AndroidReadStream(FileInputStream(File(path)))

    fun openWrite(path: String): PlatformWriteStream {
        File(path).parentFile?.mkdirs()
        return AndroidWriteStream(FileOutputStream(File(path), false))
    }

    fun move(sourcePath: String, targetPath: String, replace: Boolean): Boolean {
        val source = File(sourcePath)
        val target = File(targetPath)
        target.parentFile?.mkdirs()
        if (replace && target.exists()) target.deleteRecursively()
        return source.renameTo(target) || runCatching {
            source.copyRecursively(target, overwrite = replace)
            source.deleteRecursively()
            true
        }.getOrDefault(false)
    }

    fun delete(path: String, recursive: Boolean): Boolean {
        val file = File(path)
        if (!file.exists()) return true
        return if (recursive) file.deleteRecursively() else file.delete()
    }

    private fun File.toFileEntry(): FileEntry =
        FileEntry(
            path = absolutePath,
            name = name.ifBlank { absolutePath },
            type = when {
                isDirectory -> FileEntryType.Directory
                isFile -> FileEntryType.File
                else -> FileEntryType.Unknown
            },
            sizeBytes = if (isFile) length() else null,
            isBrowseable = isDirectory,
        )
}

private class AndroidReadStream(
    private val input: InputStream,
) : PlatformReadStream {
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        input.read(buffer, offset, length)

    override fun close() {
        input.close()
    }
}

private class AndroidWriteStream(
    private val output: OutputStream,
) : PlatformWriteStream {
    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        output.write(buffer, offset, length)
    }

    override fun close() {
        output.close()
    }
}

private class AndroidNetworkPermissionGateway : NetworkPermissionGateway {
    override fun currentState(): NetworkPermissionState =
        NetworkPermissionState(
            supportsUdpDiscovery = true,
            requiresRuntimeApproval = false,
            statusLabel = "Android LAN access scaffolded",
        )
}
