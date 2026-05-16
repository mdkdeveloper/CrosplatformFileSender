package com.dimonoso.crosplatformfilesender.discovery

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscoveryConfigTest {
    @Test
    fun discoveryConfigDoesNotPublishKeywordInDisplayText() {
        val config = DiscoveryConfig(
            keyword = "super-secret-keyword",
            deviceAlias = "Laptop",
        )

        assertFalse(config.toString().contains("super-secret-keyword"))
        assertFalse(config.toDisplaySummary().contains("super-secret-keyword"))
        assertTrue(config.toDisplaySummary().contains("<redacted>"))
    }
}
