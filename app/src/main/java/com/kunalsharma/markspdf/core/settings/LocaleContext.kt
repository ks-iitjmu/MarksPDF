package com.kunalsharma.markspdf.core.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.withLocale(tag: String?): Context {
    if (tag.isNullOrBlank()) return this

    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)

    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(configuration)
}
