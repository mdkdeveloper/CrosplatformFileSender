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
import com.dimonoso.crosplatformfilesender.discovery.toLocalDiscoveryConfig
import com.dimonoso.crosplatformfilesender.filesystem.FileEntry
import com.dimonoso.crosplatformfilesender.filesystem.FileEntryType
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import com.dimonoso.crosplatformfilesender.platform.FileSystemAccessPolicy
import com.dimonoso.crosplatformfilesender.settings.discoveryKeywordAllowedCharactersLabel
import com.dimonoso.crosplatformfilesender.settings.isDiscoveryKeywordChar
import com.dimonoso.crosplatformfilesender.settings.isValidDiscoveryKeyword
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferItem
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus
import com.dimonoso.crosplatformfilesender.transfer.TransferTask
import kotlin.math.roundToInt

private enum class AppSection(val title: String) {
    Workspace("Файли"),
    Settings("Налаштування"),
}

@Composable
@Preview
fun App() {
    val services = remember { createAppServices() }
    var selectedSection by remember { mutableStateOf(AppSection.Workspace) }
    var showQueueDialog by remember { mutableStateOf(false) }

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
                                    text = section.title,
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

@Composable
private fun AppHeader(
    services: AppServices,
    onQueueClick: () -> Unit,
) {
    val networkState = remember(services) { services.platform.networkPermissions.currentState() }
    val accessLabel = when (services.platform.fileSystem.accessPolicy) {
        FileSystemAccessPolicy.FullFileSystem -> "Повний filesystem"
        FileSystemAccessPolicy.ScopedStorage -> "Scoped storage"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Crossplatform File Sender",
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
            StatusPill(text = "UDP LAN")
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
    val label = progress?.let { "Черга ${(it * 100).roundToInt()}%" } ?: "Черга"

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

    SectionColumn(title = "Пристрої та файли") {
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
    lastError: String?,
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
            Text(if (isRunning) "Стоп" else "Старт")
        }
        StatusPill(text = if (isRunning) "Пошук активний" else "Пошук вимкнено")
    }

    lastError?.let { error ->
        ErrorState(error)
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
private fun DevicesPanel(devices: List<DiscoveredDevice>) {
    SectionColumn(title = "Знайдені пристрої") {
        if (devices.isEmpty()) {
            EmptyState("Немає знайдених пристроїв")
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

    SectionColumn(title = currentPath ?: "Мої файли") {
        if (currentPath != null) {
            OutlinedButton(onClick = { currentPath = null }) {
                Text("До коренів")
            }
        }
        if (entries.isEmpty()) {
            EmptyState("Порожньо або доступ обмежено")
        } else {
            entries.forEach { entry ->
                FileEntryRow(
                    entry = entry,
                    trailing = {
                        if (entry.isBrowseable) {
                            TextButton(onClick = { currentPath = entry.path }) {
                                Text("Відкрити")
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

    SectionColumn(title = "Віддалені файли") {
        if (selectedDevice == null) {
            EmptyState("Спершу запустіть пошук пристроїв")
        } else {
            DeviceRow(selectedDevice)
            HorizontalDivider()
            if (whitelist.none { it.enabled }) {
                EmptyState("Немає активних whitelist папок")
            } else if (entries.isEmpty()) {
                EmptyState("Немає доступних віддалених файлів")
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
    val roots = remember(services) { services.fileSystem.localRoots() }
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

    SectionColumn(title = "Налаштування") {
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
            roots = roots,
            whitelist = whitelist,
            services = services,
        )
        HorizontalDivider()
        SettingRow(label = "Платформа", value = services.platform.deviceInfo.platformName)
        SettingRow(label = "Пристрій", value = services.platform.deviceInfo.displayName)
        SettingRow(label = "Device ID", value = settings.localDeviceId)
        SettingRow(label = "Filesystem", value = services.platform.fileSystem.accessPolicy.name)
        SettingRow(label = "Discovery", value = networkState.statusLabel)
        SettingRow(label = "Архіви", value = archiveFormats)
        SettingRow(label = "Persistence", value = "settings.config")
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
            label = { Text("Ключове слово") },
            trailingIcon = {
                IconButton(onClick = { onShowKeywordChange(!showKeyword) }) {
                    EyeIcon(visible = showKeyword)
                }
            },
            supportingText = {
                if (showKeyword) {
                    Text("Дозволено: ${discoveryKeywordAllowedCharactersLabel()}")
                }
            },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = showKeyword && canSave,
                onClick = onSave,
            ) {
                Text("Зберегти")
            }
            OutlinedButton(
                enabled = showKeyword && keywordDraft != keyword,
                onClick = { onKeywordDraftChange(keyword) },
            ) {
                Text("Скасувати")
            }
        }
    }
}

@Composable
private fun WhitelistSettings(
    roots: List<FileEntry>,
    whitelist: List<WhitelistFolder>,
    services: AppServices,
) {
    SectionColumn(title = "Whitelist папок і дисків") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            roots.forEach { root ->
                OutlinedButton(
                    onClick = {
                        services.fileSystem.addWhitelistFolder(
                            WhitelistFolder(
                                id = "whitelist-${root.path.hashCode()}",
                                displayName = root.name,
                                path = root.path,
                            ),
                        )
                    },
                ) {
                    Text("Додати ${root.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (whitelist.isEmpty()) {
            EmptyState("Whitelist порожній")
        } else {
            whitelist.forEach { folder ->
                WhitelistRow(
                    folder = folder,
                    onToggle = { services.fileSystem.setWhitelistEnabled(folder.id, !folder.enabled) },
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
        title = { Text("Черга передач") },
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
                    EmptyState("Черга порожня")
                } else {
                    tasks.forEach { task ->
                        TaskRow(
                            taskId = task.id,
                            title = task.item.displayName,
                            status = task.status,
                            subtitle = "${task.direction}: ${task.source.displayName} -> ${task.target.displayName}",
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
                Text("Закрити")
            }
        },
    )
}

@Composable
private fun QueueActions(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val target = devices.firstOrNull()?.let { TransferEndpoint(it.id, it.displayName) }
        ?: TransferEndpoint("manual-peer", "Manual peer")

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
            Text("Додати відправку")
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
            Text("Запитати отримання")
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
            StatusPill(text = "Drop")
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
        val typeLabel = when (entry.type) {
            FileEntryType.Drive -> "Диск"
            FileEntryType.Directory -> "Папка"
            FileEntryType.File -> "Файл"
            FileEntryType.Unknown -> "Інше"
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "$typeLabel | ${entry.path}",
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
private fun WhitelistRow(
    folder: WhitelistFolder,
    onToggle: () -> Unit,
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
        OutlinedButton(onClick = onToggle) {
            Text(if (folder.enabled) "Вимкнути" else "Увімкнути")
        }
        TextButton(onClick = onRemove) {
            Text("Прибрати")
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
        StatusPill(text = status.name)
        if (status == TransferStatus.Paused) {
            TextButton(onClick = onResume) {
                Text("Resume")
            }
        } else {
            TextButton(onClick = onPause, enabled = status != TransferStatus.Cancelled) {
                Text("Pause")
            }
        }
        TextButton(onClick = onCancel, enabled = status != TransferStatus.Cancelled) {
            Text("Cancel")
        }
    }
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
