@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import com.dimonoso.crosplatformfilesender.filesystem.isLocalWhitelistRootPath
import com.dimonoso.crosplatformfilesender.localization.AppLocaleEnvironment
import com.dimonoso.crosplatformfilesender.platform.PlatformFamily
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogError
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogErrorCode
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogResult
import com.dimonoso.crosplatformfilesender.remote.RemoteFileDeleteResult
import com.dimonoso.crosplatformfilesender.remote.RemoteDeletePromptDecision
import com.dimonoso.crosplatformfilesender.settings.AppLanguageMode
import com.dimonoso.crosplatformfilesender.settings.AppSettings
import com.dimonoso.crosplatformfilesender.settings.AppThemeMode
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
import crosplatformfilesender.shared.generated.resources.android_storage_access_button
import crosplatformfilesender.shared.generated.resources.android_storage_access_message
import crosplatformfilesender.shared.generated.resources.archive_download_message
import crosplatformfilesender.shared.generated.resources.archive_download_title
import crosplatformfilesender.shared.generated.resources.archive_no
import crosplatformfilesender.shared.generated.resources.archive_yes
import crosplatformfilesender.shared.generated.resources.auto_search_devices_on_startup
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
import crosplatformfilesender.shared.generated.resources.local_whitelist_root
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
import crosplatformfilesender.shared.generated.resources.theme
import crosplatformfilesender.shared.generated.resources.theme_dark
import crosplatformfilesender.shared.generated.resources.theme_light
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
import crosplatformfilesender.shared.generated.resources.whitelist_path_unavailable
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
    val colorScheme = when (settings.themeMode) {
        AppThemeMode.Dark -> darkColorScheme()
        AppThemeMode.Light -> lightColorScheme()
    }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(services) {
        val startupSettings = services.settings.settings.value
        if (startupSettings.autoSearchDevicesOnStartup) {
            startLocalDiscovery(
                services = services,
                keyword = startupSettings.discoveryKeyword,
                deviceDisplayName = startupSettings.deviceDisplayNameOr(services.platform.deviceInfo.displayName),
            )
        }
    }

    AppLocaleEnvironment(resolvedLanguage.localeTag) {
        MaterialTheme(colorScheme = colorScheme) {
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
        IconActionButton(
            label = stringResource(Res.string.settings_tab),
            onClick = onSettingsClick,
            icon = { SettingsIcon() },
        )
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
        IconActionButton(
            label = label,
            enabled = enabled,
            onClick = onClick,
            variant = IconButtonVariant.Filled,
            icon = { StopIcon() },
        )
    } else {
        IconActionButton(
            label = label,
            enabled = enabled,
            onClick = onClick,
            icon = { SearchIcon() },
        )
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

    IconActionButton(
        label = label,
        onClick = onClick,
        icon = { QueueIcon() },
    )
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
private val FileSelectionCheckboxColumnWidth = 48.dp

@Composable
private fun FileBrowserColumns(
    services: AppServices,
    selectedDevice: DiscoveredDevice?,
    keyword: String,
) {
    var localReloadToken by remember { mutableStateOf(0) }
    val whitelist by services.fileSystem.whitelist.collectAsState()
    val localWhitelistRootName = stringResource(Res.string.local_whitelist_root)
    val localRoots = remember(services, localReloadToken, whitelist, localWhitelistRootName) {
        services.fileSystem.localRoots().map { entry ->
            if (isLocalWhitelistRootPath(entry.path)) {
                entry.copy(name = localWhitelistRootName)
            } else {
                entry
            }
        }
    }
    var localPath by remember { mutableStateOf<String?>(null) }
    var localPathStack by remember { mutableStateOf<List<String?>>(emptyList()) }
    var localSelection by remember { mutableStateOf(FileBrowserSelection()) }
    var activeFilePane by remember { mutableStateOf<FilePaneSide?>(null) }
    var remotePath by remember(selectedDevice?.id) { mutableStateOf<String?>(null) }
    var remotePathStack by remember(selectedDevice?.id) { mutableStateOf<List<String?>>(emptyList()) }
    var remoteSelection by remember(selectedDevice?.id) { mutableStateOf(FileBrowserSelection()) }
    var remoteReloadToken by remember(selectedDevice?.id) { mutableStateOf(0) }
    var remoteState by remember(selectedDevice?.id) { mutableStateOf<RemotePaneState>(RemotePaneState.Idle) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var localPathError by remember { mutableStateOf<String?>(null) }
    var remotePathError by remember(selectedDevice?.id) { mutableStateOf<String?>(null) }
    var pendingLocalDeleteEntries by remember { mutableStateOf<List<FileEntry>?>(null) }
    var pendingDownloadItems by remember { mutableStateOf<List<TransferItem>?>(null) }
    var activeDragPayload by remember { mutableStateOf<FilePaneDragPayload?>(null) }
    var localAutoRefreshRequest by remember { mutableStateOf(0) }
    var remoteAutoRefreshRequest by remember(selectedDevice?.id) { mutableStateOf(0) }
    val transferTasks by services.transferQueue.tasks.collectAsState()
    val storageAccessState by services.platform.storageAccess.state.collectAsState()
    val autoRefreshTracker = remember(services.transferQueue) { TransferPaneAutoRefreshTracker() }
    val coroutineScope = rememberCoroutineScope()
    val invalidDestinationText = stringResource(Res.string.invalid_transfer_destination)
    val deleteProblemText = stringResource(Res.string.delete_problem)
    val pathUnavailableText = stringResource(Res.string.whitelist_path_unavailable)
    val shouldPromptStorageAccess = storageAccessState.requiresRuntimeApproval && !storageAccessState.isGranted
    val useAndroidFileSelection = services.platform.deviceInfo.family == PlatformFamily.Android
    val localDisplayPath = if (localPath?.let(::isLocalWhitelistRootPath) == true) {
        localWhitelistRootName
    } else {
        localPath
    }

    val localEntries = remember(localPath, localReloadToken, localRoots) {
        localPath?.let(services.fileSystem::browseLocal) ?: localRoots
    }
    val remoteEntries = (remoteState as? RemotePaneState.Loaded)?.entries.orEmpty()
    val selectedLocalEntries = localEntries.filter {
        it.path in localSelection.selectedRowIds && !isLocalWhitelistRootPath(it.path)
    }
    val selectedRemoteEntries = remoteEntries.filter { it.path in remoteSelection.selectedRowIds }

    LaunchedEffect(localPath, localRoots) {
        if (localPath?.let(::isLocalWhitelistRootPath) == true &&
            localRoots.none { entry -> isLocalWhitelistRootPath(entry.path) }
        ) {
            localPath = null
            localPathStack = emptyList()
            localSelection = FileBrowserSelection()
        }
    }

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

    fun applyLocalPathInput(input: String) {
        val requestedPath = input.trim()
        if (requestedPath.isEmpty()) {
            localPath = null
            localPathStack = emptyList()
            localSelection = FileBrowserSelection()
            localPathError = null
            return
        }

        val metadata = services.platform.fileSystem.metadata(requestedPath)
        if (metadata?.isBrowseable == true) {
            localPath = metadata.path
            localPathStack = emptyList()
            localSelection = FileBrowserSelection()
            localPathError = null
        } else {
            localPathError = pathUnavailableText
        }
    }

    fun applyRemotePathInput(input: String) {
        val requestedPath = input.trim()
        if (requestedPath.isEmpty()) {
            remotePath = null
            remotePathStack = emptyList()
            remoteSelection = FileBrowserSelection()
            remotePathError = null
            return
        }

        val device = selectedDevice
        if (device == null) {
            remotePathError = pathUnavailableText
            return
        }

        coroutineScope.launch {
            when (val result = services.remoteFileCatalog.browse(device, requestedPath)) {
                is RemoteFileCatalogResult.Success -> {
                    remotePath = requestedPath
                    remotePathStack = emptyList()
                    remoteSelection = FileBrowserSelection()
                    remotePathError = null
                    remoteState = RemotePaneState.Loaded(result.entries)
                }
                is RemoteFileCatalogResult.Failure -> {
                    remotePathError = pathUnavailableText
                }
            }
        }
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
            path = localDisplayPath,
            entries = localEntries,
            emptyText = stringResource(Res.string.empty_or_limited_access),
            modifier = modifier,
            rootLabel = stringResource(Res.string.to_roots),
            pathError = localPathError,
            onPathSubmit = ::applyLocalPathInput,
            onRootClick = if (localPath == null) null else {
                {
                    localPath = null
                    localPathStack = emptyList()
                    localSelection = FileBrowserSelection()
                    localPathError = null
                }
            },
            onParentClick = if (localPath == null) null else {
                {
                    localPath = localPathStack.lastOrNull()
                    localPathStack = localPathStack.dropLast(1)
                    localSelection = FileBrowserSelection()
                    localPathError = null
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
                localPathError = null
            },
            useAndroidFileSelection = useAndroidFileSelection,
            noticeContent = if (shouldPromptStorageAccess) {
                {
                    StorageAccessNotice(
                        message = stringResource(Res.string.android_storage_access_message),
                        buttonText = stringResource(Res.string.android_storage_access_button),
                        onClick = services.platform.storageAccess::requestAccess,
                    )
                }
            } else {
                null
            },
            topRowContent = {
                if (selectedLocalEntries.isNotEmpty() && selectedDevice != null) {
                    IconActionButton(
                        label = stringResource(Res.string.send_selected),
                        onClick = {
                            requestFilePaneTransfer(
                                payload = FilePaneDragPayload(FilePaneSide.Local, selectedLocalEntries),
                                target = FilePaneSide.Remote,
                            )
                        },
                        variant = IconButtonVariant.Filled,
                        icon = { UploadIcon() },
                    )
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
            pathError = remotePathError,
            onPathSubmit = ::applyRemotePathInput,
            onRootClick = if (remotePath == null) null else {
                {
                    remotePath = null
                    remotePathStack = emptyList()
                    remoteSelection = FileBrowserSelection()
                    remotePathError = null
                }
            },
            onParentClick = if (remotePath == null) null else {
                {
                    remotePath = remotePathStack.lastOrNull()
                    remotePathStack = remotePathStack.dropLast(1)
                    remoteSelection = FileBrowserSelection()
                    remotePathError = null
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
                remotePathError = null
            },
            useAndroidFileSelection = useAndroidFileSelection,
            topRowContent = {
                if (selectedRemoteEntries.isNotEmpty()) {
                    IconActionButton(
                        label = stringResource(Res.string.download_selected),
                        onClick = {
                            requestFilePaneTransfer(
                                payload = FilePaneDragPayload(FilePaneSide.Remote, selectedRemoteEntries),
                                target = FilePaneSide.Local,
                            )
                        },
                        variant = IconButtonVariant.Filled,
                        icon = { DownloadIcon() },
                    )
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
    pathError: String? = null,
    onPathSubmit: (String) -> Unit,
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
    useAndroidFileSelection: Boolean,
    noticeContent: (@Composable () -> Unit)? = null,
    topRowContent: @Composable () -> Unit = {},
) {
    var isDropTargetHovered by remember(side) { mutableStateOf(false) }
    var isPathInputFocused by remember(side) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val latestDragPayload = rememberUpdatedState(activeDragPayload)
    val latestOnDragEnded = rememberUpdatedState(onDragEnded)
    val latestOnDrop = rememberUpdatedState(onDrop)
    val latestOnExternalDrop = rememberUpdatedState(onExternalDrop)
    val onFocusedSelectionChange: (FileBrowserSelection) -> Unit = { newSelection ->
        focusRequester.requestFocus()
        onSelectionChange(newSelection)
    }
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
                if (
                    deleteKeyEnabled &&
                    !isPathInputFocused &&
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.Delete
                ) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
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
            if (useAndroidFileSelection) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FilePanePathControl(
                        title = title,
                        path = path,
                        isEditable = false,
                        pathError = pathError,
                        onFocusedChange = { isPathInputFocused = it },
                        onPathSubmit = onPathSubmit,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    onRootClick?.let { click ->
                        IconActionButton(
                            label = rootLabel,
                            onClick = click,
                            icon = { AddFolderIcon() },
                        )
                    }
                    topRowContent()
                    IconActionButton(
                        label = stringResource(Res.string.refresh),
                        onClick = onRefresh,
                        icon = { RefreshIcon() },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FilePanePathControl(
                            title = title,
                            path = path,
                            isEditable = true,
                            pathError = pathError,
                            onFocusedChange = { isPathInputFocused = it },
                            onPathSubmit = onPathSubmit,
                        )
                    }
                    onRootClick?.let { click ->
                        IconActionButton(
                            label = rootLabel,
                            onClick = click,
                            icon = { AddFolderIcon() },
                        )
                    }
                    topRowContent()
                    IconActionButton(
                        label = stringResource(Res.string.refresh),
                        onClick = onRefresh,
                        icon = { RefreshIcon() },
                    )
                }
            }
            noticeContent?.invoke()
            when {
                errorText != null -> ErrorState(errorText)
                isLoading -> EmptyState(stringResource(Res.string.loading))
                entries.isEmpty() && onParentClick == null -> EmptyState(emptyText)
                else -> key(
                    fileEntryTableRenderKey(
                        side = side,
                        path = path,
                        entries = entries,
                        hasParentRow = onParentClick != null,
                        useAndroidFileSelection = useAndroidFileSelection,
                    ),
                ) {
                    FileEntryTable(
                        entries = entries,
                        onParentClick = onParentClick,
                        selection = selection,
                        onSelectionChange = onFocusedSelectionChange,
                        side = side,
                        onDragStarted = onDragStarted,
                        onOpen = onOpen,
                        useAndroidFileSelection = useAndroidFileSelection,
                        showSelectionCheckboxes = useAndroidFileSelection && path != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilePanePathControl(
    title: String,
    path: String?,
    isEditable: Boolean,
    pathError: String?,
    onFocusedChange: (Boolean) -> Unit,
    onPathSubmit: (String) -> Unit,
) {
    val displayPath = path.orEmpty()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (isEditable) {
            EditableFilePanePath(
                path = displayPath,
                pathError = pathError,
                onFocusedChange = onFocusedChange,
                onPathSubmit = onPathSubmit,
            )
        } else {
            CopyableFilePanePath(path = displayPath)
        }
    }
}

@Composable
private fun EditableFilePanePath(
    path: String,
    pathError: String?,
    onFocusedChange: (Boolean) -> Unit,
    onPathSubmit: (String) -> Unit,
) {
    var draftPath by remember { mutableStateOf(path) }
    var isFocused by remember { mutableStateOf(false) }
    var lastSubmittedPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        draftPath = path
        lastSubmittedPath = null
    }

    fun submitDraft() {
        val submittedPath = draftPath.trim()
        if (submittedPath == path.trim() || submittedPath == lastSubmittedPath) return
        lastSubmittedPath = submittedPath
        onPathSubmit(submittedPath)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(
                1.dp,
                if (pathError == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
            ),
        ) {
            BasicTextField(
                value = draftPath,
                onValueChange = { value ->
                    draftPath = value
                    if (value.trim() != lastSubmittedPath) {
                        lastSubmittedPath = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            submitDraft()
                        }
                        isFocused = focusState.isFocused
                        onFocusedChange(focusState.isFocused)
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            submitDraft()
                            true
                        } else {
                            false
                        }
                    },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
        }
        pathError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun CopyableFilePanePath(path: String) {
    val clipboardManager = LocalClipboardManager.current
    Text(
        text = path,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (path.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(path))
                    }
                },
            ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
    useAndroidFileSelection: Boolean,
    showSelectionCheckboxes: Boolean,
) {
    val rowIds = remember(entries, onParentClick != null) {
        buildList {
            if (onParentClick != null) add(ParentEntryRowId)
            addAll(entries.map { it.path })
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        FileEntryTableHeader(showSelectionColumn = showSelectionCheckboxes)
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
                useAndroidFileSelection = useAndroidFileSelection,
                showSelectionColumn = showSelectionCheckboxes,
            )
            HorizontalDivider()
        }
        entries.forEach { entry ->
            key(fileEntryRowRenderKey(entry)) {
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
                    useAndroidFileSelection = useAndroidFileSelection,
                    showSelectionCheckbox = showSelectionCheckboxes,
                )
                HorizontalDivider()
            }
        }
    }
}

private fun fileEntryTableRenderKey(
    side: FilePaneSide,
    path: String?,
    entries: List<FileEntry>,
    hasParentRow: Boolean,
    useAndroidFileSelection: Boolean,
): String =
    buildString {
        append(side.name)
        append('|')
        append(path.orEmpty())
        append('|')
        append(hasParentRow)
        append('|')
        append(useAndroidFileSelection)
        entries.forEach { entry ->
            append('|')
            append(fileEntryRowRenderKey(entry))
        }
    }

private fun fileEntryRowRenderKey(entry: FileEntry): String =
    buildString {
        append(entry.path)
        append('|')
        append(entry.name)
        append('|')
        append(entry.type.name)
        append('|')
        append(entry.sizeBytes ?: "")
        append('|')
        append(entry.isBrowseable)
    }

@Composable
private fun FileEntryParentRow(
    isSelected: Boolean,
    onSelect: (FileSelectionModifiers) -> Unit,
    onOpen: () -> Unit,
    useAndroidFileSelection: Boolean,
    showSelectionColumn: Boolean,
) {
    var clickModifiers by remember { mutableStateOf(FileSelectionModifiers()) }
    val rowColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val rowModifier = if (useAndroidFileSelection) {
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen)
    } else {
        Modifier
            .fillMaxWidth()
            .captureFileSelectionModifiers { clickModifiers = it }
            .combinedClickable(
                onClick = { onSelect(clickModifiers) },
                onDoubleClick = {
                    onOpen()
                },
            )
    }

    Row(
        modifier = rowModifier
            .background(rowColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSelectionColumn) {
            Spacer(modifier = Modifier.width(FileSelectionCheckboxColumnWidth))
        }
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
private fun FileEntryTableHeader(showSelectionColumn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSelectionColumn) {
            Spacer(modifier = Modifier.width(FileSelectionCheckboxColumnWidth))
        }
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
    useAndroidFileSelection: Boolean,
    showSelectionCheckbox: Boolean,
) {
    var clickModifiers by remember(entry.path) { mutableStateOf(FileSelectionModifiers()) }
    val rowColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val rowModifier = if (useAndroidFileSelection) {
        Modifier
            .fillMaxWidth()
            .filePaneDragSource(
                side = side,
                dragEntries = dragEntries,
                onDragStarted = onDragStarted,
            )
            .combinedClickable(
                onClick = {
                    if (entry.isBrowseable) {
                        onOpen(entry)
                    }
                },
            )
    } else {
        Modifier
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
    }

    Row(
        modifier = rowModifier
            .background(rowColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSelectionCheckbox) {
            Box(
                modifier = Modifier.width(FileSelectionCheckboxColumnWidth),
                contentAlignment = Alignment.Center,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        onSelect(FileSelectionModifiers(isToggleSelection = true))
                    },
                )
            }
        }
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
        ThemeSettings(
            currentMode = settings.themeMode,
            onModeChange = services.settings::updateThemeMode,
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
        AutoSearchDevicesOnStartupSettings(
            enabled = settings.autoSearchDevicesOnStartup,
            onEnabledChange = services.settings::updateAutoSearchDevicesOnStartup,
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
private fun ThemeSettings(
    currentMode: AppThemeMode,
    onModeChange: (AppThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(Res.string.theme),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeModeButton(
                mode = AppThemeMode.Dark,
                label = stringResource(Res.string.theme_dark),
                selectedMode = currentMode,
                onModeChange = onModeChange,
                icon = { MoonIcon() },
            )
            ThemeModeButton(
                mode = AppThemeMode.Light,
                label = stringResource(Res.string.theme_light),
                selectedMode = currentMode,
                onModeChange = onModeChange,
                icon = { SunIcon() },
            )
        }
    }
}

@Composable
private fun ThemeModeButton(
    mode: AppThemeMode,
    label: String,
    selectedMode: AppThemeMode,
    onModeChange: (AppThemeMode) -> Unit,
    icon: @Composable () -> Unit,
) {
    IconActionButton(
        label = label,
        onClick = { onModeChange(mode) },
        variant = if (selectedMode == mode) IconButtonVariant.Filled else IconButtonVariant.Outlined,
        icon = icon,
    )
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
private fun AutoSearchDevicesOnStartupSettings(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    RowSurface {
        Text(
            text = stringResource(Res.string.auto_search_devices_on_startup),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
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
        SingleChoiceDropdown(
            selected = mode,
            options = listOf(
                BackupMode.BackupFolder,
                BackupMode.BackupOnlyFile,
                BackupMode.DoNotBackup,
            ),
            label = { backupModeLabel(it) },
            onSelected = onModeChange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RemoteDeleteSettings(
    policy: RemoteDeletePolicy,
    supportsTrash: Boolean,
    onPolicyChange: (RemoteDeletePolicy) -> Unit,
) {
    SectionColumn(title = stringResource(Res.string.remote_delete_policy)) {
        SingleChoiceDropdown(
            selected = policy,
            options = remoteDeletePolicyOptions(supportsTrash, policy),
            label = { remoteDeletePolicyLabel(it) },
            onSelected = onPolicyChange,
            modifier = Modifier.fillMaxWidth(),
        )
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
        IconActionButton(
            label = stringResource(Res.string.add_folder),
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
            variant = IconButtonVariant.Filled,
            icon = { AddFolderIcon() },
        )
        if (whitelist.isEmpty()) {
            EmptyState(stringResource(Res.string.whitelist_empty))
        } else {
            whitelist.forEach { folder ->
                val pathAvailable = services.fileSystem.isWhitelistFolderPathAvailable(folder.id)
                WhitelistRow(
                    folder = folder,
                    pathAvailable = pathAvailable,
                    canMoveFolderToTrash = pathAvailable && services.platform.fileSystem.canMoveToTrash(folder.path),
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
internal expect fun WhitelistRow(
    folder: WhitelistFolder,
    pathAvailable: Boolean,
    canMoveFolderToTrash: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDeletePolicyChange: (WhitelistDeletePolicyOverride) -> Unit,
    onRemove: () -> Unit,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun <T> SingleChoiceDropdown(
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label(selected),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun backupModeLabel(mode: BackupMode): String =
    when (mode) {
        BackupMode.BackupFolder -> stringResource(Res.string.backup_folder)
        BackupMode.BackupOnlyFile -> stringResource(Res.string.backup_file)
        BackupMode.DoNotBackup -> stringResource(Res.string.backup_none)
    }

internal fun remoteDeletePolicyOptions(
    supportsTrash: Boolean,
    selectedPolicy: RemoteDeletePolicy,
): List<RemoteDeletePolicy> =
    buildList {
        add(RemoteDeletePolicy.DoNothing)
        add(RemoteDeletePolicy.Ask)
        if (supportsTrash || selectedPolicy == RemoteDeletePolicy.Trash) {
            add(RemoteDeletePolicy.Trash)
        }
        add(RemoteDeletePolicy.Permanent)
    }

@Composable
private fun remoteDeletePolicyLabel(policy: RemoteDeletePolicy): String =
    when (policy) {
        RemoteDeletePolicy.DoNothing -> stringResource(Res.string.delete_policy_none)
        RemoteDeletePolicy.Ask -> stringResource(Res.string.delete_policy_ask)
        RemoteDeletePolicy.Trash -> stringResource(Res.string.delete_policy_trash)
        RemoteDeletePolicy.Permanent -> stringResource(Res.string.delete_policy_permanent)
    }

internal fun whitelistDeletePolicyOptions(
    canMoveFolderToTrash: Boolean,
    selectedPolicy: WhitelistDeletePolicyOverride,
): List<WhitelistDeletePolicyOverride> =
    buildList {
        add(WhitelistDeletePolicyOverride.UseDefault)
        add(WhitelistDeletePolicyOverride.DoNothing)
        add(WhitelistDeletePolicyOverride.Ask)
        if (canMoveFolderToTrash || selectedPolicy == WhitelistDeletePolicyOverride.Trash) {
            add(WhitelistDeletePolicyOverride.Trash)
        }
        add(WhitelistDeletePolicyOverride.Permanent)
    }

@Composable
internal fun whitelistDeletePolicyLabel(policy: WhitelistDeletePolicyOverride): String =
    when (policy) {
        WhitelistDeletePolicyOverride.UseDefault -> stringResource(Res.string.delete_policy_default)
        WhitelistDeletePolicyOverride.DoNothing -> stringResource(Res.string.delete_policy_none)
        WhitelistDeletePolicyOverride.Ask -> stringResource(Res.string.delete_policy_ask)
        WhitelistDeletePolicyOverride.Trash -> stringResource(Res.string.delete_policy_trash)
        WhitelistDeletePolicyOverride.Permanent -> stringResource(Res.string.delete_policy_permanent)
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
internal fun RowSurface(content: @Composable RowScope.() -> Unit) {
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
private fun StorageAccessNotice(
    message: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onClick) {
            Text(buttonText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
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

private enum class IconButtonVariant {
    Filled,
    Outlined,
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun IconActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: IconButtonVariant = IconButtonVariant.Outlined,
    icon: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(label)
            }
        },
        state = rememberTooltipState(),
    ) {
        val buttonModifier = modifier
            .size(44.dp)
            .semantics {
                contentDescription = label
            }

        when (variant) {
            IconButtonVariant.Filled -> Button(
                enabled = enabled,
                onClick = onClick,
                modifier = buttonModifier,
                contentPadding = PaddingValues(0.dp),
            ) {
                icon()
            }
            IconButtonVariant.Outlined -> OutlinedButton(
                enabled = enabled,
                onClick = onClick,
                modifier = buttonModifier,
                contentPadding = PaddingValues(0.dp),
            ) {
                icon()
            }
        }
    }
}

@Composable
private fun MoonIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val scale = size.minDimension / 24f
        val path = Path().apply {
            moveTo(21f * scale, 12.8f * scale)
            cubicTo(20.3f * scale, 17.4f * scale, 16.4f * scale, 21f * scale, 11.6f * scale, 21f * scale)
            cubicTo(6.9f * scale, 21f * scale, 3f * scale, 17.1f * scale, 3f * scale, 12.4f * scale)
            cubicTo(3f * scale, 7.7f * scale, 6.6f * scale, 3.8f * scale, 11.2f * scale, 3f * scale)
            cubicTo(10.4f * scale, 4.1f * scale, 10f * scale, 5.5f * scale, 10f * scale, 7f * scale)
            cubicTo(10f * scale, 10.9f * scale, 13.1f * scale, 14f * scale, 17f * scale, 14f * scale)
            cubicTo(18.5f * scale, 14f * scale, 19.9f * scale, 13.6f * scale, 21f * scale, 12.8f * scale)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun SunIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.18f,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        listOf(
            Offset(0f, -1f),
            Offset(0.71f, -0.71f),
            Offset(1f, 0f),
            Offset(0.71f, 0.71f),
            Offset(0f, 1f),
            Offset(-0.71f, 0.71f),
            Offset(-1f, 0f),
            Offset(-0.71f, -0.71f),
        ).forEach { direction ->
            drawLine(
                color = color,
                start = center + direction * (size.minDimension * 0.33f),
                end = center + direction * (size.minDimension * 0.43f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SettingsIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val center = Offset(size.width * 0.5f, size.height * 0.5f)
        val outer = size.minDimension * 0.28f
        val inner = size.minDimension * 0.10f
        drawCircle(color = color, radius = outer, center = center, style = stroke)
        drawCircle(color = color, radius = inner, center = center, style = stroke)
        val tickStart = size.minDimension * 0.37f
        val tickEnd = size.minDimension * 0.45f
        listOf(
            Offset(0f, -1f) to Offset(0f, 1f),
            Offset(1f, 0f) to Offset(-1f, 0f),
            Offset(0.71f, 0.71f) to Offset(-0.71f, -0.71f),
            Offset(0.71f, -0.71f) to Offset(-0.71f, 0.71f),
        ).forEach { (positive, negative) ->
            drawLine(color, center + positive * tickStart, center + positive * tickEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, center + negative * tickStart, center + negative * tickEnd, strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun RefreshIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val scale = size.minDimension / 24f
        val path = Path().apply {
            moveTo(17.65f * scale, 6.35f * scale)
            cubicTo(16.2f * scale, 4.9f * scale, 14.21f * scale, 4f * scale, 12f * scale, 4f * scale)
            cubicTo(7.58f * scale, 4f * scale, 4.01f * scale, 7.58f * scale, 4.01f * scale, 12f * scale)
            cubicTo(4.01f * scale, 16.42f * scale, 7.58f * scale, 20f * scale, 12f * scale, 20f * scale)
            cubicTo(15.73f * scale, 20f * scale, 18.84f * scale, 17.45f * scale, 19.73f * scale, 14f * scale)
            lineTo(17.65f * scale, 14f * scale)
            cubicTo(16.83f * scale, 16.33f * scale, 14.61f * scale, 18f * scale, 12f * scale, 18f * scale)
            cubicTo(8.69f * scale, 18f * scale, 6f * scale, 15.31f * scale, 6f * scale, 12f * scale)
            cubicTo(6f * scale, 8.69f * scale, 8.69f * scale, 6f * scale, 12f * scale, 6f * scale)
            cubicTo(13.66f * scale, 6f * scale, 15.14f * scale, 6.69f * scale, 16.22f * scale, 7.78f * scale)
            lineTo(13f * scale, 11f * scale)
            lineTo(20f * scale, 11f * scale)
            lineTo(20f * scale, 4f * scale)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun QueueIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        listOf(0.28f, 0.50f, 0.72f).forEach { y ->
            drawCircle(color, radius = size.minDimension * 0.035f, center = Offset(size.width * 0.18f, size.height * y))
            drawLine(
                color,
                Offset(size.width * 0.32f, size.height * y),
                Offset(size.width * 0.84f, size.height * y),
                strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SearchIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.25f,
            center = Offset(size.width * 0.44f, size.height * 0.43f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color,
            Offset(size.width * 0.62f, size.height * 0.62f),
            Offset(size.width * 0.82f, size.height * 0.82f),
            strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun StopIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.31f, size.height * 0.31f),
            size = Size(size.width * 0.38f, size.height * 0.38f),
        )
    }
}

@Composable
private fun AddFolderIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val folder = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.34f)
            lineTo(size.width * 0.38f, size.height * 0.34f)
            lineTo(size.width * 0.46f, size.height * 0.43f)
            lineTo(size.width * 0.88f, size.height * 0.43f)
            lineTo(size.width * 0.88f, size.height * 0.78f)
            lineTo(size.width * 0.12f, size.height * 0.78f)
            close()
        }
        drawPath(folder, color = color, style = stroke)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.60f), Offset(size.width * 0.72f, size.height * 0.60f), stroke.width, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.61f, size.height * 0.49f), Offset(size.width * 0.61f, size.height * 0.71f), stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
private fun UploadIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.78f), Offset(size.width * 0.50f, size.height * 0.22f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.22f), Offset(size.width * 0.30f, size.height * 0.42f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.22f), Offset(size.width * 0.70f, size.height * 0.42f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.24f, size.height * 0.82f), Offset(size.width * 0.76f, size.height * 0.82f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
private fun DownloadIcon(modifier: Modifier = Modifier) {
    val color = LocalContentColor.current

    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 1.9.dp.toPx()
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.18f), Offset(size.width * 0.50f, size.height * 0.74f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.74f), Offset(size.width * 0.30f, size.height * 0.54f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.50f, size.height * 0.74f), Offset(size.width * 0.70f, size.height * 0.54f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.24f, size.height * 0.82f), Offset(size.width * 0.76f, size.height * 0.82f), strokeWidth, cap = StrokeCap.Round)
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
