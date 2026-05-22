package com.dimonoso.crosplatformfilesender.remote

import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryConfig
import com.dimonoso.crosplatformfilesender.discovery.discoveryKeywordFingerprint
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteBatchResult
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteMode
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteResult
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteStatus
import com.dimonoso.crosplatformfilesender.filesystem.FileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.RemoteDeletePolicy
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistedBrowseResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface RemoteFileCatalogService {
    val serverError: StateFlow<RemoteFileCatalogError?>
    val pendingDeletePrompt: StateFlow<PendingRemoteDeletePrompt?>

    fun startServer(config: DiscoveryConfig)

    fun stopServer()

    suspend fun browse(
        device: DiscoveredDevice,
        path: String?,
    ): RemoteFileCatalogResult

    suspend fun delete(
        device: DiscoveredDevice,
        paths: List<String>,
    ): RemoteFileDeleteResult

    fun resolvePendingDeletePrompt(promptId: String, decision: RemoteDeletePromptDecision)
}

fun createRemoteFileCatalogService(
    fileSystem: FileSystemService,
    keywordProvider: () -> String,
    deletePolicyProvider: () -> RemoteDeletePolicy,
): RemoteFileCatalogService =
    DefaultRemoteFileCatalogService(
        fileSystem = fileSystem,
        keywordProvider = keywordProvider,
        deletePolicyProvider = deletePolicyProvider,
        transport = createRemoteFileCatalogTransport(),
    )

internal class DefaultRemoteFileCatalogService(
    private val fileSystem: FileSystemService,
    private val keywordProvider: () -> String,
    private val deletePolicyProvider: () -> RemoteDeletePolicy = { RemoteDeletePolicy.DoNothing },
    private val transport: RemoteFileCatalogTransport,
    private val deletePromptTimeoutMillis: Long = DeletePromptTimeoutMillis,
) : RemoteFileCatalogService {
    private val _serverError = MutableStateFlow<RemoteFileCatalogError?>(null)
    private val _pendingDeletePrompt = MutableStateFlow<PendingRemoteDeletePrompt?>(null)
    private var server: RemoteFileCatalogServer? = null
    private var serverKeywordFingerprint: String? = null
    private var pendingDeleteApproval: PendingDeleteApproval? = null
    private var nextPromptId = 1

    override val serverError: StateFlow<RemoteFileCatalogError?> = _serverError
    override val pendingDeletePrompt: StateFlow<PendingRemoteDeletePrompt?> = _pendingDeletePrompt

    override fun startServer(config: DiscoveryConfig) {
        stopServer()
        serverKeywordFingerprint = discoveryKeywordFingerprint(config.keyword)
        _serverError.value = null
        server = runCatching {
            transport.startServer(config.udpPort, ::handleRequest)
        }.getOrElse { exception ->
            serverKeywordFingerprint = null
            _serverError.value = RemoteFileCatalogError(
                code = RemoteFileCatalogErrorCode.ServerUnavailable,
                message = exception.readableMessage(),
            )
            null
        }
    }

    override fun stopServer() {
        server?.close()
        server = null
        serverKeywordFingerprint = null
        _serverError.value = null
        clearPendingDeletePrompt()
    }

    override suspend fun browse(
        device: DiscoveredDevice,
        path: String?,
    ): RemoteFileCatalogResult {
        val response = runCatching {
            transport.request(
                host = device.host,
                port = device.port,
                request = RemoteFileCatalogRequest(
                    keywordFingerprint = discoveryKeywordFingerprint(keywordProvider()),
                    path = path,
                ),
            )
        }.getOrElse { exception ->
            return RemoteFileCatalogResult.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.Network,
                    message = exception.readableMessage(),
                ),
            )
        }

        return when (response) {
            is RemoteFileCatalogResponse.Success -> RemoteFileCatalogResult.Success(response.entries)
            is RemoteFileCatalogResponse.DeleteCompleted -> RemoteFileCatalogResult.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.BadRequest,
                    message = "Unexpected delete response.",
                ),
            )
            is RemoteFileCatalogResponse.Failure -> RemoteFileCatalogResult.Failure(response.error)
        }
    }

    override suspend fun delete(
        device: DiscoveredDevice,
        paths: List<String>,
    ): RemoteFileDeleteResult {
        val response = runCatching {
            transport.request(
                host = device.host,
                port = device.port,
                request = RemoteFileCatalogRequest(
                    keywordFingerprint = discoveryKeywordFingerprint(keywordProvider()),
                    operation = RemoteFileCatalogOperation.Delete,
                    deletePaths = paths,
                ),
            )
        }.getOrElse { exception ->
            return RemoteFileDeleteResult.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.Network,
                    message = exception.readableMessage(),
                ),
            )
        }

        return when (response) {
            is RemoteFileCatalogResponse.DeleteCompleted -> RemoteFileDeleteResult.Completed(response.result)
            is RemoteFileCatalogResponse.Failure -> RemoteFileDeleteResult.Failure(response.error)
            is RemoteFileCatalogResponse.Success -> RemoteFileDeleteResult.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.BadRequest,
                    message = "Unexpected browse response.",
                ),
            )
        }
    }

    override fun resolvePendingDeletePrompt(promptId: String, decision: RemoteDeletePromptDecision) {
        val pending = pendingDeleteApproval?.takeIf { it.prompt.id == promptId } ?: return
        if (!pending.result.isCompleted) {
            pending.result.complete(decision.toDeleteMode())
        }
    }

    private suspend fun handleRequest(request: RemoteFileCatalogRequest): RemoteFileCatalogResponse {
        if (request.keywordFingerprint != serverKeywordFingerprint) {
            return RemoteFileCatalogResponse.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.Unauthorized,
                    message = "Unauthorized catalog request.",
                ),
            )
        }

        return when (request.operation) {
            RemoteFileCatalogOperation.Browse -> browseWhitelisted(request.path)
            RemoteFileCatalogOperation.Delete -> deleteWhitelisted(request.deletePaths)
        }
    }

    private fun browseWhitelisted(path: String?): RemoteFileCatalogResponse =
        when (val result = fileSystem.browseWhitelistedResult(path)) {
            is WhitelistedBrowseResult.Success -> RemoteFileCatalogResponse.Success(result.entries)
            is WhitelistedBrowseResult.Failure -> RemoteFileCatalogResponse.Failure(
                RemoteFileCatalogError(
                    code = RemoteFileCatalogErrorCode.OutsideWhitelist,
                    message = result.message,
                ),
            )
        }

    private suspend fun deleteWhitelisted(paths: List<String>): RemoteFileCatalogResponse {
        val immediateResults = mutableListOf<FileDeleteResult>()
        val pathsToAsk = mutableListOf<String>()

        paths.distinct().forEach { path ->
            when (fileSystem.effectiveRemoteDeletePolicy(path, deletePolicyProvider())) {
                null -> immediateResults += path.refused("The selected item is outside the whitelist.")
                RemoteDeletePolicy.DoNothing -> immediateResults += path.refused("Remote delete is disabled.")
                RemoteDeletePolicy.Ask -> pathsToAsk += path
                RemoteDeletePolicy.Trash ->
                    immediateResults += fileSystem.deleteWhitelisted(listOf(path), FileDeleteMode.Trash).results
                RemoteDeletePolicy.Permanent ->
                    immediateResults += fileSystem.deleteWhitelisted(listOf(path), FileDeleteMode.Permanent).results
            }
        }

        if (pathsToAsk.isNotEmpty()) {
            val approvedMode = requestDeleteApproval(pathsToAsk)
            immediateResults += if (approvedMode == null) {
                pathsToAsk.map { path -> path.refused("Remote delete was cancelled.") }
            } else {
                fileSystem.deleteWhitelisted(pathsToAsk, approvedMode).results
            }
        }

        return RemoteFileCatalogResponse.DeleteCompleted(FileDeleteBatchResult(immediateResults))
    }

    private suspend fun requestDeleteApproval(paths: List<String>): FileDeleteMode? {
        if (pendingDeleteApproval != null) return null
        val prompt = PendingRemoteDeletePrompt(
            id = "delete-${nextPromptId++}",
            paths = paths,
            canMoveAllToTrash = paths.all(fileSystem::canMoveToTrash),
        )
        val approval = PendingDeleteApproval(prompt, CompletableDeferred())
        pendingDeleteApproval = approval
        _pendingDeletePrompt.value = prompt

        return try {
            withTimeoutOrNull(deletePromptTimeoutMillis) { approval.result.await() }
        } finally {
            if (pendingDeleteApproval === approval) {
                pendingDeleteApproval = null
                _pendingDeletePrompt.value = null
            }
        }
    }

    private fun clearPendingDeletePrompt() {
        val pending = pendingDeleteApproval ?: return
        if (!pending.result.isCompleted) {
            pending.result.complete(null)
        }
        pendingDeleteApproval = null
        _pendingDeletePrompt.value = null
    }

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"
}

private data class PendingDeleteApproval(
    val prompt: PendingRemoteDeletePrompt,
    val result: CompletableDeferred<FileDeleteMode?>,
)

private fun RemoteDeletePromptDecision.toDeleteMode(): FileDeleteMode? =
    when (this) {
        RemoteDeletePromptDecision.Trash -> FileDeleteMode.Trash
        RemoteDeletePromptDecision.Permanent -> FileDeleteMode.Permanent
        RemoteDeletePromptDecision.Cancel -> null
    }

private fun String.refused(message: String): FileDeleteResult =
    FileDeleteResult(path = this, status = FileDeleteStatus.Refused, message = message)

private const val DeletePromptTimeoutMillis = 60_000L
