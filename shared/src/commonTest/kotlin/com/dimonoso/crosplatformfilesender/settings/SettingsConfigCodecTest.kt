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
            deviceDisplayName = "Living room PC",
            discoveryKeyword = "key_123-&$",
            languageMode = AppLanguageMode.Ukrainian,
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

    @Test
    fun settingsDecodeKeepsDeviceDisplayNameWhenPresent() {
        val decoded = SettingsConfigCodec.decode(
            """
            device.name=Desk PC
            discovery.keyword=local-secret
            """.trimIndent(),
        )

        assertEquals("Desk PC", decoded.deviceDisplayName)
    }

    @Test
    fun settingsDecodeUsesSystemLanguageModeWhenMissing() {
        val decoded = SettingsConfigCodec.decode("discovery.keyword=local-secret")

        assertEquals(AppLanguageMode.System, decoded.languageMode)
    }

    @Test
    fun settingsDecodeFallsBackToSystemLanguageModeWhenInvalid() {
        val decoded = SettingsConfigCodec.decode(
            """
            discovery.keyword=local-secret
            ui.language=fr
            """.trimIndent(),
        )

        assertEquals(AppLanguageMode.System, decoded.languageMode)
    }

    @Test
    fun settingsLanguageModeRoundTripKeepsSupportedModes() {
        listOf(
            AppLanguageMode.System,
            AppLanguageMode.English,
            AppLanguageMode.Ukrainian,
        ).forEach { mode ->
            val settings = AppSettings(
                localDeviceId = "device-language-test",
                discoveryKeyword = "local-secret",
                languageMode = mode,
            )

            val decoded = SettingsConfigCodec.decode(SettingsConfigCodec.encode(settings))

            assertEquals(mode, decoded.languageMode)
        }
    }

    @Test
    fun transferSettingsRoundTripKeepsConcurrencyAndBackupMode() {
        val settings = AppSettings(
            localDeviceId = "device-transfer-test",
            discoveryKeyword = "local-secret",
            maxOutgoingTransfers = 3,
            maxIncomingTransfers = 4,
            backupMode = BackupMode.BackupOnlyFile,
        )

        val decoded = SettingsConfigCodec.decode(SettingsConfigCodec.encode(settings))

        assertEquals(3, decoded.maxOutgoingTransfers)
        assertEquals(4, decoded.maxIncomingTransfers)
        assertEquals(BackupMode.BackupOnlyFile, decoded.backupMode)
    }

    @Test
    fun transferSettingsFallbackToSafeDefaultsWhenMissingOrInvalid() {
        val decoded = SettingsConfigCodec.decode(
            """
            discovery.keyword=local-secret
            transfer.maxOutgoing=0
            transfer.maxIncoming=100
            backup.mode=unknown
            """.trimIndent(),
        )

        assertEquals(1, decoded.maxOutgoingTransfers)
        assertEquals(16, decoded.maxIncomingTransfers)
        assertEquals(BackupMode.BackupFolder, decoded.backupMode)
    }

    @Test
    fun systemLanguageResolverUsesUkrainianOnlyForUkCodes() {
        assertEquals(AppLanguage.Ukrainian, languageFromSystemCode("uk"))
        assertEquals(AppLanguage.Ukrainian, languageFromSystemCode("uk-UA"))
        assertEquals(AppLanguage.English, languageFromSystemCode("en"))
        assertEquals(AppLanguage.English, languageFromSystemCode("de"))
        assertEquals(AppLanguage.English, languageFromSystemCode(""))
        assertEquals(AppLanguage.English, languageFromSystemCode(null))
    }

    @Test
    fun settingsServiceReadsExistingConfigOnStartup() {
        val folder = WhitelistFolder(
            id = "folder-2",
            displayName = "Projects",
            path = "D:\\Projects",
            enabled = false,
        )
        val store = InMemorySettingsStore(
            SettingsConfigCodec.encode(
                AppSettings(
                    localDeviceId = "device-from-config",
                    discoveryKeyword = "saved_key",
                    whitelistFolders = listOf(folder),
                ),
            ),
        )

        val service = PersistentSettingsService(store)

        assertEquals("device-from-config", service.settings.value.localDeviceId)
        assertEquals("saved_key", service.settings.value.discoveryKeyword)
        assertEquals(listOf(folder), service.settings.value.whitelistFolders)
    }

    @Test
    fun settingsServicePersistsWhitelistChanges() {
        val store = InMemorySettingsStore(null)
        val service = PersistentSettingsService(store)
        val folder = WhitelistFolder(
            id = "folder-3",
            displayName = "Media",
            path = "E:\\Media",
            enabled = true,
        )

        service.updateWhitelistFolders(listOf(folder.copy(enabled = false)))

        val decoded = SettingsConfigCodec.decode(store.contents.orEmpty())
        assertEquals(listOf(folder.copy(enabled = false)), decoded.whitelistFolders)
    }

    @Test
    fun settingsServicePersistsLanguageChanges() {
        val store = InMemorySettingsStore(null)
        val service = PersistentSettingsService(store)

        service.updateLanguageMode(AppLanguageMode.English)

        val decoded = SettingsConfigCodec.decode(store.contents.orEmpty())
        assertEquals(AppLanguageMode.English, decoded.languageMode)
    }

    @Test
    fun settingsServicePersistsDeviceDisplayNameChanges() {
        val store = InMemorySettingsStore(null)
        val service = PersistentSettingsService(store)

        assertTrue(service.updateDeviceDisplayName("Desk PC"))

        val decoded = SettingsConfigCodec.decode(store.contents.orEmpty())
        assertEquals("Desk PC", decoded.deviceDisplayName)
    }
}

private class InMemorySettingsStore(
    var contents: String?,
) : SettingsStore {
    override fun readConfig(): String? = contents

    override fun writeConfig(contents: String) {
        this.contents = contents
    }
}
