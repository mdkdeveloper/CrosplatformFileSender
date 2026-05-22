package com.dimonoso.crosplatformfilesender.settings

import com.dimonoso.crosplatformfilesender.filesystem.RemoteDeletePolicy
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

const val DefaultDiscoveryKeyword = "local-secret"
const val DefaultMaxParallelTransfers = 1

private val AllowedDiscoveryKeywordSymbols = setOf('_', '-', '.', ',', '"', '&', '$')

enum class BackupMode(
    val configValue: String,
) {
    DoNotBackup("none"),
    BackupOnlyFile("file"),
    BackupFolder("folder");

    companion object {
        fun fromConfigValue(value: String?): BackupMode =
            entries.firstOrNull { it.configValue == value } ?: BackupFolder
    }
}

data class AppSettings(
    val localDeviceId: String = createLocalDeviceId(),
    val deviceDisplayName: String = "",
    val discoveryKeyword: String = DefaultDiscoveryKeyword,
    val autoSearchDevicesOnStartup: Boolean = false,
    val whitelistFolders: List<WhitelistFolder> = emptyList(),
    val languageMode: AppLanguageMode = AppLanguageMode.System,
    val maxOutgoingTransfers: Int = DefaultMaxParallelTransfers,
    val maxIncomingTransfers: Int = DefaultMaxParallelTransfers,
    val backupMode: BackupMode = BackupMode.BackupFolder,
    val remoteDeletePolicy: RemoteDeletePolicy = RemoteDeletePolicy.DoNothing,
)

interface SettingsService {
    val settings: StateFlow<AppSettings>

    fun updateDeviceDisplayName(name: String): Boolean

    fun updateDiscoveryKeyword(keyword: String): Boolean

    fun updateAutoSearchDevicesOnStartup(enabled: Boolean)

    fun updateWhitelistFolders(folders: List<WhitelistFolder>)

    fun updateLanguageMode(mode: AppLanguageMode)

    fun updateMaxOutgoingTransfers(value: Int)

    fun updateMaxIncomingTransfers(value: Int)

    fun updateBackupMode(mode: BackupMode)

    fun updateRemoteDeletePolicy(policy: RemoteDeletePolicy)
}

fun isDiscoveryKeywordChar(char: Char): Boolean =
    char.isLetterOrDigit() || char in AllowedDiscoveryKeywordSymbols

fun isValidDiscoveryKeyword(keyword: String): Boolean =
    keyword.isNotBlank() && keyword.all(::isDiscoveryKeywordChar)

fun isValidDeviceDisplayName(name: String): Boolean =
    name.isNotBlank()

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

    override fun updateDeviceDisplayName(name: String): Boolean {
        val normalized = name.trim()
        if (!isValidDeviceDisplayName(normalized)) return false

        replace(_settings.value.copy(deviceDisplayName = normalized))
        return true
    }

    override fun updateDiscoveryKeyword(keyword: String): Boolean {
        val normalized = keyword.trim()
        if (!isValidDiscoveryKeyword(normalized)) return false

        replace(_settings.value.copy(discoveryKeyword = normalized))
        return true
    }

    override fun updateAutoSearchDevicesOnStartup(enabled: Boolean) {
        replace(_settings.value.copy(autoSearchDevicesOnStartup = enabled))
    }

    override fun updateWhitelistFolders(folders: List<WhitelistFolder>) {
        replace(_settings.value.copy(whitelistFolders = folders))
    }

    override fun updateLanguageMode(mode: AppLanguageMode) {
        replace(_settings.value.copy(languageMode = mode))
    }

    override fun updateMaxOutgoingTransfers(value: Int) {
        replace(_settings.value.copy(maxOutgoingTransfers = value.normalizeTransferLimit()))
    }

    override fun updateMaxIncomingTransfers(value: Int) {
        replace(_settings.value.copy(maxIncomingTransfers = value.normalizeTransferLimit()))
    }

    override fun updateBackupMode(mode: BackupMode) {
        replace(_settings.value.copy(backupMode = mode))
    }

    override fun updateRemoteDeletePolicy(policy: RemoteDeletePolicy) {
        replace(_settings.value.copy(remoteDeletePolicy = policy))
    }

    private fun loadSettings(): AppSettings {
        val stored = store.readConfig()
            ?.let(SettingsConfigCodec::decode)
            ?: AppSettings()

        return stored.copy(
            localDeviceId = stored.localDeviceId.takeIf { it.isNotBlank() } ?: createLocalDeviceId(),
            deviceDisplayName = stored.deviceDisplayName.trim().takeIf(::isValidDeviceDisplayName).orEmpty(),
            discoveryKeyword = stored.discoveryKeyword.takeIf(::isValidDiscoveryKeyword) ?: DefaultDiscoveryKeyword,
            maxOutgoingTransfers = stored.maxOutgoingTransfers.normalizeTransferLimit(),
            maxIncomingTransfers = stored.maxIncomingTransfers.normalizeTransferLimit(),
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

internal fun Int.normalizeTransferLimit(): Int = coerceIn(1, 16)

internal interface SettingsStore {
    fun readConfig(): String?

    fun writeConfig(contents: String)
}

internal expect fun createSettingsStore(): SettingsStore

private fun createLocalDeviceId(): String =
    "device-${Random.nextLong()}-${Random.nextLong()}"
