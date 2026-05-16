package com.dimonoso.crosplatformfilesender.discovery

internal const val DiscoveryMaxPacketBytes = 4096

internal data class UdpDiscoveryPacket(
    val payload: ByteArray,
    val remoteHost: String,
)

internal interface UdpDiscoveryTransport {
    suspend fun broadcast(payload: ByteArray)

    suspend fun receive(maxPayloadBytes: Int): UdpDiscoveryPacket?

    fun close()
}

internal expect fun createUdpDiscoveryTransport(port: Int): UdpDiscoveryTransport

internal expect fun discoveryCurrentTimeMillis(): Long
