package com.dimonoso.crosplatformfilesender.discovery

import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionGateway
import com.dimonoso.crosplatformfilesender.platform.PlatformDeviceInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal class UdpLanDeviceDiscoveryService(
    private val deviceInfo: PlatformDeviceInfo,
    private val networkPermissionGateway: NetworkPermissionGateway,
    private val transportFactory: (Int) -> UdpDiscoveryTransport = ::createUdpDiscoveryTransport,
    private val timeProvider: () -> Long = ::discoveryCurrentTimeMillis,
) : DeviceDiscoveryService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    private val _isRunning = MutableStateFlow(false)
    private val _activeConfigSummary = MutableStateFlow<String?>(null)

    private var activeJob: Job? = null
    private var activeTransport: UdpDiscoveryTransport? = null

    override val devices: StateFlow<List<DiscoveredDevice>> = _devices
    override val isRunning: StateFlow<Boolean> = _isRunning
    override val activeConfigSummary: StateFlow<String?> = _activeConfigSummary

    override fun start(config: DiscoveryConfig) {
        stop()
        _activeConfigSummary.value = config.toDisplaySummary()

        val permissionState = networkPermissionGateway.currentState()
        if (!permissionState.supportsUdpDiscovery) {
            _isRunning.value = false
            return
        }

        val transport = runCatching { transportFactory(config.udpPort) }.getOrElse {
            _isRunning.value = false
            return
        }

        val keywordFingerprint = discoveryKeywordFingerprint(config.keyword)
        val announcement = DiscoveryAnnouncement(
            deviceId = deviceInfo.id,
            displayName = config.deviceAlias,
            platformName = deviceInfo.platformName,
            port = config.udpPort,
            keywordFingerprint = keywordFingerprint,
        )
        val payload = DiscoveryMessageCodec.encode(announcement)

        activeTransport = transport
        _isRunning.value = true

        val job = scope.launch {
            val broadcaster = launch {
                while (isActive) {
                    broadcastOnce(transport, payload)
                    pruneStaleDevices(config)
                    delay(config.broadcastIntervalMillis)
                }
            }
            val receiver = launch {
                while (isActive) {
                    val packet = receiveOnce(transport)
                    if (packet == null) {
                        pruneStaleDevices(config)
                    } else {
                        rememberPacket(packet, config, keywordFingerprint)
                    }
                }
            }

            try {
                joinAll(broadcaster, receiver)
            } finally {
                broadcaster.cancelAndJoin()
                receiver.cancelAndJoin()
                transport.close()
            }
        }

        activeJob = job
        job.invokeOnCompletion {
            if (activeJob === job) {
                activeTransport = null
                _isRunning.value = false
            }
        }
    }

    override fun stop() {
        val job = activeJob
        val transport = activeTransport
        activeJob = null
        activeTransport = null
        job?.cancel()
        transport?.close()
        _isRunning.value = false
        _devices.value = emptyList()
        _activeConfigSummary.value = null
    }

    private suspend fun broadcastOnce(transport: UdpDiscoveryTransport, payload: ByteArray) {
        try {
            transport.broadcast(payload)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            delay(250L)
        }
    }

    private suspend fun receiveOnce(transport: UdpDiscoveryTransport): UdpDiscoveryPacket? =
        try {
            transport.receive(DiscoveryMaxPacketBytes)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            delay(250L)
            null
        }

    private fun rememberPacket(
        packet: UdpDiscoveryPacket,
        config: DiscoveryConfig,
        keywordFingerprint: String,
    ) {
        val announcement = DiscoveryMessageCodec.decode(packet.payload) ?: return
        if (announcement.keywordFingerprint != keywordFingerprint) return
        if (announcement.deviceId == deviceInfo.id) return

        val now = timeProvider()
        val device = DiscoveredDevice(
            id = announcement.deviceId,
            displayName = announcement.displayName,
            platformName = announcement.platformName,
            host = packet.remoteHost,
            port = announcement.port,
            lastSeenEpochMillis = now,
            protocol = config.protocol,
        )

        _devices.value = (_devices.value.filterNot { it.id == device.id } + device)
            .filterNotStale(now, config.staleDeviceTimeoutMillis)
            .sortedWith(compareBy<DiscoveredDevice> { it.displayName.lowercase() }.thenBy { it.host })
    }

    private fun pruneStaleDevices(config: DiscoveryConfig) {
        val now = timeProvider()
        _devices.value = _devices.value.filterNotStale(now, config.staleDeviceTimeoutMillis)
    }

    private fun List<DiscoveredDevice>.filterNotStale(
        nowEpochMillis: Long,
        staleDeviceTimeoutMillis: Long,
    ): List<DiscoveredDevice> =
        filter { device -> nowEpochMillis - device.lastSeenEpochMillis <= staleDeviceTimeoutMillis }
}
