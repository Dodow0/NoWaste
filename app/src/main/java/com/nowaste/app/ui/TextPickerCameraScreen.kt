package com.nowaste.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Size
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class RecognizedTextBlock(
    val text: String,
    val boundingBox: Rect,
    val priorityScore: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPickerCameraScreen(
    onNavigateBack: () -> Unit,
    onTextSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("点选商品名") },
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
        if (hasCameraPermission) {
            TextPickerCameraPreview(
                onTextSelected = onTextSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("需要相机权限才能识别包装文字")
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("授权相机")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextPickerCameraPreview(
    onTextSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val imageSize = remember { AtomicReference<Size?>(null) }
    val isProcessing = remember { AtomicBoolean(false) }
    val cameraProviderReference = remember { AtomicReference<ProcessCameraProvider?>(null) }
    var textBlocks by remember { mutableStateOf<List<RecognizedTextBlock>>(emptyList()) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var isTextLocked by remember { mutableStateOf(false) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        var disposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                if (disposed) {
                    cameraProvider.unbindAll()
                    return@addListener
                }
                cameraProviderReference.set(cameraProvider)
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(
                            executor,
                            TextBlockAnalyzer(
                                recognizer = recognizer,
                                mainExecutor = mainExecutor,
                                imageSize = imageSize,
                                isProcessing = isProcessing,
                                onTextBlocks = { blocks ->
                                    if (!isTextLocked) {
                                        textBlocks = blocks
                                        if (blocks.isNotEmpty()) {
                                            isTextLocked = true
                                        }
                                    }
                                },
                            ),
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            cameraProviderReference.get()?.unbindAll()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView },
        )
        TextBlockOverlay(
            blocks = textBlocks,
            imageSize = imageSize.get(),
            onClick = { position ->
                val selected = textBlocks.filter { block ->
                    mapBoundingBox(block.boundingBox, imageSize.get(), overlaySize)?.contains(position) == true
                }.maxByOrNull { it.priorityScore }
                selected?.text?.trim()?.takeIf { it.isNotBlank() }?.let(onTextSelected)
            },
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { overlaySize = it },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (isTextLocked) "文字框已锁定，大字候选会优先选中" else "正在识别包装文字...",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (isTextLocked) {
                    Button(
                        onClick = {
                            textBlocks = emptyList()
                            isTextLocked = false
                        },
                    ) {
                        Text("重新识别")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextBlockOverlay(
    blocks: List<RecognizedTextBlock>,
    imageSize: Size?,
    onClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.08f))
            .pointerInput(blocks, imageSize) {
                detectTapGestures { offset ->
                    onClick(offset)
                }
            },
    ) {
        val overlayIntSize = IntSize(size.width.toInt(), size.height.toInt())
        blocks.forEach { block ->
            val mapped = mapBoundingBox(block.boundingBox, imageSize, overlayIntSize) ?: return@forEach
            val isPrimaryCandidate = block == blocks.firstOrNull()
            val highlightColor = if (isPrimaryCandidate) Color(0xFFFFC857) else Color(0xFF00C2A8)
            drawRoundRect(
                color = highlightColor.copy(alpha = if (isPrimaryCandidate) 0.32f else 0.20f),
                topLeft = mapped.topLeft,
                size = ComposeSize(mapped.width, mapped.height),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
            drawRoundRect(
                color = highlightColor,
                topLeft = mapped.topLeft,
                size = ComposeSize(mapped.width, mapped.height),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = if (isPrimaryCandidate) 3.dp.toPx() else 2.dp.toPx()),
            )
        }
    }
}

private class TextBlockAnalyzer(
    private val recognizer: TextRecognizer,
    private val mainExecutor: Executor,
    private val imageSize: AtomicReference<Size?>,
    private val isProcessing: AtomicBoolean,
    private val onTextBlocks: (List<RecognizedTextBlock>) -> Unit,
) : ImageAnalysis.Analyzer {
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val isRotated = rotation == 90 || rotation == 270
        imageSize.set(
            Size(
                if (isRotated) imageProxy.height else imageProxy.width,
                if (isRotated) imageProxy.width else imageProxy.height,
            ),
        )
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.flatMap { block ->
                    val box = block.boundingBox ?: return@flatMap emptyList()
                    val lines = block.lines.mapNotNull { line ->
                        val lineBox = line.boundingBox ?: return@mapNotNull null
                        line.text.trim().takeIf { it.isNotBlank() }?.let { text ->
                            RecognizedTextBlock(
                                text = text,
                                boundingBox = lineBox,
                                priorityScore = productNamePriorityScore(text, lineBox),
                            )
                        }
                    }
                    if (lines.isNotEmpty()) {
                        lines
                    } else {
                        block.text.trim().takeIf { it.isNotBlank() }?.let { text ->
                            listOf(
                                RecognizedTextBlock(
                                    text = text,
                                    boundingBox = box,
                                    priorityScore = productNamePriorityScore(text, box),
                                ),
                            )
                        }.orEmpty()
                    }
                }
                    .distinctBy { it.text to it.boundingBox.flattenToString() }
                    .sortedByDescending { it.priorityScore }
                mainExecutor.execute { onTextBlocks(blocks) }
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }
}

private fun productNamePriorityScore(text: String, box: Rect): Float {
    val cleanText = text.trim()
    val textLengthFactor = when (cleanText.length) {
        in 2..16 -> 1.35f
        in 17..28 -> 0.95f
        else -> 0.48f
    }
    val keywordPenalty = if (NonProductNamePattern.containsMatchIn(cleanText)) 0.22f else 1f
    val numericPenalty = if (cleanText.count(Char::isDigit) > cleanText.length / 2) 0.30f else 1f
    val area = (box.width().coerceAtLeast(1) * box.height().coerceAtLeast(1)).toFloat()
    return (box.height() * 2.4f + kotlin.math.sqrt(area) * 0.6f) *
        textLengthFactor *
        keywordPenalty *
        numericPenalty
}

private val NonProductNamePattern =
    Regex("""(生产|日期|保质期|有效期|配料|营养|成分|净含量|贮存|保存|执行标准|许可证|地址|电话|公司|条码|扫码|客服|规格|批号|产地)""")

private fun mapBoundingBox(
    box: Rect,
    imageSize: Size?,
    overlaySize: IntSize,
): ComposeRect? {
    if (imageSize == null || overlaySize.width == 0 || overlaySize.height == 0) return null
    val scale = minOf(
        overlaySize.width.toFloat() / imageSize.width.toFloat(),
        overlaySize.height.toFloat() / imageSize.height.toFloat(),
    )
    val drawnWidth = imageSize.width * scale
    val drawnHeight = imageSize.height * scale
    val offsetX = (overlaySize.width - drawnWidth) / 2f
    val offsetY = (overlaySize.height - drawnHeight) / 2f

    return ComposeRect(
        left = offsetX + box.left * scale,
        top = offsetY + box.top * scale,
        right = offsetX + box.right * scale,
        bottom = offsetY + box.bottom * scale,
    )
}
