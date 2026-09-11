package com.example.lovepdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/**
 * PDFBox ships its font metrics and encoding tables as Android assets and won't load
 * them without being pointed at a context first. Missing this call doesn't fail at
 * startup — it fails later, deep inside a merge, with an obscure font error.
 */
class LovePdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
