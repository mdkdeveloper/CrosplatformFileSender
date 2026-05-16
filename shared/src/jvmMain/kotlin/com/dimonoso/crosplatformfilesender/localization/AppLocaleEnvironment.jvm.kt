package com.dimonoso.crosplatformfilesender.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

actual object LocalAppLocale {
    private val localeHolder = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    @Composable
    actual infix fun provides(localeTag: String): ProvidedValue<*> {
        Locale.setDefault(Locale.forLanguageTag(localeTag))
        return localeHolder.provides(localeTag)
    }
}
