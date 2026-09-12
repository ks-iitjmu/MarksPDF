package com.kunalsharma.markspdf.core.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object DocumentSharing {

    fun shareIntent(context: Context, uri: Uri, title: String): Intent {
        val shareable = shareableUri(context, uri)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, shareable)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, title)
    }

    fun openWithIntent(context: Context, uri: Uri, title: String): Intent {
        val shareable = shareableUri(context, uri)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareable, MIME_PDF)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(view, title)
    }

    private fun shareableUri(context: Context, uri: Uri): Uri =
        if (uri.scheme == "file" && uri.path != null) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(uri.path!!))
        } else {
            uri
        }

    private const val MIME_PDF = "application/pdf"
}
