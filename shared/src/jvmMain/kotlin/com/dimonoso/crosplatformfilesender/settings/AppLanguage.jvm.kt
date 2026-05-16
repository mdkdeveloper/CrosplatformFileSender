package com.dimonoso.crosplatformfilesender.settings

import java.util.Locale

actual fun getSystemLanguageCode(): String = Locale.getDefault().language
