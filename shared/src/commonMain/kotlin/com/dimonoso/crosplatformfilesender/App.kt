@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dimonoso.crosplatformfilesender.archive.ArchiveFormat
import com.dimonoso.crosplatformfilesender.discovery.DiscoveredDevice
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryError
import com.dimonoso.crosplatformfilesender.discovery.DiscoveryErrorType
import com.dimonoso.crosplatformfilesender.discovery.toLocalDiscoveryConfig
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import com.dimonoso.crosplatformfilesender.localization.AppLocaleEnvironment
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.settings.AppLanguageMode
import com.dimonoso.crosplatformfilesender.settings.getSystemLanguageCode
import com.dimonoso.crosplatformfilesender.settings.isDiscoveryKeywordChar
import com.dimonoso.crosplatformfilesender.settings.isValidDiscoveryKeyword
import com.dimonoso.crosplatformfilesender.settings.resolveAppLanguage
import com.dimonoso.crosplatformfilesender.transfer.TransferDirection
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferItem
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus
import com.dimonoso.crosplatformfilesender.transfer.TransferTask
import crosplatformfilesender.shared.generated.resources.Res
import crosplatformfilesender.shared.generated.resources.add_folder
import crosplatformfilesender.shared.generated.resources.add_upload
import crosplatformfilesender.shared.generated.resources.app_title
import crosplatformfilesender.shared.generated.resources.archives
import crosplatformfilesender.shared.generated.resources.cancel
import crosplatformfilesender.shared.generated.resources.close
import crosplatformfilesender.shared.generated.resources.device
import crosplatformfilesender.shared.generated.resources.device_id
import crosplatformfilesender.shared.generated.resources.discovery
import crosplatformfilesender.shared.generated.resources.discovery_error_port
import crosplatformfilesender.shared.generated.resources.discovery_error_stopped
import crosplatformfilesender.shared.generated.resources.discovery_error_unavailable
import crosplatformfilesender.shared.generated.resources.drop
import crosplatformfilesender.shared.generated.resources.empty_or_limited_access
import crosplatformfilesender.shared.generated.resources.file_entry_subtitle
import crosplatformfilesender.shared.generated.resources.file_type_drive
import crosplatformfilesender.shared.generated.resources.file_type_file
import crosplatformfilesender.shared.generated.resources.file_type_folder
import crosplatformfilesender.shared.generated.resources.file_type_other
import crosplatformfilesender.shared.generated.resources.filesystem
import crosplatformfilesender.shared.generated.resources.found_devices
import crosplatformfilesender.shared.generated.resources.full_filesystem
import crosplatformfilesender.shared.generated.resources.keyword
import crosplatformfilesender.shared.generated.resources.keyword_allowed_characters
import crosplatformfilesender.shared.generated.resources.keyword_allowed_template
import crosplatformfilesender.shared.generated.resources.language
import crosplatformfilesender.shared.generated.resources.language_english
import crosplatformfilesender.shared.generated.resources.language_system
import crosplatformfilesender.shared.generated.resources.language_ukrainian
import crosplatformfilesender.shared.generated.resources.manual_peer
import crosplatformfilesender.shared.generated.resources.my_files
import crosplatformfilesender.shared.generated.resources.no_active_whitelist_folders
import crosplatformfilesender.shared.generated.resources.no_found_devices
import crosplatformfilesender.shared.generated.resources.no_remote_files_available
import crosplatformfilesender.shared.generated.resources.open
import crosplatformfilesender.shared.generated.resources.pause
import crosplatformfilesender.shared.generated.resources.persistence
import crosplatformfilesender.shared.generated.resources.platform
import crosplatformfilesender.shared.generated.resources.queue
import crosplatformfilesender.shared.generated.resources.queue_empty
import crosplatformfilesender.shared.generated.resources.queue_progress
import crosplatformfilesender.shared.generated.resources.remote_files
import crosplatformfilesender.shared.generated.resources.remove
import crosplatformfilesender.shared.generated.resources.request_download
import crosplatformfilesender.shared.generated.resources.resume
import crosplatformfilesender.shared.generated.resources.save
import crosplatformfilesender.shared.generated.resources.scoped_storage
import crosplatformfilesender.shared.generated.resources.search_active
import crosplatformfilesender.shared.generated.resources.search_off
import crosplatformfilesender.shared.generated.resources.settings_tab
import crosplatformfilesender.shared.generated.resources.settings_title
import crosplatformfilesender.shared.generated.resources.start
import crosplatformfilesender.shared.generated.resources.start_search_first
import crosplatformfilesender.shared.generated.resources.status_cancelled
import crosplatformfilesender.shared.generated.resources.status_completed
import crosplatformfilesender.shared.generated.resources.status_failed
import crosplatformfilesender.shared.generated.resources.status_paused
import crosplatformfilesender.shared.generated.resources.status_pending
import crosplatformfilesender.shared.generated.resources.status_running
import crosplatformfilesender.shared.generated.resources.stop
import crosplatformfilesender.shared.generated.resources.task_cancel
import crosplatformfilesender.shared.generated.resources.to_roots
import crosplatformfilesender.shared.generated.resources.transfer_direction_download
import crosplatformfilesender.shared.generated.resources.transfer_direction_upload
import crosplatformfilesender.shared.generated.resources.transfer_queue
import crosplatformfilesender.shared.generated.resources.transfer_subtitle
import crosplatformfilesender.shared.generated.resources.udp_lan
import crosplatformfilesender.shared.generated.resources.whitelist_empty
import crosplatformfilesender.shared.generated.resources.whitelist_folders
import crosplatformfilesender.shared.generated.resources.workspace_tab
import crosplatformfilesender.shared.generated.resources.workspace_title
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

private enum class AppSection {
    Workspace,
    Settings,
}

@Composable
@Preview
fun App() {
    val services = remember { createAppServices() }
    val settings by services.settings.settings.collectAsState()
    val systemLanguageCode = remember { getSystemLanguageCode() }
    val resolvedLanguage = resolveAppLanguage(settings.languageMode, systemLanguageCode)
    var selectedSection by remember { mutableStateOf(AppSection.Workspace) }
    var showQueueDialog by remember { mutableStateOf(false) }

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
                    )
                    PrimaryTabRow(selectedTabIndex = selectedSection.ordinal) {
                        AppSection.entries.forEach { section ->
                            Tab(
                                selected = selectedSection == section,
                                onClick = { selectedSection = section },
                                text = {
                                    Text(
                                        text = sectionTitle(section),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (selectedSection) {
                            AppSection.Workspace -> WorkspaceSection(services)
                            AppSection.Settings -> SettingsSection(services)
                        }
                    }
                }

                if (showQueueDialog) {
                    QueueDialog(
                        services = services,
                        onDismiss = { showQueueDialog = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun sectionTitle(section: AppSection): String =
    when (section) {
        AppSection.Workspace -> stringResource(Res.string.workspace_tab)
        AppSection.Settings -> stringResource(Res.string.settings_tab)
    }

@Composable
private fun AppHeader(
    services: AppServices,
    onQueueClick: () -> Unit,
) {
    val networkState = remember(services) { services.platform.networkPermissions.currentState() }
    val accessLabel = when (services.platform.fileSystem.accessPolicy) {
        FileSystemAccessPolicy.FullFileSystem -> stringResource(Res.string.full_filesystem)
        FileSystemAccessPolicy.ScopedStorage -> stringResource(Res.string.scoped_storage)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.app_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = services.platform.deviceInfo.platformName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            QueueButton(
                services = services,
                onClick = onQueueClick,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(text = services.platform.deviceInfo.displayName)
            StatusPill(text = accessLabel)
            StatusPill(text = networkState.statusLabel)
            StatusPill(text = stringResource(Res.string.udp_lan))
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
    val configSummary by services.discovery.activeConfigSummary.collectAsState()
    val lastError by services.discovery.lastError.collectAsState()

    SectionColumn(title = stringResource(Res.string.workspace_title)) {
        DiscoveryControls(
            services = services,
            keyword = settings.discoveryKeyword,
            isRunning = isRunning,
            configSummary = configSummary,
            lastError = lastError,
        )
        HorizontalDivider()
        DevicesPanel(devices)
        HorizontalDivider()
        LocalFilesPanel(services)
        HorizontalDivider()
        RemoteFilesPanel(services)
    }
}

@Composable
private fun DiscoveryControls(
    services: AppServices,
    keyword: String,
    isRunning: Boolean,
    configSummary: String?,
    lastError: DiscoveryError?,
) {
    val canStart = isValidDiscoveryKeyword(keyword)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            enabled = isRunning || canStart,
            onClick = {
                if (isRunning) {
                    services.discovery.stop()
                } else {
                    services.discovery.start(
                        services.platform.deviceInfo.toLocalDiscoveryConfig(keyword),
                    )
                }
            },
        ) {
            Text(if (isRunning) stringResource(Res.string.stop) else stringResource(Res.string.start))
        }
        StatusPill(
            text = if (isRunning) {
                stringResource(Res.string.search_active)
            } else {
                stringResource(Res.string.search_off)
            },
        )
    }

    lastError?.let { error ->
        ErrorState(discoveryErrorText(error))
    } ?: configSummary?.let { summary ->
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
private fun DevicesPanel(devices: List<DiscoveredDevice>) {
    SectionColumn(title = stringResource(Res.string.found_devices)) {
        if (devices.isEmpty()) {
            EmptyState(stringResource(Res.string.no_found_devices))
        } else {
            devices.forEach { device ->
                DeviceDropZone(device)
            }
        }
    }
}

@Composable
private fun LocalFilesPanel(services: AppServices) {
    val roots = remember(services) { services.fileSystem.localRoots() }
    var currentPath by remember { mutableStateOf<String?>(null) }
    val entries = currentPath?.let(services.fileSystem::browseLocal) ?: roots

    SectionColumn(title = currentPath ?: stringResource(Res.string.my_files)) {
        if (currentPath != null) {
            OutlinedButton(onClick = { currentPath = null }) {
                Text(stringResource(Res.string.to_roots))
            }
        }
        if (entries.isEmpty()) {
            EmptyState(stringResource(Res.string.empty_or_limited_access))
        } else {
            entries.forEach { entry ->
                FileEntryRow(
                    entry = entry,
                    trailing = {
                        if (entry.isBrowseable) {
                            TextButton(onClick = { currentPath = entry.path }) {
                                Text(stringResource(Res.string.open))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RemoteFilesPanel(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val whitelist by services.fileSystem.whitelist.collectAsState()
    val selectedDevice = devices.firstOrNull()
    val entries = selectedDevice?.let { services.fileSystem.browseRemote(it) }.orEmpty()

    SectionColumn(title = stringResource(Res.string.remote_files)) {
        if (selectedDevice == null) {
            EmptyState(stringResource(Res.string.start_search_first))
        } else {
            DeviceRow(selectedDevice)
            HorizontalDivider()
            if (whitelist.none { it.enabled }) {
                EmptyState(stringResource(Res.string.no_active_whitelist_folders))
            } else if (entries.isEmpty()) {
                EmptyState(stringResource(Res.string.no_remote_files_available))
            } else {
                entries.forEach { entry ->
                    FileEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(services: AppServices) {
    val settings by services.settings.settings.collectAsState()
    val whitelist by services.fileSystem.whitelist.collectAsState()
    val networkState = remember(services) { services.platform.networkPermissions.currentState() }
    val archiveFormats = remember(services) {
        services.archive.supportedFormats.joinToString { format ->
            when (format) {
                ArchiveFormat.ZIP -> "ZIP"
            }
        }
    }

    var showKeyword by remember { mutableStateOf(false) }
    var keywordDraft by remember { mutableStateOf(settings.discoveryKeyword) }

    LaunchedEffect(settings.discoveryKeyword) {
        keywordDraft = settings.discoveryKeyword
    }

    SectionColumn(title = stringResource(Res.string.settings_title)) {
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
        WhitelistSettings(
            whitelist = whitelist,
            services = services,
        )
        HorizontalDivider()
        SettingRow(label = stringResource(Res.string.platform), value = services.platform.deviceInfo.platformName)
        SettingRow(label = stringResource(Res.string.device), value = services.platform.deviceInfo.displayName)
        SettingRow(label = stringResource(Res.string.device_id), value = settings.localDeviceId)
        SettingRow(label = stringResource(Res.string.filesystem), value = services.platform.fileSystem.accessPolicy.name)
        SettingRow(label = stringResource(Res.string.discovery), value = networkState.statusLabel)
        SettingRow(label = stringResource(Res.string.archives), value = archiveFormats)
        SettingRow(label = stringResource(Res.string.persistence), value = "settings.config")
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
                    onEnabledChange = { enabled -> services.fileSystem.setWhitelistEnabled(folder.id, enabled) },
                    onRemove = { services.fileSystem.removeWhitelistFolder(folder.id) },
                )
            }
        }
    }
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
                QueueActions(services)
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
private fun QueueActions(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val manualPeerLabel = stringResource(Res.string.manual_peer)
    val target = devices.firstOrNull()?.let { TransferEndpoint(it.id, it.displayName) }
        ?: TransferEndpoint("manual-peer", manualPeerLabel)

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                services.transferQueue.enqueueUpload(
                    item = TransferItem(
                        path = "/local/demo-file.zip",
                        displayName = "demo-file.zip",
                        isDirectory = false,
                        sizeBytes = 25_000_000,
                    ),
                    target = target,
                )
            },
        ) {
            Text(stringResource(Res.string.add_upload))
        }
        OutlinedButton(
            onClick = {
                services.transferQueue.enqueueDownload(
                    item = TransferItem(
                        path = "/remote/photos",
                        displayName = "photos",
                        isDirectory = true,
                    ),
                    source = target,
                    destinationPath = "/downloads",
                )
            },
        ) {
            Text(stringResource(Res.string.request_download))
        }
    }
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
private fun DeviceDropZone(device: DiscoveredDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = "${device.platformName} | ${device.host}:${device.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(text = stringResource(Res.string.drop))
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
    onEnabledChange: (Boolean) -> Unit,
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
private fun TaskRow(
    taskId: String,
    title: String,
    status: TransferStatus,
    subtitle: String,
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
    }

@Composable
private fun transferStatusLabel(status: TransferStatus): String =
    when (status) {
        TransferStatus.Pending -> stringResource(Res.string.status_pending)
        TransferStatus.Running -> stringResource(Res.string.status_running)
        TransferStatus.Paused -> stringResource(Res.string.status_paused)
        TransferStatus.Completed -> stringResource(Res.string.status_completed)
        TransferStatus.Failed -> stringResource(Res.string.status_failed)
        TransferStatus.Cancelled -> stringResource(Res.string.status_cancelled)
    }

@Composable
private fun SettingRow(label: String, value: String) {
    RowSurface {
        Text(
            text = label,
            modifier = Modifier.width(160.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
