package com.kunalsharma.markspdf

import android.app.Application
import android.content.Context
import com.kunalsharma.markspdf.core.settings.AppSettings
import com.kunalsharma.markspdf.core.settings.withLocale
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MarksPDFApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withLocale(AppSettings(base).languageTag()))
    }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
