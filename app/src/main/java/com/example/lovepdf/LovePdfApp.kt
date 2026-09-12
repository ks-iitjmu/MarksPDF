package com.example.lovepdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class LovePdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
