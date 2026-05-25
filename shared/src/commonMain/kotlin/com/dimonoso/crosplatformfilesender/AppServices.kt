package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.archive.ArchiveService
import com.dimonoso.crosplatformfilesender.archive.createArchiveService
import com.dimonoso.crosplatformfilesender.backup.BackupService
import com.dimonoso.crosplatformfilesender.backup.PersistentBackupService
import com.dimonoso.crosplatformfilesender.backup.createBackupStore
import com.dimonoso.crosplatformfilesender.discovery.DeviceDiscoveryService
import com.dimonoso.crosplatformfilesender.discovery.createDeviceDiscoveryService
import com.dimonoso.crosplatformfilesender.filesystem.FileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.InMemoryFileSystemService
import com.dimonoso.crosplatformfilesender.platform.PlatformServices
import com.dimonoso.crosplatformfilesender.platform.PlatformFolderPicker
import com.dimonoso.crosplatformfilesender.platform.createPlatformFolderPicker
import com.dimonoso.crosplatformfilesender.platform.createPlatformServices
import com.dimonoso.crosplatformfilesender.remote.RemoteFileCatalogService
import com.dimonoso.crosplatformfilesender.remote.createRemoteFileCatalogService
import com.dimonoso.crosplatformfilesender.settings.SettingsService
import com.dimonoso.crosplatformfilesender.settings.createSettingsService
import com.dimonoso.crosplatformfilesender.transfer.NetworkTransferQueueService
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferQueueService

data class AppServices(
    val platform: PlatformServices,
    val folderPicker: PlatformFolderPicker,
    val settings: SettingsService,
    val discovery: DeviceDiscoveryService,
    val fileSystem: FileSystemService,
    val remoteFileCatalog: RemoteFileCatalogService,
    val transferQueue: TransferQueueService,
    val archive: ArchiveService,
    val backups: BackupService,
    val pendingShare: PendingShareService,
)

fun createAppServices(): AppServices {
    val platformServices = createPlatformServices()
    val settings = createSettingsService()
    if (settings.settings.value.deviceDisplayName.isBlank()) {
        settings.updateDeviceDisplayName(platformServices.deviceInfo.displayName)
    }
    val platform = platformServices.copy(
        deviceInfo = platformServices.deviceInfo.copy(
            id = settings.settings.value.localDeviceId,
        ),
    )
    val localEndpoint = TransferEndpoint(
        deviceId = platform.deviceInfo.id,
        displayName = settings.settings.value.deviceDisplayName.ifBlank { platform.deviceInfo.displayName },
    )

    val fileSystem = InMemoryFileSystemService(
        platformFileSystem = platform.fileSystem,
        initialWhitelist = settings.settings.value.whitelistFolders,
        onWhitelistChanged = settings::updateWhitelistFolders,
    )
    val archive = createArchiveService(platform.fileSystem)
    val backups = PersistentBackupService(
        fileSystem = platform.fileSystem,
        archiveService = archive,
        store = createBackupStore(),
        timeProvider = ::currentTimeMillis,
    )

    return AppServices(
        platform = platform,
        folderPicker = createPlatformFolderPicker(),
        settings = settings,
        discovery = createDeviceDiscoveryService(
            deviceInfo = platform.deviceInfo,
            networkPermissionGateway = platform.networkPermissions,
        ),
        fileSystem = fileSystem,
        remoteFileCatalog = createRemoteFileCatalogService(
            fileSystem = fileSystem,
            keywordProvider = { settings.settings.value.discoveryKeyword },
            deletePolicyProvider = { settings.settings.value.remoteDeletePolicy },
        ),
        transferQueue = NetworkTransferQueueService(
            localEndpoint = localEndpoint,
            platformFileSystem = platform.fileSystem,
            archiveService = archive,
            backupService = backups,
            settingsService = settings,
            keywordProvider = { settings.settings.value.discoveryKeyword },
            timeProvider = ::currentTimeMillis,
        ),
        archive = archive,
        backups = backups,
        pendingShare = createPendingShareService(),
    )
}
