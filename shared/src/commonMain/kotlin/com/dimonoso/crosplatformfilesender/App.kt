@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferItem
import com.dimonoso.crosplatformfilesender.transfer.TransferStatus

private enum class AppSection(val title: String) {
    Devices("Пристрої"),
    LocalFiles("Мої файли"),
    Whitelist("Whitelist"),
    RemoteBrowse("Віддалено"),
    Queue("Черга"),
    Settings("Налаштування"),
}

@Composable
@Preview
fun App() {
    val services = remember { createAppServices() }
    var selectedSection by remember { mutableStateOf(AppSection.Devices) }

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
                AppHeader(services)
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
                        AppSection.Devices -> DevicesSection(services)
                        AppSection.LocalFiles -> LocalFilesSection(services)
                        AppSection.Whitelist -> WhitelistSection(services)
                        AppSection.RemoteBrowse -> RemoteBrowseSection(services)
                        AppSection.Queue -> QueueSection(services)
                        AppSection.Settings -> SettingsSection(services)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader(services: AppServices) {
    val networkState = remember(services) { services.platform.networkPermissions.currentState() }
    val accessLabel = when (services.platform.fileSystem.accessPolicy) {
        FileSystemAccessPolicy.FullFileSystem -> "Повний filesystem"
        FileSystemAccessPolicy.ScopedStorage -> "Scoped storage"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            StatusPill(text = networkState.statusLabel)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(text = services.platform.deviceInfo.displayName)
            StatusPill(text = accessLabel)
            StatusPill(text = "UDP LAN discovery")
            StatusPill(text = "ZIP")
        }
    }
}

@Composable
private fun DevicesSection(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val isRunning by services.discovery.isRunning.collectAsState()
    val configSummary by services.discovery.activeConfigSummary.collectAsState()
    var keyword by remember { mutableStateOf("local-secret") }

    SectionColumn(title = "Пошук пристроїв") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Ключове слово") },
            )
            Button(
                enabled = keyword.isNotBlank(),
                onClick = {
                    services.discovery.start(
                        services.platform.deviceInfo.toLocalDiscoveryConfig(keyword.trim()),
                    )
                },
            ) {
                Text(if (isRunning) "Оновити" else "Старт")
            }
            OutlinedButton(onClick = services.discovery::stop, enabled = isRunning) {
                Text("Стоп")
            }
        }
        configSummary?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (devices.isEmpty()) {
            EmptyState("Немає знайдених пристроїв")
        } else {
            devices.forEach { device ->
                DeviceRow(device)
            }
        }
    }
}

@Composable
private fun LocalFilesSection(services: AppServices) {
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
private fun WhitelistSection(services: AppServices) {
    val roots = remember(services) { services.fileSystem.localRoots() }
    val whitelist by services.fileSystem.whitelist.collectAsState()

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
        HorizontalDivider()
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
private fun RemoteBrowseSection(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val whitelist by services.fileSystem.whitelist.collectAsState()
    val selectedDevice = devices.firstOrNull()
    val entries = selectedDevice?.let { services.fileSystem.browseRemote(it) }.orEmpty()

    SectionColumn(title = "Віддалений перегляд") {
        if (selectedDevice == null) {
            EmptyState("Спершу запустіть пошук пристроїв")
        } else {
            DeviceRow(selectedDevice)
            HorizontalDivider()
            if (whitelist.none { it.enabled }) {
                EmptyState("Немає активних whitelist папок")
            } else {
                entries.forEach { entry ->
                    FileEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun QueueSection(services: AppServices) {
    val devices by services.discovery.devices.collectAsState()
    val tasks by services.transferQueue.tasks.collectAsState()
    val target = devices.firstOrNull()?.let { TransferEndpoint(it.id, it.displayName) }
        ?: TransferEndpoint("manual-peer", "Manual peer")

    SectionColumn(title = "Черга передач") {
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
}

@Composable
private fun SettingsSection(services: AppServices) {
    val networkState = remember(services) { services.platform.networkPermissions.currentState() }
    val archiveFormats = remember(services) {
        services.archive.supportedFormats.joinToString { format ->
            when (format) {
                ArchiveFormat.ZIP -> "ZIP"
            }
        }
    }

    SectionColumn(title = "Налаштування каркаса") {
        SettingRow(label = "Платформа", value = services.platform.deviceInfo.platformName)
        SettingRow(label = "Пристрій", value = services.platform.deviceInfo.displayName)
        SettingRow(label = "Filesystem", value = services.platform.fileSystem.accessPolicy.name)
        SettingRow(label = "Discovery", value = networkState.statusLabel)
        SettingRow(label = "Архіви", value = archiveFormats)
        SettingRow(label = "Persistence черги", value = "In-memory")
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
