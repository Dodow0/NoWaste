package com.nowaste.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

private const val FullscreenPhotoMaxSidePx = 2048
private const val PreviewPhotoMaxSidePx = 960

@Composable
fun SampledPhotoImage(
    photoUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxSidePx: Int = PreviewPhotoMaxSidePx,
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val errorPainter = rememberVectorPainter(Icons.Default.BrokenImage)

    Box(
        modifier = modifier.background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photoUri.takeIf { it.isNotBlank() },
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            placeholder = errorPainter,
            error = errorPainter,
            fallback = errorPainter,
        )
    }
}

@Composable
fun FullscreenSampledPhotoImage(
    photoUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    SampledPhotoImage(
        photoUri = photoUri,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        maxSidePx = FullscreenPhotoMaxSidePx,
    )
}
