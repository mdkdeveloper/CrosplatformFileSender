package com.dimonoso.crosplatformfilesender.discovery

import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionGateway
import com.dimonoso.crosplatformfilesender.platform.PlatformDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.Collections

actual fun createDeviceDiscoveryService(
    deviceInfo: PlatformDeviceInfo,
    networkPermissionGateway: NetworkPermissionGateway,
): DeviceDiscoveryService =
    UdpLanDeviceDiscoveryService(
        deviceInfo = deviceInfo,
        networkPermissionGateway = networkPermissionGateway,
    )

internal actual fun createUdpDiscoveryTransport(port: Int): UdpDiscoveryTransport =
    JavaUdpDiscoveryTransport(port)

internal actual fun discoveryCurrentTimeMillis(): Long = System.currentTimeMillis()

private class JavaUdpDiscoveryTransport(
    private val port: Int,
) : UdpDiscoveryTransport {
    private val receiveSocket = DatagramSocket(null).apply {
        reuseAddress = true
        soTimeout = 1_000
        bind(InetSocketAddress(this@JavaUdpDiscoveryTransport.port))
    }
    private val sendSocket = DatagramSocket().apply {
        broadcast = true
    }

    override suspend fun broadcast(payload: ByteArray) {
        withContext(Dispatchers.IO) {
            broadcastTargets().forEach { target ->
                runCatching {
                    sendSocket.send(DatagramPacket(payload, payload.size, target))
                }
            }
        }
    }

    override suspend fun receive(maxPayloadBytes: Int): UdpDiscoveryPacket? =
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(maxPayloadBytes)
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                receiveSocket.receive(packet)
                UdpDiscoveryPacket(
                    payload = packet.data.copyOfRange(packet.offset, packet.offset + packet.length),
                    remoteHost = packet.address.hostAddress ?: packet.address.hostName,
                )
            } catch (_: SocketTimeoutException) {
                null
            } catch (exception: SocketException) {
                if (receiveSocket.isClosed) null else throw exception
            }
        }

    override fun close() {
        receiveSocket.close()
        sendSocket.close()
    }

    private fun broadcastTargets(): List<InetSocketAddress> {
        val targets = linkedSetOf(InetSocketAddress("255.255.255.255", port))
        val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }.getOrNull()
            ?: return targets.toList()

        interfaces
            .filter { networkInterface ->
                runCatching { networkInterface.isUp && !networkInterface.isLoopback }.getOrDefault(false)
            }
            .flatMap { networkInterface -> networkInterface.interfaceAddresses.orEmpty() }
            .mapNotNull { interfaceAddress -> interfaceAddress.broadcast }
            .forEach { broadcastAddress -> targets += InetSocketAddress(broadcastAddress, port) }

        return targets.toList()
    }
}
