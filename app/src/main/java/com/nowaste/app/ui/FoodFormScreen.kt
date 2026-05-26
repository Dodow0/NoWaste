package com.nowaste.app.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.photos.createFoodPhotoUri
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FormDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodFormScreen(
    item: FoodItem?,
    onNavigateBack: () -> Unit,
    onScanBarcode: () -> Unit,
    categoryTags: List<String> = emptyList(),
    productLookupUiState: ProductLookupUiState = ProductLookupUiState.Idle,
    onLookupProduct: (String) -> Unit = {},
    onClearProductLookupState: () -> Unit = {},
    onSave: (FoodItemInput) -> Unit,
    onDelete: (() -> Unit)?,
    navBackStackEntry: NavBackStackEntry? = null,
) {
    val context = LocalContext.current
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var expiryDate by remember(item?.id) { mutableStateOf(item?.expiryDate ?: LocalDate.now().plusDays(3)) }
    var categoryTag by remember(item?.id) { mutableStateOf(item?.categoryTag.orEmpty()) }
    var note by remember(item?.id) { mutableStateOf(item?.note.orEmpty()) }
    var barcodeValue by remember(item?.id) { mutableStateOf(item?.barcodeValue.orEmpty()) }
    var photoUri by remember(item?.id) { mutableStateOf(item?.photoUri.orEmpty()) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var launchCameraCapture by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSpeechUnavailableDialog by remember { mutableStateOf(false) }
    var showCameraUnavailableDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }

    val scannedBarcodeState = if (navBackStackEntry != null) {
        navBackStackEntry.savedStateHandle
            .getStateFlow("scannedBarcode", "")
            .collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf("") }
    }
    val scannedBarcode by scannedBarcodeState
    LaunchedEffect(scannedBarcode) {
        if (scannedBarcode.isNotBlank()) {
            barcodeValue = scannedBarcode
            onLookupProduct(scannedBarcode)
            navBackStackEntry?.savedStateHandle?.set("scannedBarcode", "")
        }
    }

    LaunchedEffect(productLookupUiState) {
        val result = (productLookupUiState as? ProductLookupUiState.Success)?.result
        if (result != null) {
            if (name.isBlank() && !result.name.isNullOrBlank()) {
                name = result.name
            }
            if (categoryTag.isBlank() && !result.categoryTag.isNullOrBlank()) {
                categoryTag = result.categoryTag
            }
            onClearProductLookupState()
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spokenText.isNotBlank()) {
                note = if (note.isBlank()) spokenText else "$note\n$spokenText"
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingPhotoUri?.let { photoUri = it.toString() }
        }
        pendingPhotoUri = null
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCameraCapture = true
        } else {
            showCameraPermissionDeniedDialog = true
        }
    }

    LaunchedEffect(launchCameraCapture) {
        if (launchCameraCapture) {
            launchCameraCapture = false
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
                showCameraUnavailableDialog = true
            } catch (_: SecurityException) {
                pendingPhotoUri = null
                showCameraPermissionDeniedDialog = true
            }
        }
    }

    val isEditing = item != null
    val canSave = name.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑食品" else "添加食品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormSectionCard(title = "核心信息") {
                SoftTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "食品名称",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                SoftTextField(
                    value = expiryDate.format(FormDateFormatter),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = "到期日",
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "选择日期",
                            )
                        }
                    },
                )
            }

            FormSectionCard(title = "附加信息") {
                SoftTextField(
                    value = categoryTag,
                    onValueChange = { categoryTag = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "分类标签",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                CategoryTagSelector(
                    categoryTags = categoryTags,
                    selectedCategory = categoryTag,
                    onCategorySelected = { categoryTag = it },
                )
                SoftTextField(
                    value = barcodeValue,
                    onValueChange = { barcodeValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "条形码",
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onScanBarcode) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "扫描条形码",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                ProductLookupFeedback(productLookupUiState = productLookupUiState)
            }

            FormSectionCard(title = "照片与备注") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoftTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.weight(1f),
                        label = "备注",
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    )
                    IconButton(
                        onClick = {
                            try {
                                voiceLauncher.launch(
                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                        )
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "说出备注内容")
                                    },
                                )
                            } catch (_: ActivityNotFoundException) {
                                showSpeechUnavailableDialog = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "语音输入备注",
                        )
                    }
                    IconButton(
                        onClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) {
                                launchCameraCapture = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "拍照记录",
                        )
                    }
                }
                if (photoUri.isNotBlank()) {
                    PhotoPreview(
                        photoUri = photoUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.large),
                    )
                    TextButton(onClick = { photoUri = "" }) {
                        Text("移除照片")
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        FoodItemInput(
                            name = name,
                            expiryDate = expiryDate,
                            categoryTag = categoryTag,
                            note = note,
                            barcodeValue = barcodeValue,
                            photoUri = photoUri,
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                )
                Text(
                    text = "保存",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate.toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            expiryDate = it.toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除食品") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }

    if (showSpeechUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showSpeechUnavailableDialog = false },
            title = { Text("无法使用语音输入") },
            text = { Text("当前设备没有可用的语音识别服务。") },
            confirmButton = {
                TextButton(onClick = { showSpeechUnavailableDialog = false }) {
                    Text("知道了")
                }
            },
        )
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
            text = { Text("需要相机权限才能拍摄食品包装照片。") },
            confirmButton = {
                TextButton(onClick = { showCameraPermissionDeniedDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingFoodScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记录不存在") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "这条食品记录已经不存在",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun CategoryTagSelector(
    categoryTags: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    val categories = remember(categoryTags, selectedCategory) {
        (categoryTags + selectedCategory.takeIf { it.isNotBlank() }.orEmpty())
            .filter { it.isNotBlank() }
            .distinct()
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        item(key = "none") {
            FilterChip(
                selected = selectedCategory.isBlank(),
                onClick = { onCategorySelected("") },
                label = { Text("无标签") },
            )
        }
        items(
            items = categories,
            key = { it },
        ) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    readOnly: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = singleLine,
        readOnly = readOnly,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ProductLookupFeedback(productLookupUiState: ProductLookupUiState) {
    when (productLookupUiState) {
        ProductLookupUiState.Loading -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(
                text = "正在查询商品信息...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        ProductLookupUiState.Failed -> Text(
            text = "没有查到商品信息，可继续手动填写。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is ProductLookupUiState.Success -> if (productLookupUiState.result == null) {
            Text(
                text = "没有查到商品信息，可继续手动填写。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProductLookupUiState.Idle -> Unit
    }
}

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun PhotoPreview(
    photoUri: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageURI(Uri.parse(photoUri))
        },
    )
}
