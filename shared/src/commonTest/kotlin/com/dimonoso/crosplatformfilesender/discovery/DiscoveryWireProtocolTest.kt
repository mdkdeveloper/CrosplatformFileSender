package com.dimonoso.crosplatformfilesender.discovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiscoveryWireProtocolTest {
    @Test
    fun keywordFingerprintIsStableAndDoesNotExposeKeyword() {
        val keyword = "super-secret-keyword"
        val first = discoveryKeywordFingerprint(keyword)
        val second = discoveryKeywordFingerprint(keyword)

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        assertFalse(first.contains(keyword))
        assertNotEquals(first, discoveryKeywordFingerprint("another-keyword"))
    }

    @Test
    fun keywordFingerprintUsesExpectedSha256Digest() {
        assertEquals(
            "7e7e25a6fa5d659e7312e29854c1185945f7a8d4be175380e920ec31d617410a",
            discoveryKeywordFingerprint("abc"),
        )
    }

    @Test
    fun discoveryAnnouncementRoundTripsThroughWireCodec() {
        val announcement = DiscoveryAnnouncement(
            deviceId = "desktop=user",
            displayName = "Office\nLaptop",
            platformName = "Windows 11",
            port = 47777,
            keywordFingerprint = discoveryKeywordFingerprint("local-secret"),
        )

        val decoded = DiscoveryMessageCodec.decode(DiscoveryMessageCodec.encode(announcement))

        assertEquals(announcement, decoded)
    }

    @Test
    fun discoveryCodecRejectsMalformedPackets() {
        assertNull(DiscoveryMessageCodec.decode("not-a-discovery-packet".encodeToByteArray()))
        assertNull(DiscoveryMessageCodec.decode("CFS_DISCOVERY_V1\ntype=announce\nport=0".encodeToByteArray()))
    }
}
