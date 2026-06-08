package com.nowaste.app.photos

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val PhotoTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")

fun createFoodPhotoUri(context: Context): Uri {
    val photoDirectory = foodPhotoDirectory(context).apply {
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

fun isFoodPhotoUri(context: Context, uri: Uri): Boolean =
    resolveFoodPhotoFile(context, uri) != null

fun deleteFoodPhotoUri(context: Context, uri: Uri): Boolean {
    val photoFile = resolveFoodPhotoFile(context, uri) ?: return false
    val deletedByProvider = if (uri.scheme == "content") runCatching {
        context.contentResolver.delete(uri, null, null) > 0
    }.getOrDefault(false) else false
    if (deletedByProvider) return true

    return runCatching { photoFile.delete() }.getOrDefault(false)
}

private fun foodPhotoDirectory(context: Context): File =
    File(context.filesDir, "photos")

private fun resolveFoodPhotoFile(context: Context, uri: Uri): File? {
    val photoDirectory = foodPhotoDirectory(context).canonicalFile
    val candidate = when (uri.scheme) {
        "file" -> uri.path?.let(::File)
        "content" -> {
            if (uri.authority != "${context.packageName}.fileprovider") return null
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: return null
            File(photoDirectory, fileName)
        }
        else -> null
    }?.canonicalFile ?: return null

    return candidate.takeIf {
        it.name.startsWith("food_") &&
            it.extension.equals("jpg", ignoreCase = true) &&
            it.path.startsWith(photoDirectory.path + File.separator)
    }
}
