package com.dimonoso.crosplatformfilesender.settings

import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

const val DefaultDiscoveryKeyword = "local-secret"

private val AllowedDiscoveryKeywordSymbols = setOf('_', '-', '.', ',', '"', '&', '$')

data class AppSettings(
    val localDeviceId: String = createLocalDeviceId(),
    val discoveryKeyword: String = DefaultDiscoveryKeyword,
    val whitelistFolders: List<WhitelistFolder> = emptyList(),
    val languageMode: AppLanguageMode = AppLanguageMode.System,
)

interface SettingsService {
    val settings: StateFlow<AppSettings>

    fun updateDiscoveryKeyword(keyword: String): Boolean

    fun updateWhitelistFolders(folders: List<WhitelistFolder>)

    fun updateLanguageMode(mode: AppLanguageMode)
}

fun isDiscoveryKeywordChar(char: Char): Boolean =
    char.isLetterOrDigit() || char in AllowedDiscoveryKeywordSymbols

fun isValidDiscoveryKeyword(keyword: String): Boolean =
    keyword.isNotBlank() && keyword.all(::isDiscoveryKeywordChar)

fun createSettingsService(): SettingsService =
    PersistentSettingsService(createSettingsStore())

internal class PersistentSettingsService(
    private val store: SettingsStore,
) : SettingsService {
    private val _settings = MutableStateFlow(loadSettings())

    override val settings: StateFlow<AppSettings> = _settings

    init {
        persist(_settings.value)
    }

    override fun updateDiscoveryKeyword(keyword: String): Boolean {
        val normalized = keyword.trim()
        if (!isValidDiscoveryKeyword(normalized)) return false

        replace(_settings.value.copy(discoveryKeyword = normalized))
        return true
    }

    override fun updateWhitelistFolders(folders: List<WhitelistFolder>) {
        replace(_settings.value.copy(whitelistFolders = folders))
    }

    override fun updateLanguageMode(mode: AppLanguageMode) {
        replace(_settings.value.copy(languageMode = mode))
    }

    private fun loadSettings(): AppSettings {
        val stored = store.readConfig()
            ?.let(SettingsConfigCodec::decode)
            ?: AppSettings()

        return stored.copy(
            localDeviceId = stored.localDeviceId.takeIf { it.isNotBlank() } ?: createLocalDeviceId(),
            discoveryKeyword = stored.discoveryKeyword.takeIf(::isValidDiscoveryKeyword) ?: DefaultDiscoveryKeyword,
        )
    }

    private fun replace(settings: AppSettings) {
        _settings.value = settings
        persist(settings)
    }

    private fun persist(settings: AppSettings) {
        runCatching {
            store.writeConfig(SettingsConfigCodec.encode(settings))
        }
    }
}

internal interface SettingsStore {
    fun readConfig(): String?

    fun writeConfig(contents: String)
}

internal expect fun createSettingsStore(): SettingsStore

private fun createLocalDeviceId(): String =
    "device-${Random.nextLong()}-${Random.nextLong()}"
