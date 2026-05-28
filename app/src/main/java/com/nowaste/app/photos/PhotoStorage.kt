package com.nowaste.app.photos

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val PhotoTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")

fun createFoodPhotoUri(context: Context): Uri {
    val photoDirectory = File(context.filesDir, "photos").apply {
        mkdirs()
    }
    val timestamp = LocalDateTime.now().format(PhotoTimestampFormatter)
    val photoFile = File(photoDirectory, "food_$timestamp.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile,
    )
}

fun deleteFoodPhotoUri(context: Context, uri: Uri): Boolean {
    val deletedByProvider = runCatching {
        context.contentResolver.delete(uri, null, null) > 0
    }.getOrDefault(false)
    if (deletedByProvider) return true

    return runCatching {
        when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.delete() == true
            "content" -> {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: return@runCatching false
                File(File(context.filesDir, "photos"), fileName).delete()
            }
            else -> false
        }
    }.getOrDefault(false)
}
