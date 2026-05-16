package com.dimonoso.crosplatformfilesender.discovery

import kotlin.test.Test

class UdpDiscoveryTransportJvmTest {
    @Test
    fun transportBindsRequestedPortWithoutUsingUnconnectedSocketPort() {
        val transport = createUdpDiscoveryTransport(0)

        transport.close()
    }
}
