package com.nowaste.app.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nowaste.app.photos.createFoodPhotoUri
import com.nowaste.app.photos.deleteFoodPhotoUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchPhotoCaptureScreen(
    onNavigateBack: () -> Unit,
    onFinished: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var capturedPhotoUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraLaunchRequest by remember { mutableIntStateOf(0) }
    var keepCapturing by remember { mutableStateOf(false) }
    var showCameraUnavailableDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = pendingPhotoUri
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            capturedPhotoUris = capturedPhotoUris + uri.toString()
            pendingPhotoUri = null
            if (keepCapturing) {
                cameraLaunchRequest += 1
            }
        } else {
            uri?.let { deleteFoodPhotoUri(context, it) }
            pendingPhotoUri = null
            keepCapturing = false
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            cameraLaunchRequest += 1
        } else {
            keepCapturing = false
            showCameraPermissionDeniedDialog = true
        }
    }

    fun requestCameraSession() {
        keepCapturing = true
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            cameraLaunchRequest += 1
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(cameraLaunchRequest) {
        if (cameraLaunchRequest == 0) return@LaunchedEffect
        val uri = createFoodPhotoUri(context)
        pendingPhotoUri = uri
        try {
            cameraLauncher.launch(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
            )
        } catch (_: ActivityNotFoundException) {
            pendingPhotoUri = null
            keepCapturing = false
            deleteFoodPhotoUri(context, uri)
            showCameraUnavailableDialog = true
        } catch (_: SecurityException) {
            pendingPhotoUri = null
            keepCapturing = false
            deleteFoodPhotoUri(context, uri)
            showCameraPermissionDeniedDialog = true
        }
    }

    fun removeCapturedPhoto(photoUri: String) {
        capturedPhotoUris = capturedPhotoUris.filterNot { it == photoUri }
        deleteFoodPhotoUri(context, photoUri.toUri())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("连续拍照添加") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "拍下每件食品的包装或保质期位置，完成后统一生成待补全记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = ::requestCameraSession,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                )
                Text(
                    text = if (capturedPhotoUris.isEmpty()) "开始连续拍照" else "继续连续拍照",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Button(
                onClick = { onFinished(capturedPhotoUris) },
                enabled = capturedPhotoUris.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("完成添加 ${capturedPhotoUris.size} 件")
            }
            if (capturedPhotoUris.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有照片",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(capturedPhotoUris, key = { it }) { photoUri ->
                        PhotoCapturePreview(
                            photoUri = photoUri,
                            onDelete = { removeCapturedPhoto(photoUri) },
                        )
                    }
                }
            }
        }
    }

    if (showCameraUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showCameraUnavailableDialog = false },
            title = { Text("无法拍照") },
            text = { Text("当前设备没有可用的相机应用。") },
            confirmButton = {
                TextButton(onClick = { showCameraUnavailableDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }

    if (showCameraPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDeniedDialog = false },
            title = { Text("无法拍照") },
            text = { Text("需要相机权限才能连续拍摄食品照片。") },
            confirmButton = {
                TextButton(onClick = { showCameraPermissionDeniedDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun PhotoCapturePreview(
    photoUri: String,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(180.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box {
            SampledPhotoImage(
                photoUri = photoUri,
                contentDescription = "已拍摄食品照片",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f),
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除照片",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
