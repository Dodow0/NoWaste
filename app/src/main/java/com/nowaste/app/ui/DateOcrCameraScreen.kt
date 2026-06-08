package com.nowaste.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.Text as VisionText
import com.nowaste.app.domain.extractExpiryDateFromText
import com.nowaste.app.domain.extractProductionDateFromText
import com.nowaste.app.domain.extractShelfLifeDurationFromText
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.roundToInt

enum class DateOcrField(
    val routeValue: String,
    val label: String,
    val savedStateKey: String,
) {
    ProductionDate(
        routeValue = "production-date",
        label = "生产日期",
        savedStateKey = "selectedProductionDate",
    ),
    ShelfLife(
        routeValue = "shelf-life",
        label = "保质期",
        savedStateKey = "selectedShelfLife",
    ),
    ExpiryDate(
        routeValue = "expiry-date",
        label = "到期日",
        savedStateKey = "selectedExpiryDate",
    );

    companion object {
        fun fromRoute(value: String?): DateOcrField? =
            values().firstOrNull { it.routeValue == value || it.name == value }
    }
}

private val DateOcrFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private const val StableFrameThreshold = 3
private val DateOcrHighlightColor = Color(0xFF30D158)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOcrCameraScreen(
    field: DateOcrField,
    onNavigateBack: () -> Unit,
    onValueSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var candidate by remember { mutableStateOf<DateOcrCandidate?>(null) }
    var lastCandidateValue by remember { mutableStateOf<String?>(null) }
    var stableHits by remember { mutableIntStateOf(0) }
    var hasCompleted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun confirmCandidate(value: String) {
        if (hasCompleted) return
        hasCompleted = true
        vibrateDateCaptured(context)
        onValueSelected(value)
    }

    LaunchedEffect(candidate?.value, stableHits) {
        val detected = candidate ?: return@LaunchedEffect
        if (stableHits >= StableFrameThreshold) {
            confirmCandidate(detected.value)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("识别${field.label}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
            ) {
                DateOcrCameraPreview(
                    field = field,
                    onCandidateDetected = { detected ->
                        if (hasCompleted) return@DateOcrCameraPreview
                        if (detected == null) {
                            candidate = null
                            lastCandidateValue = null
                            stableHits = 0
                        } else {
                            candidate = detected
                            if (detected.value == lastCandidateValue) {
                                stableHits += 1
                            } else {
                                lastCandidateValue = detected.value
                                stableHits = 1
                            }
                        }
                    },
                )
                DateOcrOverlay(
                    candidate = candidate,
                    onConfirm = {
                        candidate?.let { confirmCandidate(it.value) }
                    },
                )
                DateOcrBottomPanel(
                    field = field,
                    candidate = candidate,
                    stableHits = stableHits,
                    onConfirm = {
                        candidate?.let { confirmCandidate(it.value) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        } else {
            DateOcrPermissionContent(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DateOcrCameraPreview(
    field: DateOcrField,
    onCandidateDetected: (DateOcrCandidate?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val latestOnCandidateDetected by rememberUpdatedState(onCandidateDetected)
    val analyzer = remember(field, mainExecutor) {
        DateOcrAnalyzer(
            field = field,
            mainExecutor = mainExecutor,
            onCandidateDetected = { latestOnCandidateDetected(it) },
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView },
    )

    DisposableEffect(lifecycleOwner, previewView, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, analyzer) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            analyzer.close()
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun DateOcrOverlay(
    candidate: DateOcrCandidate?,
    onConfirm: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val highlight = candidate?.toHighlightRect(maxWidthPx, maxHeightPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            highlight?.let { rect ->
                drawRoundRect(
                    color = DateOcrHighlightColor.copy(alpha = 0.18f),
                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )
                drawRoundRect(
                    color = DateOcrHighlightColor,
                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        if (highlight != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = highlight.left.roundToInt(),
                            y = highlight.top.roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { highlight.width.toDp() },
                        height = with(density) { highlight.height.toDp() },
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onConfirm),
            )
        }
    }
}

@Composable
private fun DateOcrBottomPanel(
    field: DateOcrField,
    candidate: DateOcrCandidate?,
    stableHits: Int,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "对准包装上的${field.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = candidate?.let { "已识别：${it.value}" } ?: "识别会实时进行，无需拍照。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = if (candidate == null) {
                    "找到有效内容后会出现绿色框；连续稳定识别会自动填入。"
                } else {
                    "稳定帧数 $stableHits/$StableFrameThreshold，点击绿色框或按钮可立即填入。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onConfirm,
                enabled = candidate != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
                Text(
                    text = "填入${field.label}",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DateOcrPermissionContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "开启权限后即可实时识别包装上的日期文字。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRequestPermission) {
                Text("开启相机权限")
            }
        }
    }
}

private data class DateOcrCandidate(
    val value: String,
    val boundingBox: Rect,
    val imageWidth: Int,
    val imageHeight: Int,
)

private data class HighlightRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private fun DateOcrCandidate.toHighlightRect(
    containerWidth: Float,
    containerHeight: Float,
): HighlightRect {
    val scale = min(
        containerWidth / imageWidth.coerceAtLeast(1),
        containerHeight / imageHeight.coerceAtLeast(1),
    )
    val drawnWidth = imageWidth * scale
    val drawnHeight = imageHeight * scale
    val offsetX = (containerWidth - drawnWidth) / 2f
    val offsetY = (containerHeight - drawnHeight) / 2f
    val left = (offsetX + boundingBox.left * scale).coerceIn(0f, containerWidth)
    val top = (offsetY + boundingBox.top * scale).coerceIn(0f, containerHeight)
    val right = (offsetX + boundingBox.right * scale).coerceIn(left + 1f, containerWidth)
    val bottom = (offsetY + boundingBox.bottom * scale).coerceIn(top + 1f, containerHeight)
    return HighlightRect(
        left = left,
        top = top,
        width = right - left,
        height = bottom - top,
    )
}

private class DateOcrAnalyzer(
    private val field: DateOcrField,
    private val mainExecutor: Executor,
    private val onCandidateDetected: (DateOcrCandidate?) -> Unit,
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private val isProcessing = AtomicBoolean(false)

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val imageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.height
        } else {
            imageProxy.width
        }
        val imageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) {
            imageProxy.width
        } else {
            imageProxy.height
        }

        recognizer.process(image)
            .addOnSuccessListener(mainExecutor) { visionText ->
                onCandidateDetected(
                    findDateOcrCandidate(
                    visionText = visionText,
                    field = field,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    ),
                )
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    fun close() {
        recognizer.close()
    }
}

private fun findDateOcrCandidate(
    visionText: VisionText,
    field: DateOcrField,
    imageWidth: Int,
    imageHeight: Int,
): DateOcrCandidate? {
    val value = field.extractValue(visionText.text) ?: return null
    val block = visionText.textBlocks.firstOrNull { field.extractValue(it.text) == value }
        ?: visionText.textBlocks.firstOrNull()
        ?: return null
    val line = block.lines.firstOrNull { field.extractValue(it.text) == value }
    val box = line?.boundingBox ?: block.boundingBox ?: return null

    return DateOcrCandidate(
        value = value,
        boundingBox = box,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
    )
}

private fun DateOcrField.extractValue(text: String): String? =
    when (this) {
        DateOcrField.ProductionDate -> extractProductionDateFromText(text)?.format(DateOcrFormatter)
        DateOcrField.ShelfLife -> extractShelfLifeDurationFromText(text)?.toDisplayText()
        DateOcrField.ExpiryDate -> extractExpiryDateFromText(text)?.format(DateOcrFormatter)
    }

private fun vibrateDateCaptured(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return

    runCatching {
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                70L,
                VibrationEffect.DEFAULT_AMPLITUDE,
            ),
        )
    }
}
