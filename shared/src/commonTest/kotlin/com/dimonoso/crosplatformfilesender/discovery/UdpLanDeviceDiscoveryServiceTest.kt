package com.dimonoso.crosplatformfilesender.discovery

import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionGateway
import com.dimonoso.crosplatformfilesender.platform.NetworkPermissionState
import com.dimonoso.crosplatformfilesender.platform.PlatformDeviceInfo
import com.dimonoso.crosplatformfilesender.platform.PlatformFamily
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UdpLanDeviceDiscoveryServiceTest {
    @Test
    fun startPublishesTransportFailureInsteadOfSilentlyStayingStopped() {
        val service = UdpLanDeviceDiscoveryService(
            deviceInfo = PlatformDeviceInfo(
                id = "desktop-test",
                displayName = "Test desktop",
                platformName = "Test OS",
                family = PlatformFamily.DesktopJvm,
            ),
            networkPermissionGateway = object : NetworkPermissionGateway {
                override fun currentState(): NetworkPermissionState =
                    NetworkPermissionState(
                        supportsUdpDiscovery = true,
                        requiresRuntimeApproval = false,
                        statusLabel = "Network available",
                    )
            },
            transportFactory = { error("socket unavailable") },
        )

        service.start(
            DiscoveryConfig(
                keyword = "test_key",
                deviceAlias = "Test desktop",
            ),
        )

        assertFalse(service.isRunning.value)
        assertNotNull(service.lastError.value)
        assertTrue(service.lastError.value.orEmpty().contains("socket unavailable"))
    }
}
