package com.nowaste.app.photos

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val PhotoTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

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
