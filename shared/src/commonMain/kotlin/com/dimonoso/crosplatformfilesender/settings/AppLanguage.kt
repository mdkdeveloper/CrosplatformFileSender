package com.dimonoso.crosplatformfilesender.settings

enum class AppLanguageMode(val configValue: String) {
    System("system"),
    English("en"),
    Ukrainian("uk"),
    ;

    companion object {
        fun fromConfigValue(value: String?): AppLanguageMode =
            entries.firstOrNull { it.configValue == value } ?: System
    }
}

enum class AppLanguage(val localeTag: String) {
    English("en"),
    Ukrainian("uk"),
}

fun resolveAppLanguage(
    mode: AppLanguageMode,
    systemLanguageCode: String?,
): AppLanguage =
    when (mode) {
        AppLanguageMode.System -> languageFromSystemCode(systemLanguageCode)
        AppLanguageMode.English -> AppLanguage.English
        AppLanguageMode.Ukrainian -> AppLanguage.Ukrainian
    }

fun languageFromSystemCode(systemLanguageCode: String?): AppLanguage =
    if (systemLanguageCode.orEmpty().lowercase().startsWith("uk")) {
        AppLanguage.Ukrainian
    } else {
        AppLanguage.English
    }

expect fun getSystemLanguageCode(): String
