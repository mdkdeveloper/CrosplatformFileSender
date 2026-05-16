package com.dimonoso.crosplatformfilesender.settings

import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsConfigCodecTest {
    @Test
    fun discoveryKeywordAllowsOnlySupportedCharacters() {
        assertTrue(isValidDiscoveryKeyword("abcXYZ123_-.,\"&$"))
        assertFalse(isValidDiscoveryKeyword(""))
        assertFalse(isValidDiscoveryKeyword("bad password"))
        assertFalse(isValidDiscoveryKeyword("bad/keyword"))
    }

    @Test
    fun settingsRoundTripKeepsKeywordAndWhitelist() {
        val settings = AppSettings(
            localDeviceId = "device-test-id",
            discoveryKeyword = "key_123-&$",
            whitelistFolders = listOf(
                WhitelistFolder(
                    id = "folder-1",
                    displayName = "Downloads",
                    path = "C:\\Users\\dimon\\Downloads",
                    enabled = false,
                ),
            ),
        )

        val decoded = SettingsConfigCodec.decode(SettingsConfigCodec.encode(settings))

        assertEquals(settings, decoded)
    }

    @Test
    fun settingsDecodeCreatesLocalDeviceIdWhenMissing() {
        val decoded = SettingsConfigCodec.decode("discovery.keyword=local-secret")

        assertTrue(decoded.localDeviceId.isNotBlank())
    }
}
