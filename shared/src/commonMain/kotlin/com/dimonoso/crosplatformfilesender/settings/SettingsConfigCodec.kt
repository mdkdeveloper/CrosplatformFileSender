package com.dimonoso.crosplatformfilesender.settings

import com.dimonoso.crosplatformfilesender.filesystem.RemoteDeletePolicy
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistDeletePolicyOverride
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder

internal object SettingsConfigCodec {
    private const val DeviceIdKey = "device.id"
    private const val DeviceNameKey = "device.name"
    private const val KeywordKey = "discovery.keyword"
    private const val AutoSearchDevicesOnStartupKey = "discovery.autoSearchOnStartup"
    private const val WhitelistCountKey = "whitelist.count"
    private const val LanguageKey = "ui.language"
    private const val MaxOutgoingTransfersKey = "transfer.maxOutgoing"
    private const val MaxIncomingTransfersKey = "transfer.maxIncoming"
    private const val BackupModeKey = "backup.mode"
    private const val RemoteDeletePolicyKey = "delete.remote"

    fun encode(settings: AppSettings): String =
        buildString {
            appendLine("# CrosplatformFileSender settings")
            appendLine("$DeviceIdKey=${settings.localDeviceId.escapeConfigValue()}")
            appendLine("$DeviceNameKey=${settings.deviceDisplayName.escapeConfigValue()}")
            appendLine("$KeywordKey=${settings.discoveryKeyword.escapeConfigValue()}")
            appendLine("$AutoSearchDevicesOnStartupKey=${settings.autoSearchDevicesOnStartup}")
            appendLine("$LanguageKey=${settings.languageMode.configValue}")
            appendLine("$MaxOutgoingTransfersKey=${settings.maxOutgoingTransfers.normalizeTransferLimit()}")
            appendLine("$MaxIncomingTransfersKey=${settings.maxIncomingTransfers.normalizeTransferLimit()}")
            appendLine("$BackupModeKey=${settings.backupMode.configValue}")
            appendLine("$RemoteDeletePolicyKey=${settings.remoteDeletePolicy.configValue}")
            appendLine("$WhitelistCountKey=${settings.whitelistFolders.size}")
            settings.whitelistFolders.forEachIndexed { index, folder ->
                appendLine("whitelist.$index.id=${folder.id.escapeConfigValue()}")
                appendLine("whitelist.$index.displayName=${folder.displayName.escapeConfigValue()}")
                appendLine("whitelist.$index.path=${folder.path.escapeConfigValue()}")
                appendLine("whitelist.$index.enabled=${folder.enabled}")
                appendLine("whitelist.$index.deletePolicy=${folder.deletePolicyOverride.configValue}")
            }
        }

    fun decode(contents: String): AppSettings {
        val properties = contents
            .lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null

                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).unescapeConfigValue() ?: return@mapNotNull null
                key to value
            }
            .toMap()

        val keyword = properties[KeywordKey]
            ?.takeIf(::isValidDiscoveryKeyword)
            ?: DefaultDiscoveryKeyword
        val autoSearchDevicesOnStartup = properties[AutoSearchDevicesOnStartupKey]?.toBooleanStrictOrNull() ?: false
        val localDeviceId = properties[DeviceIdKey]?.takeIf { it.isNotBlank() } ?: AppSettings().localDeviceId
        val deviceDisplayName = properties[DeviceNameKey]
            ?.trim()
            ?.takeIf(::isValidDeviceDisplayName)
            .orEmpty()
        val languageMode = AppLanguageMode.fromConfigValue(properties[LanguageKey])
        val maxOutgoingTransfers = properties[MaxOutgoingTransfersKey]
            ?.toIntOrNull()
            ?.normalizeTransferLimit()
            ?: DefaultMaxParallelTransfers
        val maxIncomingTransfers = properties[MaxIncomingTransfersKey]
            ?.toIntOrNull()
            ?.normalizeTransferLimit()
            ?: DefaultMaxParallelTransfers
        val backupMode = BackupMode.fromConfigValue(properties[BackupModeKey])
        val remoteDeletePolicy = RemoteDeletePolicy.fromConfigValue(properties[RemoteDeletePolicyKey])
        val whitelistCount = properties[WhitelistCountKey]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val folders = (0 until whitelistCount).mapNotNull { index ->
            val prefix = "whitelist.$index"
            val path = properties["$prefix.path"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            WhitelistFolder(
                id = properties["$prefix.id"]?.takeIf { it.isNotBlank() } ?: "whitelist-${path.hashCode()}",
                displayName = properties["$prefix.displayName"]?.takeIf { it.isNotBlank() } ?: path,
                path = path,
                enabled = properties["$prefix.enabled"]?.toBooleanStrictOrNull() ?: true,
                deletePolicyOverride = WhitelistDeletePolicyOverride.fromConfigValue(properties["$prefix.deletePolicy"]),
            )
        }

        return AppSettings(
            localDeviceId = localDeviceId,
            deviceDisplayName = deviceDisplayName,
            discoveryKeyword = keyword,
            autoSearchDevicesOnStartup = autoSearchDevicesOnStartup,
            whitelistFolders = folders,
            languageMode = languageMode,
            maxOutgoingTransfers = maxOutgoingTransfers,
            maxIncomingTransfers = maxIncomingTransfers,
            backupMode = backupMode,
            remoteDeletePolicy = remoteDeletePolicy,
        )
    }
}

private fun String.escapeConfigValue(): String =
    buildString(length) {
        for (char in this@escapeConfigValue) {
            when (char) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                else -> append(char)
            }
        }
    }

private fun String.unescapeConfigValue(): String? =
    buildString(length) {
        var index = 0
        while (index < this@unescapeConfigValue.length) {
            val char = this@unescapeConfigValue[index]
            if (char != '%') {
                append(char)
                index += 1
                continue
            }

            if (index + 2 >= this@unescapeConfigValue.length) return null
            val hex = this@unescapeConfigValue.substring(index + 1, index + 3)
            val code = hex.toIntOrNull(16) ?: return null
            append(code.toChar())
            index += 3
        }
    }
