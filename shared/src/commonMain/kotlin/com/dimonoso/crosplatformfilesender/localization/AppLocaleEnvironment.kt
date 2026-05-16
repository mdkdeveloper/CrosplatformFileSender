package com.dimonoso.crosplatformfilesender.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

@Composable
fun AppLocaleEnvironment(
    localeTag: String,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppLocale provides localeTag) {
        key(localeTag) {
            content()
        }
    }
}

expect object LocalAppLocale {
    @Composable
    infix fun provides(localeTag: String): ProvidedValue<*>
}
