package com.dimonoso.crosplatformfilesender.discovery

import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionGateway
import com.dimonoso.crosplatformfilesender.platform.PlatformDeviceInfo
import kotlinx.coroutines.flow.StateFlow

enum class DiscoveryProtocol {
    UdpLan,
}

data class DiscoveryConfig(
    val keyword: String,
    val deviceAlias: String,
    val udpPort: Int = 47777,
    val broadcastIntervalMillis: Long = 2_000L,
    val staleDeviceTimeoutMillis: Long = 12_000L,
    val protocol: DiscoveryProtocol = DiscoveryProtocol.UdpLan,
) {
    init {
        require(keyword.isNotBlank()) { "Discovery keyword must not be blank." }
        require(deviceAlias.isNotBlank()) { "Device alias must not be blank." }
        require(udpPort in 1..65535) { "UDP port must be between 1 and 65535." }
        require(broadcastIntervalMillis > 0) { "Broadcast interval must be positive." }
        require(staleDeviceTimeoutMillis > 0) { "Stale device timeout must be positive." }
    }

    fun toDisplaySummary(): String =
        "DiscoveryConfig(deviceAlias=$deviceAlias, udpPort=$udpPort, broadcastIntervalMillis=$broadcastIntervalMillis, staleDeviceTimeoutMillis=$staleDeviceTimeoutMillis, protocol=$protocol, keyword=<redacted>)"

    override fun toString(): String = toDisplaySummary()
}

data class DiscoveredDevice(
    val id: String,
    val displayName: String,
    val platformName: String,
    val host: String,
    val port: Int,
    val lastSeenEpochMillis: Long,
    val protocol: DiscoveryProtocol = DiscoveryProtocol.UdpLan,
)

interface DeviceDiscoveryService {
    val devices: StateFlow<List<DiscoveredDevice>>
    val isRunning: StateFlow<Boolean>
    val activeConfigSummary: StateFlow<String?>

    fun start(config: DiscoveryConfig)

    fun stop()
}

fun PlatformDeviceInfo.toLocalDiscoveryConfig(keyword: String): DiscoveryConfig =
    DiscoveryConfig(
        keyword = keyword,
        deviceAlias = displayName,
    )

expect fun createDeviceDiscoveryService(
    deviceInfo: PlatformDeviceInfo,
    networkPermissionGateway: NetworkPermissionGateway,
): DeviceDiscoveryService
