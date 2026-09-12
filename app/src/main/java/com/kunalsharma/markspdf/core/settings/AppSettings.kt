package com.kunalsharma.markspdf.core.settings

import android.content.Context
import androidx.core.content.edit

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun languageTag(): String? = prefs.getString(KEY_LANGUAGE, null)

    fun setLanguageTag(tag: String?) {
        prefs.edit {
            if (tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, tag)
        }
    }

    fun hasSeenIntro(): Boolean = prefs.getBoolean(KEY_INTRO_SEEN, false)

    fun setIntroSeen() {
        prefs.edit { putBoolean(KEY_INTRO_SEEN, true) }
    }

    private companion object {
        const val PREFS = "markspdf_settings"
        const val KEY_LANGUAGE = "language_tag"
        const val KEY_INTRO_SEEN = "intro_seen"
    }
}
enum class AppLanguage(val tag: String, val label: String, val englishName: String) {
    English("en", "English", "English"),
    Hindi("hi", "हिन्दी", "Hindi"),
    Punjabi("pa", "ਪੰਜਾਬੀ", "Punjabi");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }
    }
}
