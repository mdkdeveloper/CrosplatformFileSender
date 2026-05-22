@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryError
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryErrorType
import com.dimonoso.crosplatformfilesender.discovery.toLocalDiscoveryConfig
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileDeleteBatchResult
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.RemoteDeletePolicy
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistDeletePolicyOverride
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import com.dimonoso.crosplatformfilesender.localization.AppLocaleEnvironment
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogError
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogErrorCode
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogResult
import com.dimonoso.crosplatformfilesender.remote.RemoteFileDeleteResult
import com.dimonoso.crosplatformfilesender.remote.RemoteDeletePromptDecision
import com.dimonoso.crosplatformfilesender.settings.AppLanguageMode
import com.dimonoso.crosplatformfilesender.settings.AppSettings
import com.dimonoso.crosplatformfilesender.settings.BackupMode
import com.dimonoso.crosplatformfilesender.settings.getSystemLanguageCode
import com.dimonoso.crosplatformfilesender.settings.isDiscoveryKeywordChar
import com.dimonoso.crosplatformfilesender.settings.isValidDiscoveryKeyword
import com.dimonoso.crosplatformfilesender.settings.resolveAppLanguage
import com.dimonoso.crosplatformfilesender.transfer.TransferDirection
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferItem
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus
import com.dimonoso.crosplatformfilesender.transfer.TransferTask
import com.dimonoso.crosplatformfilesender.transfer.toTransferEndpoint
import crosplatformfilesender.shared.generated.resources.Res
import crosplatformfilesender.shared.generated.resources.add_folder
import crosplatformfilesender.shared.generated.resources.add_upload
import crosplatformfilesender.shared.generated.resources.archive_download_message
import crosplatformfilesender.shared.generated.resources.archive_download_title
import crosplatformfilesender.shared.generated.resources.archive_no
import crosplatformfilesender.shared.generated.resources.archive_yes
import crosplatformfilesender.shared.generated.resources.backup_archives
import crosplatformfilesender.shared.generated.resources.backup_empty
import crosplatformfilesender.shared.generated.resources.backup_file
import crosplatformfilesender.shared.generated.resources.backup_folder
import crosplatformfilesender.shared.generated.resources.backup_mode
import crosplatformfilesender.shared.generated.resources.backup_none
import crosplatformfilesender.shared.generated.resources.cancel
import crosplatformfilesender.shared.generated.resources.catalog_server_error
import crosplatformfilesender.shared.generated.resources.close
import crosplatformfilesender.shared.generated.resources.delete
import crosplatformfilesender.shared.generated.resources.delete_policy_ask
import crosplatformfilesender.shared.generated.resources.delete_policy_default
import crosplatformfilesender.shared.generated.resources.delete_policy_none
import crosplatformfilesender.shared.generated.resources.delete_policy_permanent
import crosplatformfilesender.shared.generated.resources.delete_policy_trash
import crosplatformfilesender.shared.generated.resources.delete_problem
import crosplatformfilesender.shared.generated.resources.device_name
import crosplatformfilesender.shared.generated.resources.discovery_error_port
import crosplatformfilesender.shared.generated.resources.discovery_error_stopped
import crosplatformfilesender.shared.generated.resources.discovery_error_unavailable
import crosplatformfilesender.shared.generated.resources.download_selected
import crosplatformfilesender.shared.generated.resources.empty_or_limited_access
import crosplatformfilesender.shared.generated.resources.file_entry_subtitle
import crosplatformfilesender.shared.generated.resources.file_size_bytes
import crosplatformfilesender.shared.generated.resources.file_type_drive
import crosplatformfilesender.shared.generated.resources.file_type_file
import crosplatformfilesender.shared.generated.resources.file_type_folder
import crosplatformfilesender.shared.generated.resources.file_type_other
import crosplatformfilesender.shared.generated.resources.found_devices
import crosplatformfilesender.shared.generated.resources.invalid_transfer_destination
import crosplatformfilesender.shared.generated.resources.keyword
import crosplatformfilesender.shared.generated.resources.keyword_allowed_characters
import crosplatformfilesender.shared.generated.resources.keyword_allowed_template
import crosplatformfilesender.shared.generated.resources.language
import crosplatformfilesender.shared.generated.resources.language_english
import crosplatformfilesender.shared.generated.resources.language_system
import crosplatformfilesender.shared.generated.resources.language_ukrainian
import crosplatformfilesender.shared.generated.resources.loading
import crosplatformfilesender.shared.generated.resources.local_delete_message
import crosplatformfilesender.shared.generated.resources.local_delete_title
import crosplatformfilesender.shared.generated.resources.manual_peer
import crosplatformfilesender.shared.generated.resources.max_incoming_transfers
import crosplatformfilesender.shared.generated.resources.max_outgoing_transfers
import crosplatformfilesender.shared.generated.resources.my_files
import crosplatformfilesender.shared.generated.resources.name_column
import crosplatformfilesender.shared.generated.resources.no_active_whitelist_folders
import crosplatformfilesender.shared.generated.resources.no_found_devices
import crosplatformfilesender.shared.generated.resources.no_remote_files_available
import crosplatformfilesender.shared.generated.resources.open
import crosplatformfilesender.shared.generated.resources.pause
import crosplatformfilesender.shared.generated.resources.queue
import crosplatformfilesender.shared.generated.resources.queue_empty
import crosplatformfilesender.shared.generated.resources.queue_progress
import crosplatformfilesender.shared.generated.resources.refresh
import crosplatformfilesender.shared.generated.resources.remote_files
import crosplatformfilesender.shared.generated.resources.remote_error_bad_request
import crosplatformfilesender.shared.generated.resources.remote_error_network
import crosplatformfilesender.shared.generated.resources.remote_error_outside_whitelist
import crosplatformfilesender.shared.generated.resources.remote_error_server_unavailable
import crosplatformfilesender.shared.generated.resources.remote_error_unauthorized
import crosplatformfilesender.shared.generated.resources.remote_error_unknown
import crosplatformfilesender.shared.generated.resources.remote_delete_policy
import crosplatformfilesender.shared.generated.resources.remote_delete_prompt_message
import crosplatformfilesender.shared.generated.resources.remote_delete_prompt_title
import crosplatformfilesender.shared.generated.resources.remove
import crosplatformfilesender.shared.generated.resources.request_download
import crosplatformfilesender.shared.generated.resources.resume
import crosplatformfilesender.shared.generated.resources.restore
import crosplatformfilesender.shared.generated.resources.save
import crosplatformfilesender.shared.generated.resources.search_active
import crosplatformfilesender.shared.generated.resources.search_off
import crosplatformfilesender.shared.generated.resources.send_selected
import crosplatformfilesender.shared.generated.resources.settings_tab
import crosplatformfilesender.shared.generated.resources.settings_title
import crosplatformfilesender.shared.generated.resources.size_column
import crosplatformfilesender.shared.generated.resources.start_search_first
import crosplatformfilesender.shared.generated.resources.status_cancelled
import crosplatformfilesender.shared.generated.resources.status_completed
import crosplatformfilesender.shared.generated.resources.status_failed
import crosplatformfilesender.shared.generated.resources.status_paused
import crosplatformfilesender.shared.generated.resources.status_pending
import crosplatformfilesender.shared.generated.resources.status_running
import crosplatformfilesender.shared.generated.resources.task_cancel
import crosplatformfilesender.shared.generated.resources.task_error
import crosplatformfilesender.shared.generated.resources.to_roots
import crosplatformfilesender.shared.generated.resources.to_whitelist
import crosplatformfilesender.shared.generated.resources.transfer_direction_download
import crosplatformfilesender.shared.generated.resources.transfer_direction_upload
import crosplatformfilesender.shared.generated.resources.transfer_limits
import crosplatformfilesender.shared.generated.resources.transfer_progress_bytes
import crosplatformfilesender.shared.generated.resources.transfer_queue
import crosplatformfilesender.shared.generated.resources.transfer_subtitle
import crosplatformfilesender.shared.generated.resources.type_column
import crosplatformfilesender.shared.generated.resources.whitelist_empty
import crosplatformfilesender.shared.generated.resources.whitelist_folders
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App() {
    val services = remember { createAppServices() }
    val settings by services.settings.settings.collectAsState()
    val systemLanguageCode = remember { getSystemLanguageCode() }
    val resolvedLanguage = resolveAppLanguage(settings.languageMode, systemLanguageCode)
    var showQueueDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    AppLocaleEnvironment(resolvedLanguage.localeTag) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier
                        .safeContentPadding()
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AppHeader(
                        services = services,
                        onQueueClick = { showQueueDialog = true },
                        onSettingsClick = { showSettingsDialog = true },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        WorkspaceSection(services)
                    }
                }

                if (showQueueDialog) {
                    QueueDialog(
                        services = services,
                        onDismiss = { showQueueDialog = false },
                    )
                }
                if (showSettingsDialog) {
                    SettingsDialog(
                        services = services,
                        onDismiss = { showSettingsDialog = false },
                    )
                }
                PendingRemoteDeleteDialog(services)
            }
        }
    }
}

@Composable
private fun AppHeader(
    services: AppServices,
    onQueueClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val settings by services.settings.settings.collectAsState()
    val deviceDisplayName = settings.deviceDisplayNameOr(services.platform.deviceInfo.displayName)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = deviceDisplayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SearchToggleButton(
            services = services,
            systemDeviceName = services.platform.deviceInfo.displayName,
        )
        QueueButton(
            services = services,
            onClick = onQueueClick,
        )
        OutlinedButton(onClick = onSettingsClick) {
            Text(stringResource(Res.string.settings_tab), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun AppSettings.deviceDisplayNameOr(systemDisplayName: String): String =
    deviceDisplayName.ifBlank { systemDisplayName }

private fun startLocalDiscovery(
    services: AppServices,
    keyword: String,
    deviceDisplayName: String,
) {
    val config = services.platform.deviceInfo.toLocalDiscoveryConfig(keyword, deviceDisplayName)
    services.remoteFileCatalog.stopServer()
    services.transferQueue.stopServer()
    services.discovery.start(config)
    if (services.discovery.isRunning.value) {
        services.remoteFileCatalog.startServer(config)
        services.transferQueue.startServer((config.udpPort + 1).coerceAtMost(65535))
    }
}

private fun stopLocalDiscovery(services: AppServices) {
    services.remoteFileCatalog.stopServer()
    services.transferQueue.stopServer()
    services.discovery.stop()
}

@Composable
private fun SearchToggleButton(
    services: AppServices,
    systemDeviceName: String,
) {
    val settings by services.settings.settings.collectAsState()
    val isRunning by services.discovery.isRunning.collectAsState()
    val keyword = settings.discoveryKeyword
    val deviceDisplayName = settings.deviceDisplayNameOr(systemDeviceName)
    val enabled = isRunning || isValidDiscoveryKeyword(keyword)
    val label = if (isRunning) {
        stringResource(Res.string.search_active)
    } else {
        stringResource(Res.string.search_off)
    }
    val onClick = {
        if (isRunning) {
            stopLocalDiscovery(services)
        } else {
            startLocalDiscovery(services, keyword, deviceDisplayName)
        }
    }

    if (isRunning) {
        Button(
            enabled = enabled,
            onClick = onClick,
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            enabled = enabled,
            onClick = onClick,
        ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QueueButton(
    services: AppServices,
    onClick: () -> Unit,
) {
    val tasks by services.transferQueue.tasks.collectAsState()
    val progress = remember(tasks) { queueProgress(tasks) }
    val label = progress?.let {
        stringResource(Res.string.queue_progress, (it * 100).roundToInt().toString())
    } ?: stringResource(Res.string.queue)

    OutlinedButton(onClick = onClick) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WorkspaceSection(services: AppServices) {
    val settings by services.settings.settings.collectAsState()
    val devices by services.discovery.devices.collectAsState()
    val isRunning by services.discovery.isRunning.collectAsState()
    val lastError by services.discovery.lastError.collectAsState()
    val serverError by services.remoteFileCatalog.serverError.collectAsState()
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedDeviceSnapshot by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var missingSelectedDeviceAttempts by remember { mutableStateOf(0) }

    val liveSelectedDevice = devices.firstOrNull { device -> device.id == selectedDeviceId }
    val selectedDevice = if (selectedDeviceId == null) null else liveSelectedDevice ?: selectedDeviceSnapshot
    val visibleDevices = selectedDeviceSnapshot
        ?.takeIf { snapshot -> selectedDeviceId == snapshot.id && devices.none { device -> device.id == snapshot.id } }
        ?.let { snapshot -> listOf(snapshot) + devices }
        ?: devices

    LaunchedEffect(devices, selectedDeviceId) {
        if (selectedDeviceId == null) {
            selectedDeviceSnapshot = null
            missingSelectedDeviceAttempts = 0
            return@LaunchedEffect
        }

        val liveDevice = devices.firstOrNull { device -> device.id == selectedDeviceId }
        if (liveDevice != null) {
            selectedDeviceSnapshot = liveDevice
            missingSelectedDeviceAttempts = 0
        } else if (selectedDeviceSnapshot == null) {
            selectedDeviceId = null
            missingSelectedDeviceAttempts = 0
        }
    }

    LaunchedEffect(selectedDeviceId, liveSelectedDevice?.id, isRunning, missingSelectedDeviceAttempts) {
        if (!isRunning || selectedDeviceId == null || liveSelectedDevice != null) return@LaunchedEffect

        if (missingSelectedDeviceAttempts >= SelectedDeviceMissingAttemptLimit) {
            selectedDeviceId = null
            selectedDeviceSnapshot = null
            missingSelectedDeviceAttempts = 0
            return@LaunchedEffect
        }

        delay(SelectedDeviceMissingAttemptIntervalMillis)
        missingSelectedDeviceAttempts += 1
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            services.remoteFileCatalog.stopServer()
            services.transferQueue.stopServer()
            selectedDeviceId = null
            selectedDeviceSnapshot = null
            missingSelectedDeviceAttempts = 0
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lastError?.let { error ->
            ErrorState(discoveryErrorText(error))
        } ?: serverError?.let { error ->
            ErrorState(stringResource(Res.string.catalog_server_error, error.message))
        }
        DevicesPanel(
            devices = visibleDevices,
            selectedDeviceId = selectedDeviceId,
            onSelect = { device ->
                selectedDeviceId = device.id
                selectedDeviceSnapshot = device
                missingSelectedDeviceAttempts = 0
            },
        )
        HorizontalDivider()
        FileBrowserColumns(
            services = services,
            selectedDevice = selectedDevice,
            keyword = settings.discoveryKeyword,
        )
    }
}

@Composable
private fun discoveryErrorText(error: DiscoveryError): String =
    when (error.type) {
        DiscoveryErrorType.UdpPortOpenFailed -> stringResource(
            Res.string.discovery_error_port,
            error.port?.toString().orEmpty(),
            error.detail,
        )
        DiscoveryErrorType.SearchStopped -> stringResource(Res.string.discovery_error_stopped, error.detail)
        DiscoveryErrorType.Unavailable -> stringResource(Res.string.discovery_error_unavailable, error.detail)
    }

@Composable
private fun DevicesPanel(
    devices: List<DiscoveredDevice>,
    selectedDeviceId: String?,
    onSelect: (DiscoveredDevice) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.found_devices)) {
        if (devices.isEmpty()) {
            EmptyState(stringResource(Res.string.no_found_devices))
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                devices.forEach { device ->
                    DeviceDropZone(
                        device = device,
                        isSelected = device.id == selectedDeviceId,
                        onClick = { onSelect(device) },
                    )
                }
            }
        }
    }
}

private sealed interface RemotePaneState {
    data object Idle : RemotePaneState
    data object Loading : RemotePaneState
    data class Loaded(val entries: List<FileEntry>) : RemotePaneState
    data class Failed(val error: RemoteFileCatalogError) : RemotePaneState
}

private data class FileBrowserSelection(
    val selectedRowIds: Set<String> = emptySet(),
    val anchorRowId: String? = null,
)

private data class FileSelectionModifiers(
    val isRangeSelection: Boolean = false,
    val isToggleSelection: Boolean = false,
)

private const val SelectedDeviceMissingAttemptLimit = 5
private const val SelectedDeviceMissingAttemptIntervalMillis = 2_000L
private const val PaneAutoRefreshDebounceMillis = 250L

@Composable
private fun FileBrowserColumns(
    services: AppServices,
    selectedDevice: DiscoveredDevice?,
    keyword: String,
) {
    val localRoots = remember(services) { services.fileSystem.localRoots() }
    var localPath by remember { mutableStateOf<String?>(null) }
    var localPathStack by remember { mutableStateOf<List<String?>>(emptyList()) }
    var localSelection by remember { mutableStateOf(FileBrowserSelection()) }
    var activeFilePane by remember { mutableStateOf<FilePaneSide?>(null) }
    var localReloadToken by remember { mutableStateOf(0) }
    var remotePath by remember(selectedDevice?.id) { mutableStateOf<String?>(null) }
    var remotePathStack by remember(selectedDevice?.id) { mutableStateOf<List<String?>>(emptyList()) }
    var remoteSelection by remember(selectedDevice?.id) { mutableStateOf(FileBrowserSelection()) }
    var remoteReloadToken by remember(selectedDevice?.id) { mutableStateOf(0) }
    var remoteState by remember(selectedDevice?.id) { mutableStateOf<RemotePaneState>(RemotePaneState.Idle) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var pendingLocalDeleteEntries by remember { mutableStateOf<List<FileEntry>?>(null) }
    var pendingDownloadItems by remember { mutableStateOf<List<TransferItem>?>(null) }
    var activeDragPayload by remember { mutableStateOf<FilePaneDragPayload?>(null) }
    var localAutoRefreshRequest by remember { mutableStateOf(0) }
    var remoteAutoRefreshRequest by remember(selectedDevice?.id) { mutableStateOf(0) }
    val transferTasks by services.transferQueue.tasks.collectAsState()
    val autoRefreshTracker = remember(services.transferQueue) { TransferPaneAutoRefreshTracker() }
    val coroutineScope = rememberCoroutineScope()
    val invalidDestinationText = stringResource(Res.string.invalid_transfer_destination)
    val deleteProblemText = stringResource(Res.string.delete_problem)

    val localEntries = remember(localPath, localReloadToken, localRoots) {
        localPath?.let(services.fileSystem::browseLocal) ?: localRoots
    }
    val remoteEntries = (remoteState as? RemotePaneState.Loaded)?.entries.orEmpty()
    val selectedLocalEntries = localEntries.filter { it.path in localSelection.selectedRowIds }
    val selectedRemoteEntries = remoteEntries.filter { it.path in remoteSelection.selectedRowIds }

    LaunchedEffect(transferTasks, selectedDevice?.id, localPath, remotePath) {
        val refreshRequest = autoRefreshTracker.consume(
            tasks = transferTasks,
            selectedRemoteDeviceId = selectedDevice?.id,
            localPath = localPath,
            remotePath = remotePath,
        )
        if (refreshRequest.local) {
            localAutoRefreshRequest += 1
        }
        if (refreshRequest.remote) {
            remoteAutoRefreshRequest += 1
        }
    }

    LaunchedEffect(localAutoRefreshRequest) {
        if (localAutoRefreshRequest == 0) return@LaunchedEffect
        delay(PaneAutoRefreshDebounceMillis)
        localReloadToken += 1
    }

    LaunchedEffect(selectedDevice?.id, remoteAutoRefreshRequest) {
        if (remoteAutoRefreshRequest == 0) return@LaunchedEffect
        delay(PaneAutoRefreshDebounceMillis)
        remoteReloadToken += 1
    }

    fun requestFilePaneTransfer(
        payload: FilePaneDragPayload,
        target: FilePaneSide,
    ): Boolean {
        val destinationDirectoryPath = when (target) {
            FilePaneSide.Local -> localPath
            FilePaneSide.Remote -> remotePath
        }

        return when (val decision = filePaneDropDecision(payload, target, destinationDirectoryPath)) {
            FilePaneDropDecision.Ignore -> false
            FilePaneDropDecision.InvalidDestination -> {
                transferError = invalidDestinationText
                false
            }
            is FilePaneDropDecision.Upload -> {
                val device = selectedDevice ?: return false
                services.transferQueue.enqueueUpload(
                    items = decision.items,
                    target = device.toTransferEndpoint(),
                    destinationDirectoryPath = decision.destinationDirectoryPath,
                )
                localSelection = FileBrowserSelection()
                true
            }
            is FilePaneDropDecision.Download -> {
                val device = selectedDevice ?: return false
                services.transferQueue.enqueueDownload(
                    items = decision.items,
                    source = device.toTransferEndpoint(),
                    destinationDirectoryPath = decision.destinationDirectoryPath,
                    archiveDirectories = false,
                )
                remoteSelection = FileBrowserSelection()
                true
            }
            is FilePaneDropDecision.ConfirmDirectoryDownload -> {
                pendingDownloadItems = decision.items
                true
            }
        }
    }

    fun setDeleteProblem(result: FileDeleteBatchResult) {
        deleteError = result.firstProblem?.let { problem -> "$deleteProblemText ${problem.message}" }
    }

    fun requestDelete(side: FilePaneSide) {
        when (side) {
            FilePaneSide.Local -> {
                if (selectedLocalEntries.isNotEmpty()) {
                    pendingLocalDeleteEntries = selectedLocalEntries
                }
            }
            FilePaneSide.Remote -> {
                val device = selectedDevice ?: return
                if (selectedRemoteEntries.isEmpty()) return

                coroutineScope.launch {
                    when (val result = services.remoteFileCatalog.delete(device, selectedRemoteEntries.map { it.path })) {
                        is RemoteFileDeleteResult.Completed -> {
                            setDeleteProblem(result.result)
                            if (result.result.hasDeletedEntries) {
                                remoteSelection = FileBrowserSelection()
                                remoteReloadToken += 1
                            }
                        }
                        is RemoteFileDeleteResult.Failure -> {
                            deleteError = "$deleteProblemText ${result.error.message}"
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedDevice?.id, remotePath, remoteReloadToken, keyword) {
        if (selectedDevice == null) {
            remoteState = RemotePaneState.Idle
            return@LaunchedEffect
        }

        remoteState = RemotePaneState.Loading
        remoteState = when (val result = services.remoteFileCatalog.browse(selectedDevice, remotePath)) {
            is RemoteFileCatalogResult.Success -> RemotePaneState.Loaded(result.entries)
            is RemoteFileCatalogResult.Failure -> RemotePaneState.Failed(result.error)
        }
    }

    val localPane: @Composable (Modifier) -> Unit = { modifier ->
        FileBrowserPane(
            title = stringResource(Res.string.my_files),
            path = localPath,
            entries = localEntries,
            emptyText = stringResource(Res.string.empty_or_limited_access),
            modifier = modifier,
            rootLabel = stringResource(Res.string.to_roots),
            onRootClick = if (localPath == null) null else {
                {
                    localPath = null
                    localPathStack = emptyList()
                    localSelection = FileBrowserSelection()
                }
            },
            onParentClick = if (localPath == null) null else {
                {
                    localPath = localPathStack.lastOrNull()
                    localPathStack = localPathStack.dropLast(1)
                    localSelection = FileBrowserSelection()
                }
            },
            onRefresh = { localReloadToken += 1 },
            selection = localSelection,
            onSelectionChange = {
                activeFilePane = FilePaneSide.Local
                localSelection = it
                remoteSelection = FileBrowserSelection()
            },
            side = FilePaneSide.Local,
            activeDragPayload = activeDragPayload,
            onDragStarted = { payload -> activeDragPayload = payload },
            onDragEnded = { activeDragPayload = null },
            onDrop = { payload -> requestFilePaneTransfer(payload, FilePaneSide.Local) },
            onDelete = { requestDelete(FilePaneSide.Local) },
            deleteKeyEnabled = activeFilePane == FilePaneSide.Local,
            onOpen = { entry ->
                localPathStack = localPathStack + localPath
                localPath = entry.path
                localSelection = FileBrowserSelection()
            },
            topRowContent = {
                if (selectedLocalEntries.isNotEmpty() && selectedDevice != null) {
                    Button(
                        onClick = {
                            requestFilePaneTransfer(
                                payload = FilePaneDragPayload(FilePaneSide.Local, selectedLocalEntries),
                                target = FilePaneSide.Remote,
                            )
                        },
                    ) {
                        Text(stringResource(Res.string.send_selected))
                    }
                }
            },
        )
    }
    val remotePane: @Composable (Modifier) -> Unit = { modifier ->
        FileBrowserPane(
            title = selectedDevice?.displayName ?: stringResource(Res.string.remote_files),
            path = remotePath,
            entries = remoteEntries,
            emptyText = if (selectedDevice == null) {
                stringResource(Res.string.start_search_first)
            } else {
                stringResource(Res.string.no_remote_files_available)
            },
            modifier = modifier,
            rootLabel = stringResource(Res.string.to_whitelist),
            isLoading = remoteState is RemotePaneState.Loading,
            errorText = (remoteState as? RemotePaneState.Failed)?.let { remoteFileCatalogErrorText(it.error) },
            onRootClick = if (remotePath == null) null else {
                {
                    remotePath = null
                    remotePathStack = emptyList()
                    remoteSelection = FileBrowserSelection()
                }
            },
            onParentClick = if (remotePath == null) null else {
                {
                    remotePath = remotePathStack.lastOrNull()
                    remotePathStack = remotePathStack.dropLast(1)
                    remoteSelection = FileBrowserSelection()
                }
            },
            onRefresh = {
                if (selectedDevice != null) remoteReloadToken += 1
            },
            selection = remoteSelection,
            onSelectionChange = {
                activeFilePane = FilePaneSide.Remote
                remoteSelection = it
                localSelection = FileBrowserSelection()
            },
            side = FilePaneSide.Remote,
            activeDragPayload = activeDragPayload,
            onDragStarted = { payload -> activeDragPayload = payload },
            onDragEnded = { activeDragPayload = null },
            onDrop = { payload -> requestFilePaneTransfer(payload, FilePaneSide.Remote) },
            onExternalDrop = { event ->
                val droppedEntries = externalFilePaneDropEntries(event, services.platform.fileSystem)
                requestFilePaneTransfer(
                    payload = FilePaneDragPayload(FilePaneSide.Local, droppedEntries),
                    target = FilePaneSide.Remote,
                )
            },
            onDelete = { requestDelete(FilePaneSide.Remote) },
            deleteKeyEnabled = activeFilePane == FilePaneSide.Remote,
            onOpen = { entry ->
                remotePathStack = remotePathStack + remotePath
                remotePath = entry.path
                remoteSelection = FileBrowserSelection()
            },
            topRowContent = {
                if (selectedRemoteEntries.isNotEmpty()) {
                    Button(
                        onClick = {
                            requestFilePaneTransfer(
                                payload = FilePaneDragPayload(FilePaneSide.Remote, selectedRemoteEntries),
                                target = FilePaneSide.Local,
                            )
                        },
                    ) {
                        Text(stringResource(Res.string.download_selected))
                    }
                }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        transferError?.let { ErrorState(it) }
        deleteError?.let { ErrorState(it) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 760.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    localPane(Modifier.fillMaxWidth())
                    remotePane(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    localPane(Modifier.weight(1f))
                    remotePane(Modifier.weight(1f))
                }
            }
        }
    }

    val itemsToConfirm = pendingDownloadItems
    if (itemsToConfirm != null) {
        AlertDialog(
            onDismissRequest = { pendingDownloadItems = null },
            title = { Text(stringResource(Res.string.archive_download_title)) },
            text = { Text(stringResource(Res.string.archive_download_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val destination = localPath
                        val device = selectedDevice
                        if (destination != null && device != null) {
                            services.transferQueue.enqueueDownload(
                                items = itemsToConfirm,
                                source = device.toTransferEndpoint(),
                                destinationDirectoryPath = destination,
                                archiveDirectories = true,
                            )
                            remoteSelection = FileBrowserSelection()
                        }
                        pendingDownloadItems = null
                    },
                ) {
                    Text(stringResource(Res.string.archive_yes))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val destination = localPath
                        val device = selectedDevice
                        if (destination != null && device != null) {
                            coroutineScope.launch {
                                val expandedItems = itemsToConfirm.flatMap { item ->
                                    collectRemoteDownloadItems(
                                        services = services,
                                        device = device,
                                        item = item,
                                        destinationDirectoryPath = destination,
                                    )
                                }
                                services.transferQueue.enqueueDownload(
                                    items = expandedItems,
                                    source = device.toTransferEndpoint(),
                                    destinationDirectoryPath = destination,
                                    archiveDirectories = false,
                                )
                                remoteSelection = FileBrowserSelection()
                            }
                        }
                        pendingDownloadItems = null
                    },
                ) {
                    Text(stringResource(Res.string.archive_no))
                }
            },
        )
    }

    val localItemsToDelete = pendingLocalDeleteEntries
    if (localItemsToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingLocalDeleteEntries = null },
            title = { Text(stringResource(Res.string.local_delete_title)) },
            text = { Text(stringResource(Res.string.local_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val result = services.fileSystem.deleteLocalToTrash(localItemsToDelete.map { it.path })
                        setDeleteProblem(result)
                        if (result.hasDeletedEntries) {
                            localSelection = FileBrowserSelection()
                            localReloadToken += 1
                        }
                        pendingLocalDeleteEntries = null
                    },
                ) {
                    Text(stringResource(Res.string.delete_policy_trash))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingLocalDeleteEntries = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun FileBrowserPane(
    title: String,
    path: String?,
    entries: List<FileEntry>,
    emptyText: String,
    modifier: Modifier = Modifier,
    rootLabel: String,
    isLoading: Boolean = false,
    errorText: String? = null,
    onRootClick: (() -> Unit)?,
    onParentClick: (() -> Unit)?,
    onRefresh: () -> Unit,
    selection: FileBrowserSelection,
    onSelectionChange: (FileBrowserSelection) -> Unit,
    side: FilePaneSide,
    activeDragPayload: FilePaneDragPayload?,
    onDragStarted: (FilePaneDragPayload) -> Unit,
    onDragEnded: () -> Unit,
    onDrop: (FilePaneDragPayload) -> Boolean,
    onExternalDrop: ((DragAndDropEvent) -> Boolean)? = null,
    onDelete: () -> Unit,
    deleteKeyEnabled: Boolean,
    onOpen: (FileEntry) -> Unit,
    topRowContent: @Composable RowScope.() -> Unit = {},
) {
    var isDropTargetHovered by remember(side) { mutableStateOf(false) }
    val latestDragPayload = rememberUpdatedState(activeDragPayload)
    val latestOnDragEnded = rememberUpdatedState(onDragEnded)
    val latestOnDrop = rememberUpdatedState(onDrop)
    val latestOnExternalDrop = rememberUpdatedState(onExternalDrop)
    val dropTarget = remember(side) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDropTargetHovered = false
                val payload = latestDragPayload.value
                val consumed = if (payload != null) {
                    latestOnDrop.value(payload)
                } else {
                    latestOnExternalDrop.value?.invoke(event) == true
                }
                latestOnDragEnded.value()
                return consumed
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDropTargetHovered = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDropTargetHovered = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDropTargetHovered = false
                latestOnDragEnded.value()
            }
        }
    }

    Surface(
        modifier = modifier
            .heightIn(min = 420.dp)
            .onPreviewKeyEvent { event ->
                if (deleteKeyEnabled && event.type == KeyEventType.KeyDown && event.key == Key.Delete) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    activeDragPayload?.canDropOn(side) == true ||
                        onExternalDrop != null && isExternalFilePaneDropEvent(event)
                },
                target = dropTarget,
            ),
        color = if (isDropTargetHovered) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isDropTargetHovered) 2.dp else 1.dp,
            color = if (isDropTargetHovered) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = path.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                onRootClick?.let { click ->
                    OutlinedButton(onClick = click) {
                        Text(rootLabel)
                    }
                }
                topRowContent()
                OutlinedButton(onClick = onRefresh) {
                    Text(stringResource(Res.string.refresh))
                }
            }
            when {
                errorText != null -> ErrorState(errorText)
                isLoading -> EmptyState(stringResource(Res.string.loading))
                entries.isEmpty() && onParentClick == null -> EmptyState(emptyText)
                else -> FileEntryTable(
                    entries = entries,
                    onParentClick = onParentClick,
                    selection = selection,
                    onSelectionChange = onSelectionChange,
                    side = side,
                    onDragStarted = onDragStarted,
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun FileEntryTable(
    entries: List<FileEntry>,
    onParentClick: (() -> Unit)?,
    selection: FileBrowserSelection,
    onSelectionChange: (FileBrowserSelection) -> Unit,
    side: FilePaneSide,
    onDragStarted: (FilePaneDragPayload) -> Unit,
    onOpen: (FileEntry) -> Unit,
) {
    val rowIds = remember(entries, onParentClick != null) {
        buildList {
            if (onParentClick != null) add(ParentEntryRowId)
            addAll(entries.map { it.path })
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        FileEntryTableHeader()
        HorizontalDivider()
        onParentClick?.let { click ->
            FileEntryParentRow(
                isSelected = ParentEntryRowId in selection.selectedRowIds,
                onSelect = { modifiers ->
                    onSelectionChange(
                        selection.updatedSelection(
                            rowIds = rowIds,
                            rowId = ParentEntryRowId,
                            modifiers = modifiers,
                        ),
                    )
                },
                onOpen = click,
            )
            HorizontalDivider()
        }
        entries.forEach { entry ->
            FileEntryTableRow(
                entry = entry,
                isSelected = entry.path in selection.selectedRowIds,
                side = side,
                dragEntries = {
                    filePaneDragEntries(
                        entries = entries,
                        selectedRowIds = selection.selectedRowIds,
                        draggedRowId = entry.path,
                    )
                },
                onDragStarted = onDragStarted,
                onSelect = { modifiers ->
                    onSelectionChange(
                        selection.updatedSelection(
                            rowIds = rowIds,
                            rowId = entry.path,
                            modifiers = modifiers,
                        ),
                    )
                },
                onOpen = onOpen,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun FileEntryParentRow(
    isSelected: Boolean,
    onSelect: (FileSelectionModifiers) -> Unit,
    onOpen: () -> Unit,
) {
    var clickModifiers by remember { mutableStateOf(FileSelectionModifiers()) }
    val rowColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .captureFileSelectionModifiers { clickModifiers = it }
            .combinedClickable(
                onClick = { onSelect(clickModifiers) },
                onDoubleClick = {
                    onOpen()
                },
            )
            .background(rowColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "..",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.file_type_folder),
            modifier = Modifier.width(86.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(96.dp))
    }
}

@Composable
private fun FileEntryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.name_column),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.type_column),
            modifier = Modifier.width(86.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.size_column),
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FileEntryTableRow(
    entry: FileEntry,
    isSelected: Boolean,
    side: FilePaneSide,
    dragEntries: () -> List<FileEntry>,
    onDragStarted: (FilePaneDragPayload) -> Unit,
    onSelect: (FileSelectionModifiers) -> Unit,
    onOpen: (FileEntry) -> Unit,
) {
    var clickModifiers by remember(entry.path) { mutableStateOf(FileSelectionModifiers()) }
    val rowColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .captureFileSelectionModifiers { clickModifiers = it }
            .filePaneDragSource(
                side = side,
                dragEntries = dragEntries,
                onDragStarted = onDragStarted,
            )
            .combinedClickable(
                onClick = { onSelect(clickModifiers) },
                onDoubleClick = {
                    if (entry.isBrowseable) {
                        onOpen(entry)
                    }
                },
            )
            .background(rowColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = fileEntryTypeLabel(entry.type),
            modifier = Modifier.width(86.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = fileSizeLabel(entry),
            modifier = Modifier.width(96.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Modifier.filePaneDragSource(
    side: FilePaneSide,
    dragEntries: () -> List<FileEntry>,
    onDragStarted: (FilePaneDragPayload) -> Unit,
): Modifier =
    dragAndDropSource(transferData = {
        val entries = dragEntries()
        if (entries.isEmpty()) {
            null
        } else {
            onDragStarted(FilePaneDragPayload(side, entries))
            createFilePaneDragTransferData()
        }
    })

private fun Modifier.captureFileSelectionModifiers(
    onModifiersChange: (FileSelectionModifiers) -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) {
                    val keyboardModifiers = event.keyboardModifiers
                    onModifiersChange(
                        FileSelectionModifiers(
                            isRangeSelection = keyboardModifiers.isShiftPressed,
                            isToggleSelection = keyboardModifiers.isCtrlPressed || keyboardModifiers.isMetaPressed,
                        ),
                    )
                }
            }
        }
    }

private fun FileBrowserSelection.updatedSelection(
    rowIds: List<String>,
    rowId: String,
    modifiers: FileSelectionModifiers,
): FileBrowserSelection {
    val rowIdSet = rowIds.toSet()
    val validSelection = selectedRowIds.intersect(rowIdSet)
    if (rowId !in rowIdSet) {
        return copy(selectedRowIds = validSelection)
    }

    if (modifiers.isRangeSelection) {
        val anchor = anchorRowId?.takeIf { it in rowIdSet } ?: rowId
        val anchorIndex = rowIds.indexOf(anchor)
        val rowIndex = rowIds.indexOf(rowId)
        val start = if (anchorIndex <= rowIndex) anchorIndex else rowIndex
        val end = if (anchorIndex <= rowIndex) rowIndex else anchorIndex
        val rangeSelection = rowIds.subList(start, end + 1).toSet()
        return FileBrowserSelection(
            selectedRowIds = if (modifiers.isToggleSelection) {
                validSelection + rangeSelection
            } else {
                rangeSelection
            },
            anchorRowId = anchor,
        )
    }

    if (modifiers.isToggleSelection) {
        return FileBrowserSelection(
            selectedRowIds = if (rowId in validSelection) {
                validSelection - rowId
            } else {
                validSelection + rowId
            },
            anchorRowId = rowId,
        )
    }

    return FileBrowserSelection(
        selectedRowIds = setOf(rowId),
        anchorRowId = rowId,
    )
}

private suspend fun collectRemoteDownloadItems(
    services: AppServices,
    device: DiscoveredDevice,
    item: TransferItem,
    destinationDirectoryPath: String,
): List<TransferItem> {
    if (!item.isDirectory) {
        return listOf(item.copy(destinationDirectoryPath = destinationDirectoryPath))
    }

    val directoryDestination = services.platform.fileSystem.childPath(destinationDirectoryPath, item.displayName)
    val children = when (val result = services.remoteFileCatalog.browse(device, item.path)) {
        is RemoteFileCatalogResult.Success -> result.entries
        is RemoteFileCatalogResult.Failure -> emptyList()
    }
    return children.flatMap { child ->
        collectRemoteDownloadItems(
            services = services,
            device = device,
            item = child.toTransferItem(),
            destinationDirectoryPath = directoryDestination,
        )
    }
}

@Composable
private fun fileSizeLabel(entry: FileEntry): String =
    entry.sizeBytes?.let { size -> stringResource(Res.string.file_size_bytes, size.toString()) }.orEmpty()

@Composable
private fun remoteFileCatalogErrorText(error: RemoteFileCatalogError): String =
    when (error.code) {
        RemoteFileCatalogErrorCode.Unauthorized -> stringResource(Res.string.remote_error_unauthorized)
        RemoteFileCatalogErrorCode.OutsideWhitelist -> stringResource(Res.string.remote_error_outside_whitelist)
        RemoteFileCatalogErrorCode.BadRequest -> stringResource(Res.string.remote_error_bad_request)
        RemoteFileCatalogErrorCode.Network -> stringResource(Res.string.remote_error_network, error.message)
        RemoteFileCatalogErrorCode.ServerUnavailable -> stringResource(
            Res.string.remote_error_server_unavailable,
            error.message,
        )
    }

@Composable
private fun SettingsSection(services: AppServices) {
    val settings by services.settings.settings.collectAsState()
    val whitelist by services.fileSystem.whitelist.collectAsState()
    val backupRecords by services.backups.records.collectAsState()
    val deviceDisplayName = settings.deviceDisplayNameOr(services.platform.deviceInfo.displayName)

    var deviceNameDraft by remember { mutableStateOf(deviceDisplayName) }
    var showKeyword by remember { mutableStateOf(false) }
    var keywordDraft by remember { mutableStateOf(settings.discoveryKeyword) }

    LaunchedEffect(deviceDisplayName) {
        deviceNameDraft = deviceDisplayName
    }

    LaunchedEffect(settings.discoveryKeyword) {
        keywordDraft = settings.discoveryKeyword
    }

    SectionColumn(title = stringResource(Res.string.settings_title)) {
        DeviceNameSettings(
            deviceName = deviceDisplayName,
            deviceNameDraft = deviceNameDraft,
            onDeviceNameDraftChange = { value ->
                deviceNameDraft = value.filterNot { char -> char == '\n' || char == '\r' }
            },
            onSave = {
                if (services.settings.updateDeviceDisplayName(deviceNameDraft)) {
                    val updatedSettings = services.settings.settings.value
                    if (services.discovery.isRunning.value) {
                        startLocalDiscovery(
                            services = services,
                            keyword = updatedSettings.discoveryKeyword,
                            deviceDisplayName = updatedSettings.deviceDisplayNameOr(services.platform.deviceInfo.displayName),
                        )
                    }
                }
            },
            onCancel = { deviceNameDraft = deviceDisplayName },
        )
        HorizontalDivider()
        LanguageSettings(
            currentMode = settings.languageMode,
            onModeChange = services.settings::updateLanguageMode,
        )
        HorizontalDivider()
        KeywordSettings(
            keyword = settings.discoveryKeyword,
            keywordDraft = keywordDraft,
            showKeyword = showKeyword,
            onShowKeywordChange = { showKeyword = it },
            onKeywordDraftChange = { value ->
                keywordDraft = value.filter(::isDiscoveryKeywordChar)
            },
            onSave = {
                if (services.settings.updateDiscoveryKeyword(keywordDraft)) {
                    showKeyword = false
                }
            },
        )
        HorizontalDivider()
        TransferSettings(
            maxOutgoing = settings.maxOutgoingTransfers,
            maxIncoming = settings.maxIncomingTransfers,
            onMaxOutgoingChange = services.settings::updateMaxOutgoingTransfers,
            onMaxIncomingChange = services.settings::updateMaxIncomingTransfers,
        )
        HorizontalDivider()
        BackupSettings(
            mode = settings.backupMode,
            onModeChange = services.settings::updateBackupMode,
        )
        HorizontalDivider()
        RemoteDeleteSettings(
            policy = settings.remoteDeletePolicy,
            supportsTrash = services.platform.fileSystem.supportsTrash,
            onPolicyChange = services.settings::updateRemoteDeletePolicy,
        )
        HorizontalDivider()
        BackupArchiveSettings(
            records = backupRecords,
            onRestore = { record -> services.backups.restore(record.id) },
            onDelete = { record -> services.backups.delete(record.id) },
        )
        HorizontalDivider()
        WhitelistSettings(
            whitelist = whitelist,
            services = services,
        )
    }
}

@Composable
private fun DeviceNameSettings(
    deviceName: String,
    deviceNameDraft: String,
    onDeviceNameDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val normalizedDraft = deviceNameDraft.trim()
    val canSave = normalizedDraft.isNotBlank() && normalizedDraft != deviceName

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = deviceNameDraft,
            onValueChange = onDeviceNameDraftChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = deviceNameDraft.isNotBlank() && normalizedDraft.isBlank(),
            label = { Text(stringResource(Res.string.device_name)) },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = canSave,
                onClick = onSave,
            ) {
                Text(stringResource(Res.string.save))
            }
            OutlinedButton(
                enabled = deviceNameDraft != deviceName,
                onClick = onCancel,
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    }
}

@Composable
private fun LanguageSettings(
    currentMode: AppLanguageMode,
    onModeChange: (AppLanguageMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageModeButton(
                mode = AppLanguageMode.System,
                label = stringResource(Res.string.language_system),
                selectedMode = currentMode,
                onModeChange = onModeChange,
            )
            LanguageModeButton(
                mode = AppLanguageMode.English,
                label = stringResource(Res.string.language_english),
                selectedMode = currentMode,
                onModeChange = onModeChange,
            )
            LanguageModeButton(
                mode = AppLanguageMode.Ukrainian,
                label = stringResource(Res.string.language_ukrainian),
                selectedMode = currentMode,
                onModeChange = onModeChange,
            )
        }
    }
}

@Composable
private fun LanguageModeButton(
    mode: AppLanguageMode,
    label: String,
    selectedMode: AppLanguageMode,
    onModeChange: (AppLanguageMode) -> Unit,
) {
    if (selectedMode == mode) {
        Button(onClick = { onModeChange(mode) }) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = { onModeChange(mode) }) {
            Text(label)
        }
    }
}

@Composable
private fun KeywordSettings(
    keyword: String,
    keywordDraft: String,
    showKeyword: Boolean,
    onShowKeywordChange: (Boolean) -> Unit,
    onKeywordDraftChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val canSave = isValidDiscoveryKeyword(keywordDraft) && keywordDraft.trim() != keyword
    val allowedCharacters = stringResource(Res.string.keyword_allowed_characters)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = if (showKeyword) keywordDraft else "******",
            onValueChange = {
                if (showKeyword) onKeywordDraftChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            readOnly = !showKeyword,
            singleLine = true,
            isError = showKeyword && keywordDraft.isNotBlank() && !isValidDiscoveryKeyword(keywordDraft),
            label = { Text(stringResource(Res.string.keyword)) },
            trailingIcon = {
                IconButton(onClick = { onShowKeywordChange(!showKeyword) }) {
                    EyeIcon(visible = showKeyword)
                }
            },
            supportingText = {
                if (showKeyword) {
                    Text(stringResource(Res.string.keyword_allowed_template, allowedCharacters))
                }
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = showKeyword && canSave,
                onClick = onSave,
            ) {
                Text(stringResource(Res.string.save))
            }
            OutlinedButton(
                enabled = showKeyword && keywordDraft != keyword,
                onClick = { onKeywordDraftChange(keyword) },
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    }
}

@Composable
private fun TransferSettings(
    maxOutgoing: Int,
    maxIncoming: Int,
    onMaxOutgoingChange: (Int) -> Unit,
    onMaxIncomingChange: (Int) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.transfer_limits)) {
        TransferLimitRow(
            label = stringResource(Res.string.max_outgoing_transfers),
            value = maxOutgoing,
            onValueChange = onMaxOutgoingChange,
        )
        TransferLimitRow(
            label = stringResource(Res.string.max_incoming_transfers),
            value = maxIncoming,
            onValueChange = onMaxIncomingChange,
        )
    }
}

@Composable
private fun TransferLimitRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    RowSurface {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { onValueChange(value - 1) }, enabled = value > 1) {
            Text("-")
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onValueChange(value + 1) }) {
            Text("+")
        }
    }
}

@Composable
private fun BackupSettings(
    mode: BackupMode,
    onModeChange: (BackupMode) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.backup_mode)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BackupModeButton(BackupMode.BackupFolder, stringResource(Res.string.backup_folder), mode, onModeChange)
            BackupModeButton(BackupMode.BackupOnlyFile, stringResource(Res.string.backup_file), mode, onModeChange)
            BackupModeButton(BackupMode.DoNotBackup, stringResource(Res.string.backup_none), mode, onModeChange)
        }
    }
}

@Composable
private fun BackupModeButton(
    mode: BackupMode,
    label: String,
    selectedMode: BackupMode,
    onModeChange: (BackupMode) -> Unit,
) {
    if (mode == selectedMode) {
        Button(onClick = { onModeChange(mode) }) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = { onModeChange(mode) }) {
            Text(label)
        }
    }
}

@Composable
private fun RemoteDeleteSettings(
    policy: RemoteDeletePolicy,
    supportsTrash: Boolean,
    onPolicyChange: (RemoteDeletePolicy) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.remote_delete_policy)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RemoteDeletePolicyButton(
                policy = RemoteDeletePolicy.DoNothing,
                label = stringResource(Res.string.delete_policy_none),
                selectedPolicy = policy,
                onPolicyChange = onPolicyChange,
            )
            RemoteDeletePolicyButton(
                policy = RemoteDeletePolicy.Ask,
                label = stringResource(Res.string.delete_policy_ask),
                selectedPolicy = policy,
                onPolicyChange = onPolicyChange,
            )
            if (supportsTrash) {
                RemoteDeletePolicyButton(
                    policy = RemoteDeletePolicy.Trash,
                    label = stringResource(Res.string.delete_policy_trash),
                    selectedPolicy = policy,
                    onPolicyChange = onPolicyChange,
                )
            }
            RemoteDeletePolicyButton(
                policy = RemoteDeletePolicy.Permanent,
                label = stringResource(Res.string.delete_policy_permanent),
                selectedPolicy = policy,
                onPolicyChange = onPolicyChange,
            )
        }
    }
}

@Composable
private fun RemoteDeletePolicyButton(
    policy: RemoteDeletePolicy,
    label: String,
    selectedPolicy: RemoteDeletePolicy,
    onPolicyChange: (RemoteDeletePolicy) -> Unit,
) {
    if (policy == selectedPolicy) {
        Button(onClick = { onPolicyChange(policy) }) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = { onPolicyChange(policy) }) {
            Text(label)
        }
    }
}

@Composable
private fun BackupArchiveSettings(
    records: List<com.dimonoso.crosplatformfilesender.backup.BackupRecord>,
    onRestore: (com.dimonoso.crosplatformfilesender.backup.BackupRecord) -> Unit,
    onDelete: (com.dimonoso.crosplatformfilesender.backup.BackupRecord) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.backup_archives)) {
        if (records.isEmpty()) {
            EmptyState(stringResource(Res.string.backup_empty))
        } else {
            records.forEach { record ->
                RowSurface {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.originalTargetPath, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = record.archivePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = { onRestore(record) }) {
                        Text(stringResource(Res.string.restore))
                    }
                    TextButton(onClick = { onDelete(record) }) {
                        Text(stringResource(Res.string.delete))
                    }
                }
            }
        }
    }
}

@Composable
private fun WhitelistSettings(
    whitelist: List<WhitelistFolder>,
    services: AppServices,
) {
    SectionColumn(title = stringResource(Res.string.whitelist_folders)) {
        Button(
            enabled = services.folderPicker.isAvailable,
            onClick = {
                services.folderPicker.pickFolder { folder ->
                    services.fileSystem.addWhitelistFolder(
                        WhitelistFolder(
                            id = "whitelist-${folder.path.hashCode()}",
                            displayName = folder.displayName,
                            path = folder.path,
                        ),
                    )
                }
            },
        ) {
            Text(stringResource(Res.string.add_folder))
        }
        if (whitelist.isEmpty()) {
            EmptyState(stringResource(Res.string.whitelist_empty))
        } else {
            whitelist.forEach { folder ->
                WhitelistRow(
                    folder = folder,
                    canMoveFolderToTrash = services.platform.fileSystem.canMoveToTrash(folder.path),
                    onEnabledChange = { enabled -> services.fileSystem.setWhitelistEnabled(folder.id, enabled) },
                    onDeletePolicyChange = { deletePolicy ->
                        services.fileSystem.setWhitelistDeletePolicyOverride(folder.id, deletePolicy)
                    },
                    onRemove = { services.fileSystem.removeWhitelistFolder(folder.id) },
                )
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    services: AppServices,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SettingsSection(services)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun QueueDialog(
    services: AppServices,
    onDismiss: () -> Unit,
) {
    val tasks by services.transferQueue.tasks.collectAsState()
    val progress = remember(tasks) { queueProgress(tasks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.transfer_queue)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                progress?.let {
                    LinearProgressIndicator(
                        progress = { it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (tasks.isEmpty()) {
                    EmptyState(stringResource(Res.string.queue_empty))
                } else {
                    tasks.forEach { task ->
                        val direction = transferDirectionLabel(task.direction)
                        TaskRow(
                            taskId = task.id,
                            title = task.item.displayName,
                            status = task.status,
                            subtitle = stringResource(
                                Res.string.transfer_subtitle,
                                direction,
                                task.source.displayName,
                                task.target.displayName,
                            ),
                            progressBytes = task.progressBytes,
                            totalBytes = task.totalBytes,
                            errorMessage = task.errorMessage,
                            onPause = { services.transferQueue.pause(task.id) },
                            onResume = { services.transferQueue.resume(task.id) },
                            onCancel = { services.transferQueue.cancel(task.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun SectionColumn(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        content()
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DeviceDropZone(
    device: DiscoveredDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    if (isSelected) {
        Button(onClick = onClick) {
            Text(device.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(device.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DeviceRow(device: DiscoveredDevice) {
    RowSurface {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                text = "${device.platformName} | ${device.host}:${device.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(text = device.protocol.name)
    }
}

@Composable
private fun FileEntryRow(
    entry: FileEntry,
    trailing: @Composable (() -> Unit)? = null,
) {
    RowSurface {
        val typeLabel = fileEntryTypeLabel(entry.type)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.file_entry_subtitle, typeLabel, entry.path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun fileEntryTypeLabel(type: FileEntryType): String =
    when (type) {
        FileEntryType.Drive -> stringResource(Res.string.file_type_drive)
        FileEntryType.Directory -> stringResource(Res.string.file_type_folder)
        FileEntryType.File -> stringResource(Res.string.file_type_file)
        FileEntryType.Unknown -> stringResource(Res.string.file_type_other)
    }

@Composable
private fun WhitelistRow(
    folder: WhitelistFolder,
    canMoveFolderToTrash: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDeletePolicyChange: (WhitelistDeletePolicyOverride) -> Unit,
    onRemove: () -> Unit,
) {
    RowSurface {
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.UseDefault,
                    label = stringResource(Res.string.delete_policy_default),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.DoNothing,
                    label = stringResource(Res.string.delete_policy_none),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.Ask,
                    label = stringResource(Res.string.delete_policy_ask),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                if (canMoveFolderToTrash) {
                    WhitelistDeletePolicyButton(
                        policy = WhitelistDeletePolicyOverride.Trash,
                        label = stringResource(Res.string.delete_policy_trash),
                        selectedPolicy = folder.deletePolicyOverride,
                        onPolicyChange = onDeletePolicyChange,
                    )
                }
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.Permanent,
                    label = stringResource(Res.string.delete_policy_permanent),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
            }
        }
        Switch(
            checked = folder.enabled,
            onCheckedChange = onEnabledChange,
        )
        TextButton(onClick = onRemove) {
            Text(stringResource(Res.string.remove))
        }
    }
}

@Composable
private fun WhitelistDeletePolicyButton(
    policy: WhitelistDeletePolicyOverride,
    label: String,
    selectedPolicy: WhitelistDeletePolicyOverride,
    onPolicyChange: (WhitelistDeletePolicyOverride) -> Unit,
) {
    if (policy == selectedPolicy) {
        Button(onClick = { onPolicyChange(policy) }) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = { onPolicyChange(policy) }) {
            Text(label)
        }
    }
}

@Composable
private fun PendingRemoteDeleteDialog(services: AppServices) {
    val prompt by services.remoteFileCatalog.pendingDeletePrompt.collectAsState()
    val pending = prompt ?: return

    AlertDialog(
        onDismissRequest = {
            services.remoteFileCatalog.resolvePendingDeletePrompt(pending.id, RemoteDeletePromptDecision.Cancel)
        },
        title = { Text(stringResource(Res.string.remote_delete_prompt_title)) },
        text = { Text(stringResource(Res.string.remote_delete_prompt_message, pending.paths.size.toString())) },
        confirmButton = {
            Button(
                onClick = {
                    services.remoteFileCatalog.resolvePendingDeletePrompt(
                        pending.id,
                        RemoteDeletePromptDecision.Permanent,
                    )
                },
            ) {
                Text(stringResource(Res.string.delete_policy_permanent))
            }
        },
        dismissButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pending.canMoveAllToTrash) {
                    OutlinedButton(
                        onClick = {
                            services.remoteFileCatalog.resolvePendingDeletePrompt(
                                pending.id,
                                RemoteDeletePromptDecision.Trash,
                            )
                        },
                    ) {
                        Text(stringResource(Res.string.delete_policy_trash))
                    }
                }
                OutlinedButton(
                    onClick = {
                        services.remoteFileCatalog.resolvePendingDeletePrompt(
                            pending.id,
                            RemoteDeletePromptDecision.Cancel,
                        )
                    },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        },
    )
}

@Composable
private fun TaskRow(
    taskId: String,
    title: String,
    status: TransferStatus,
    subtitle: String,
    progressBytes: Long,
    totalBytes: Long?,
    errorMessage: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    RowSurface {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "$taskId | $subtitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            totalBytes?.let { total ->
                Text(
                    text = stringResource(Res.string.transfer_progress_bytes, progressBytes.toString(), total.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            errorMessage?.let { message ->
                Text(
                    text = stringResource(Res.string.task_error, message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        StatusPill(text = transferStatusLabel(status))
        if (status == TransferStatus.Paused) {
            TextButton(onClick = onResume) {
                Text(stringResource(Res.string.resume))
            }
        } else {
            TextButton(onClick = onPause, enabled = status != TransferStatus.Cancelled) {
                Text(stringResource(Res.string.pause))
            }
        }
        TextButton(onClick = onCancel, enabled = status != TransferStatus.Cancelled) {
            Text(stringResource(Res.string.task_cancel))
        }
    }
}

@Composable
private fun transferDirectionLabel(direction: TransferDirection): String =
    when (direction) {
        TransferDirection.Upload -> stringResource(Res.string.transfer_direction_upload)
        TransferDirection.Download -> stringResource(Res.string.transfer_direction_download)
        TransferDirection.Receive -> stringResource(Res.string.transfer_direction_download)
    }

@Composable
private fun transferStatusLabel(status: TransferStatus): String =
    when (status) {
        TransferStatus.Pending -> stringResource(Res.string.status_pending)
        TransferStatus.Preparing -> stringResource(Res.string.loading)
        TransferStatus.Running -> stringResource(Res.string.status_running)
        TransferStatus.Paused -> stringResource(Res.string.status_paused)
        TransferStatus.Completed -> stringResource(Res.string.status_completed)
        TransferStatus.Failed -> stringResource(Res.string.status_failed)
        TransferStatus.Cancelled -> stringResource(Res.string.status_cancelled)
    }

@Composable
private fun RowSurface(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EyeIcon(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.50f)
            quadraticTo(size.width * 0.30f, size.height * 0.12f, size.width * 0.50f, size.height * 0.12f)
            quadraticTo(size.width * 0.70f, size.height * 0.12f, size.width * 0.92f, size.height * 0.50f)
            quadraticTo(size.width * 0.70f, size.height * 0.88f, size.width * 0.50f, size.height * 0.88f)
            quadraticTo(size.width * 0.30f, size.height * 0.88f, size.width * 0.08f, size.height * 0.50f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth))
        drawCircle(color = color, radius = size.minDimension * 0.13f, center = center)
        if (!visible) {
            drawLine(
                color = color,
                start = Offset(size.width * 0.18f, size.height * 0.86f),
                end = Offset(size.width * 0.84f, size.height * 0.16f),
                strokeWidth = strokeWidth,
            )
        }
    }
}

private fun queueProgress(tasks: List<TransferTask>): Float? {
    val activeTasks = tasks.filter { task ->
        task.status != TransferStatus.Completed && task.status != TransferStatus.Cancelled
    }
    if (activeTasks.isEmpty()) return null

    return activeTasks
        .map { task ->
            val total = task.totalBytes
            if (total == null || total <= 0L) {
                0f
            } else {
                (task.progressBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        }
        .average()
        .toFloat()
}
