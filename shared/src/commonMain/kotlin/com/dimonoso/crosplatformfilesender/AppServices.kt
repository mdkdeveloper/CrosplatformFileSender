package com.dimonoso.crosplatformfilesender

import com.dimonoso.crosplatformfilesender.archive.ArchiveService
import com.dimonoso.crosplatformfilesender.archive.StubArchiveService
import com.dimonoso.crosplatformfilesender.discovery.DeviceDiscoveryService
import com.dimonoso.crosplatformfilesender.discovery.createDeviceDiscoveryService
import com.dimonoso.crosplatformfilesender.filesystem.FileSystemService
import com.dimonoso.crosplatformfilesender.filesystem.InMemoryFileSystemService
import com.dimonoso.crosplatformfilesender.platform.PlatformServices
import com.dimonoso.crosplatformfilesender.platform.createPlatformServices
import com.dimonoso.crosplatformfilesender.settings.SettingsService
import com.dimonoso.crosplatformfilesender.settings.createSettingsService
import com.dimonoso.crosplatformfilesender.transfer.InMemoryTransferQueueService
import com.dimonoso.crosplatformfilesender.transfer.TransferEndpoint
import com.dimonoso.crosplatformfilesender.transfer.TransferQueueService

data class AppServices(
    val platform: PlatformServices,
    val settings: SettingsService,
    val discovery: DeviceDiscoveryService,
    val fileSystem: FileSystemService,
    val transferQueue: TransferQueueService,
    val archive: ArchiveService,
)

fun createAppServices(): AppServices {
    val settings = createSettingsService()
    val platformServices = createPlatformServices()
    val platform = platformServices.copy(
        deviceInfo = platformServices.deviceInfo.copy(
            id = settings.settings.value.localDeviceId,
        ),
    )
    val localEndpoint = TransferEndpoint(
        deviceId = platform.deviceInfo.id,
        displayName = platform.deviceInfo.displayName,
    )

    return AppServices(
        platform = platform,
        settings = settings,
        discovery = createDeviceDiscoveryService(
            deviceInfo = platform.deviceInfo,
            networkPermissionGateway = platform.networkPermissions,
        ),
        fileSystem = InMemoryFileSystemService(
            platformFileSystem = platform.fileSystem,
            initialWhitelist = settings.settings.value.whitelistFolders,
            onWhitelistChanged = settings::updateWhitelistFolders,
        ),
        transferQueue = InMemoryTransferQueueService(localEndpoint),
        archive = StubArchiveService(),
    )
}
