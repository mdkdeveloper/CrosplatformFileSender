package com.dimonoso.crosplatformfilesender.discovery

import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionGateway
import com.dimonoso.crosplatformfilesender.platform.PlatformDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StubDeviceDiscoveryService(
    private val deviceInfo: PlatformDeviceInfo,
    private val networkPermissionGateway: NetworkPermissionGateway,
    private val timeProvider: () -> Long = { 0L },
) : DeviceDiscoveryService {
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    private val _isRunning = MutableStateFlow(false)
    private val _activeConfigSummary = MutableStateFlow<String?>(null)
    private val _lastError = MutableStateFlow<DiscoveryError?>(null)

    override val devices: StateFlow<List<DiscoveredDevice>> = _devices
    override val isRunning: StateFlow<Boolean> = _isRunning
    override val activeConfigSummary: StateFlow<String?> = _activeConfigSummary
    override val lastError: StateFlow<DiscoveryError?> = _lastError

    override fun start(config: DiscoveryConfig) {
        val permissionState = networkPermissionGateway.currentState()

        _isRunning.value = permissionState.supportsUdpDiscovery
        _activeConfigSummary.value = config.toDisplaySummary()
        _lastError.value = if (permissionState.supportsUdpDiscovery) {
            null
        } else {
            DiscoveryError(
                type = DiscoveryErrorType.Unavailable,
                detail = permissionState.statusLabel,
            )
        }
        _devices.value = if (permissionState.supportsUdpDiscovery) {
            listOf(
                DiscoveredDevice(
                    id = "stub-remote-device",
                    displayName = "Demo peer for ${config.deviceAlias}",
                    platformName = deviceInfo.platformName,
                    host = "192.168.1.42",
                    port = config.udpPort,
                    lastSeenEpochMillis = timeProvider(),
                ),
            )
        } else {
            emptyList()
        }
    }

    override fun stop() {
        _isRunning.value = false
        _devices.value = emptyList()
        _activeConfigSummary.value = null
        _lastError.value = null
    }
}
