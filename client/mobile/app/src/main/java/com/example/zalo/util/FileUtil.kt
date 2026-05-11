package com.example.zalo.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtil {
    fun fromUri(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "upload_file_" + System.currentTimeMillis())
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
