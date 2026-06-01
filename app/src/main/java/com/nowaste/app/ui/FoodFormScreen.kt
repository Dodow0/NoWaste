package com.nowaste.app.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.layout.ContentScale
import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.BatchPhotoPendingNamePrefix
import com.nowaste.app.domain.BatchPhotoPendingNote
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.ShelfLifeDuration
import com.nowaste.app.domain.extractExpiryDateFromText
import com.nowaste.app.domain.extractProductionDateFromText
import com.nowaste.app.domain.extractShelfLifeDurationFromText
import com.nowaste.app.photos.createFoodPhotoUri
import com.nowaste.app.photos.deleteFoodPhotoUri
import com.nowaste.app.settings.AppSettings
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
    onPickNameFromPhoto: () -> Unit,
    onPickDateFromCamera: (DateOcrField) -> Unit,
    categoryTags: List<String> = emptyList(),
    onSave: (FoodItemInput) -> Unit,
    onDelete: (() -> Unit)?,
    navBackStackEntry: NavBackStackEntry? = null,
) {
    val context = LocalContext.current
    var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var productionDateText by rememberSaveable(item?.id) {
        mutableStateOf(item?.productionDate?.format(FormDateFormatter).orEmpty())
    }
    var shelfLifeText by rememberSaveable(item?.id) {
        mutableStateOf(item?.storedShelfLifeText().orEmpty())
    }
    var reminderDaysBeforeExpiryText by rememberSaveable(item?.id) {
        mutableStateOf(item?.reminderDaysBeforeExpiry?.toString().orEmpty())
    }
    var expiryDateText by rememberSaveable(item?.id) {
        mutableStateOf(item?.expiryDate?.format(FormDateFormatter).orEmpty())
    }
    var categoryTag by rememberSaveable(item?.id) { mutableStateOf(item?.categoryTag.orEmpty()) }
    var quantityText by rememberSaveable(item?.id) { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var note by rememberSaveable(item?.id) { mutableStateOf(item?.note.orEmpty()) }
    var photoUri by rememberSaveable(item?.id) { mutableStateOf(item?.photoUri.orEmpty()) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var launchCameraCapture by remember { mutableStateOf(false) }
    var showProductionDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDiscardChangesDialog by rememberSaveable(item?.id) { mutableStateOf(false) }
    var showCameraUnavailableDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var hasEditedExpiryDate by rememberSaveable(item?.id) { mutableStateOf(false) }
    var isSaving by rememberSaveable(item?.id) { mutableStateOf(false) }
    val parsedProductionDate = remember(productionDateText) {
        parseProductionDateInput(productionDateText)
    }
    val parsedShelfLife = remember(shelfLifeText) {
        extractShelfLifeDurationFromText(shelfLifeText)
    }
    val parsedExpiryDate = remember(expiryDateText) {
        parseExpiryDateInput(expiryDateText)
    }
    val parsedReminderDaysBeforeExpiry = remember(reminderDaysBeforeExpiryText) {
        reminderDaysBeforeExpiryText.trim().takeIf { it.isNotBlank() }?.toIntOrNull()
    }
    val parsedQuantity = remember(quantityText) {
        quantityText.trim().toIntOrNull()
    }
    val calculatedExpiryDate = parsedProductionDate?.let { productionDate ->
        parsedShelfLife?.addTo(productionDate)
    }

    val selectedFoodNameState = if (navBackStackEntry != null) {
        navBackStackEntry.savedStateHandle
            .getStateFlow("selectedFoodName", "")
            .collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf("") }
    }
    val selectedFoodName by selectedFoodNameState
    LaunchedEffect(selectedFoodName) {
        if (selectedFoodName.isNotBlank()) {
            name = selectedFoodName.trim()
            navBackStackEntry?.savedStateHandle?.set("selectedFoodName", "")
        }
    }

    val selectedProductionDateState = if (navBackStackEntry != null) {
        navBackStackEntry.savedStateHandle
            .getStateFlow(DateOcrField.ProductionDate.savedStateKey, "")
            .collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf("") }
    }
    val selectedProductionDate by selectedProductionDateState
    LaunchedEffect(selectedProductionDate) {
        if (selectedProductionDate.isNotBlank()) {
            productionDateText = selectedProductionDate
            navBackStackEntry?.savedStateHandle?.set(DateOcrField.ProductionDate.savedStateKey, "")
        }
    }

    val selectedShelfLifeState = if (navBackStackEntry != null) {
        navBackStackEntry.savedStateHandle
            .getStateFlow(DateOcrField.ShelfLife.savedStateKey, "")
            .collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf("") }
    }
    val selectedShelfLife by selectedShelfLifeState
    LaunchedEffect(selectedShelfLife) {
        if (selectedShelfLife.isNotBlank()) {
            shelfLifeText = selectedShelfLife
            navBackStackEntry?.savedStateHandle?.set(DateOcrField.ShelfLife.savedStateKey, "")
        }
    }

    val selectedExpiryDateState = if (navBackStackEntry != null) {
        navBackStackEntry.savedStateHandle
            .getStateFlow(DateOcrField.ExpiryDate.savedStateKey, "")
            .collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf("") }
    }
    val selectedExpiryDate by selectedExpiryDateState
    LaunchedEffect(selectedExpiryDate) {
        if (selectedExpiryDate.isNotBlank()) {
            hasEditedExpiryDate = true
            expiryDateText = selectedExpiryDate
            navBackStackEntry?.savedStateHandle?.set(DateOcrField.ExpiryDate.savedStateKey, "")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pendingPhotoUri?.let {
            if (result.resultCode == Activity.RESULT_OK) {
                photoUri = it.toString()
            } else {
                deleteFoodPhotoUri(context, it)
            }
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
                deleteFoodPhotoUri(context, uri)
                showCameraUnavailableDialog = true
            } catch (_: SecurityException) {
                pendingPhotoUri = null
                deleteFoodPhotoUri(context, uri)
                showCameraPermissionDeniedDialog = true
            }
        }
    }

    val isEditing = item != null
    LaunchedEffect(calculatedExpiryDate, hasEditedExpiryDate) {
        if (calculatedExpiryDate != null && !hasEditedExpiryDate) {
            expiryDateText = calculatedExpiryDate.format(FormDateFormatter)
        }
    }

    fun requestFoodPhotoCapture() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchCameraCapture = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val dateInputFeedback = dateInputFeedback(
        productionDateText = productionDateText,
        parsedProductionDate = parsedProductionDate,
        shelfLifeText = shelfLifeText,
        hasParsedShelfLife = parsedShelfLife != null,
        expiryDateText = expiryDateText,
        parsedExpiryDate = parsedExpiryDate,
        calculatedExpiryDate = calculatedExpiryDate,
    )
    val hasValidOptionalDateInputs =
        (productionDateText.isBlank() || parsedProductionDate != null) &&
            (shelfLifeText.isBlank() || parsedShelfLife != null)
    val reminderDaysRange = AppSettings.MIN_NEAR_EXPIRY_DAYS..AppSettings.MAX_NEAR_EXPIRY_DAYS
    val hasValidReminderDays =
        reminderDaysBeforeExpiryText.isBlank() ||
            (parsedReminderDaysBeforeExpiry != null && parsedReminderDaysBeforeExpiry in reminderDaysRange)
    val hasValidQuantity = parsedQuantity != null && parsedQuantity >= 1
    val canSave = name.isNotBlank() &&
        parsedExpiryDate != null &&
        hasValidOptionalDateInputs &&
        hasValidReminderDays &&
        hasValidQuantity
    val hasUnsavedChanges =
        name != item?.name.orEmpty() ||
            productionDateText != item?.productionDate?.format(FormDateFormatter).orEmpty() ||
            shelfLifeText != item?.storedShelfLifeText().orEmpty() ||
            reminderDaysBeforeExpiryText != item?.reminderDaysBeforeExpiry?.toString().orEmpty() ||
            expiryDateText != item?.expiryDate?.format(FormDateFormatter).orEmpty() ||
            categoryTag != item?.categoryTag.orEmpty() ||
            quantityText != (item?.quantity ?: 1).toString() ||
            note != item?.note.orEmpty() ||
            photoUri != item?.photoUri.orEmpty()

    fun requestNavigateBack() {
        if (isSaving) return
        if (hasUnsavedChanges) {
            showDiscardChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        requestNavigateBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑食品" else "添加食品") },
                navigationIcon = {
                    IconButton(onClick = { requestNavigateBack() }) {
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
                    trailingIcon = {
                        IconButton(onClick = onPickNameFromPhoto) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "从包装文字选择商品名",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                SoftTextField(
                    value = productionDateText,
                    onValueChange = { productionDateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "生产日期（可选）",
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showProductionDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "选择生产日期",
                                )
                            }
                            IconButton(onClick = { onPickDateFromCamera(DateOcrField.ProductionDate) }) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "拍照识别生产日期",
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                SoftTextField(
                    value = shelfLifeText,
                    onValueChange = { shelfLifeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "保质期（可选，配合生产日期自动计算）",
                    singleLine = true,
                    trailingIcon = {
                            IconButton(onClick = { onPickDateFromCamera(DateOcrField.ShelfLife) }) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "拍照识别保质期",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                SoftTextField(
                    value = expiryDateText,
                    onValueChange = {
                        hasEditedExpiryDate = true
                        expiryDateText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "到期日（必填）",
                    singleLine = true,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showExpiryDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "选择到期日",
                                )
                            }
                            IconButton(onClick = { onPickDateFromCamera(DateOcrField.ExpiryDate) }) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "拍照识别到期日",
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                dateInputFeedback?.let { feedback ->
                    Text(
                        text = feedback.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feedback.isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            FormSectionCard(title = "附加信息") {
                SoftTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "数量",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                if (!hasValidQuantity) {
                    Text(
                        text = "数量至少为 1。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
                    value = reminderDaysBeforeExpiryText,
                    onValueChange = { reminderDaysBeforeExpiryText = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "单独提醒提前天数（可选）",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                Text(
                    text = "留空则使用设置页的默认提前天数；可填 ${AppSettings.MIN_NEAR_EXPIRY_DAYS}-${AppSettings.MAX_NEAR_EXPIRY_DAYS} 天。",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasValidReminderDays) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
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
                        onClick = { requestFoodPhotoCapture() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "拍照记录",
                        )
                    }
                }
                Text(
                    text = "位置、照片等",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (photoUri.isNotBlank()) {
                    PhotoPreview(
                        photoUri = photoUri,
                        onClick = { showPhotoViewer = true },
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
                    if (isSaving) return@Button
                    val expiryDate = parsedExpiryDate ?: return@Button
                    isSaving = true
                    val cleanNote = if (
                        item?.note == BatchPhotoPendingNote &&
                        note == BatchPhotoPendingNote &&
                        !name.trim().startsWith(BatchPhotoPendingNamePrefix)
                    ) {
                        ""
                    } else {
                        note
                    }
                    onSave(
                        FoodItemInput(
                            name = name,
                            expiryDate = expiryDate,
                            categoryTag = categoryTag,
                            quantity = parsedQuantity ?: 1,
                            note = cleanNote,
                            photoUri = photoUri,
                            productionDate = parsedProductionDate,
                            shelfLifeAmount = parsedShelfLife?.amount,
                            shelfLifeUnit = parsedShelfLife?.unit,
                            reminderDaysBeforeExpiry = parsedReminderDaysBeforeExpiry,
                        ),
                    )
                },
                enabled = canSave && !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                )
                Text(
                    text = if (isSaving) "保存中..." else "保存",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showProductionDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (parsedProductionDate ?: LocalDate.now()).toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showProductionDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            productionDateText = it.toLocalDate().format(FormDateFormatter)
                        }
                        showProductionDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProductionDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExpiryDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (parsedExpiryDate ?: calculatedExpiryDate ?: LocalDate.now()).toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            hasEditedExpiryDate = true
                            expiryDateText = it.toLocalDate().format(FormDateFormatter)
                        }
                        showExpiryDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
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

    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            title = { Text("放弃未保存内容？") },
            text = { Text("当前填写的信息还没有保存，退出后这些修改会丢失。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChangesDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardChangesDialog = false }) {
                    Text("继续填写")
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

    if (showPhotoViewer && photoUri.isNotBlank()) {
        PhotoViewerDialog(
            photoUri = photoUri,
            onDismiss = { showPhotoViewer = false },
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

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun FoodItem.storedShelfLifeText(): String? {
    val amount = shelfLifeAmount ?: return null
    val unit = shelfLifeUnit ?: return null
    return ShelfLifeDuration(amount, unit).toDisplayText()
}

private data class DateInputFeedback(
    val message: String,
    val isError: Boolean,
)

private fun parseProductionDateInput(input: String): LocalDate? =
    input.trim().takeIf { it.isNotBlank() }?.let(::extractProductionDateFromText)

private fun parseExpiryDateInput(input: String): LocalDate? =
    input.trim().takeIf { it.isNotBlank() }?.let(::extractExpiryDateFromText)

private fun dateInputFeedback(
    productionDateText: String,
    parsedProductionDate: LocalDate?,
    shelfLifeText: String,
    hasParsedShelfLife: Boolean,
    expiryDateText: String,
    parsedExpiryDate: LocalDate?,
    calculatedExpiryDate: LocalDate?,
): DateInputFeedback? =
    when {
        productionDateText.isNotBlank() && parsedProductionDate == null -> DateInputFeedback(
            message = "生产日期格式不对，请输入 2026-05-20 这样的日期。",
            isError = true,
        )

        shelfLifeText.isNotBlank() && !hasParsedShelfLife -> DateInputFeedback(
            message = "保质期格式不太对，可以输入 180天、35日、12个月，或“保质期 6 个月”。",
            isError = true,
        )

        expiryDateText.isNotBlank() && parsedExpiryDate == null -> DateInputFeedback(
            message = "到期日格式不对，请输入 2026-05-20，或通过日历/拍照识别填写。",
            isError = true,
        )

        shelfLifeText.isNotBlank() && hasParsedShelfLife && parsedProductionDate == null && expiryDateText.isBlank() ->
            DateInputFeedback(
                message = "请补充生产日期以自动计算到期日，或直接填写最终到期日。",
                isError = true,
            )

        calculatedExpiryDate != null -> DateInputFeedback(
            message = "已根据生产日期和保质期计算最终到期日：${calculatedExpiryDate.format(FormDateFormatter)}",
            isError = false,
        )

        else -> null
    }

@Composable
private fun PhotoPreview(
    photoUri: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SampledPhotoImage(
        photoUri = photoUri,
        contentDescription = "食品照片",
        modifier = modifier.clickable(onClick = onClick),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PhotoViewerDialog(
    photoUri: String,
    onDismiss: () -> Unit,
) {
    var scale by remember(photoUri) { mutableStateOf(1f) }
    var offsetX by remember(photoUri) { mutableStateOf(0f) }
    var offsetY by remember(photoUri) { mutableStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        offsetX += panChange.x
        offsetY += panChange.y
        if (nextScale == 1f) {
            offsetX = 0f
            offsetY = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            FullscreenSampledPhotoImage(
                photoUri = photoUri,
                contentDescription = "食品照片",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    )
                    .transformable(transformableState),
                contentScale = ContentScale.Fit,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Text("关闭", color = Color.White)
            }
        }
    }
}
