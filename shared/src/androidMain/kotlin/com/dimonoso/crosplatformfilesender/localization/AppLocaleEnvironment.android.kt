package com.dimonoso.crosplatformfilesender.localization

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    @Composable
    actual infix fun provides(localeTag: String): ProvidedValue<*> {
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)

        val configuration = Configuration(LocalConfiguration.current)
        configuration.setLocale(locale)
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)

        return LocalConfiguration.provides(configuration)
    }
}
