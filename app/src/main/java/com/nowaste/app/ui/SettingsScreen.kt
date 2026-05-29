package com.nowaste.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.AppTheme
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.ShelfLifeUnit
import com.nowaste.app.settings.AppSettings
import com.nowaste.app.settings.SettingsState
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    foodItems: List<FoodItem>,
    onNavigateBack: () -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onNearExpiryDaysChange: (Int) -> Unit,
    onAddCategoryTag: (String) -> Unit,
    onDeleteCategoryTag: (String) -> Unit,
    onMoveCategoryTag: (String, Int) -> Unit,
    onSmartParsingEnabledChange: (Boolean) -> Unit,
    onSmartParsingApiUrlChange: (String) -> Unit,
    onSmartParsingApiKeyChange: (String) -> Unit,
    onSmartParsingModelChange: (String) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onImportFoods: (List<FoodItemInput>, () -> Unit) -> Unit,
    onTestSmartParsing: ((String) -> Unit, (String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }
    var pendingExportContent by remember { mutableStateOf<String?>(null) }
    var dataFeedback by remember { mutableStateOf<String?>(null) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pendingExportContent
        dataFeedback = if (uri != null && content != null && writeExportContent(context, uri, content)) {
            "已导出 JSON 文件。"
        } else if (uri == null) {
            "已取消导出。"
        } else {
            "导出失败，请稍后重试。"
        }
        pendingExportContent = null
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val content = pendingExportContent
        dataFeedback = if (uri != null && content != null && writeExportContent(context, uri, content)) {
            "已导出 CSV 文件。"
        } else if (uri == null) {
            "已取消导出。"
        } else {
            "导出失败，请稍后重试。"
        }
        pendingExportContent = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            dataFeedback = "已取消导入。"
            return@rememberLauncherForActivityResult
        }
        parseImportContent(context, uri)
            .onSuccess { inputs ->
                if (inputs.isEmpty()) {
                    dataFeedback = "没有找到可导入的食品记录。"
                } else {
                    onImportFoods(inputs) {
                        dataFeedback = "已导入 ${inputs.size} 条记录。"
                    }
                }
            }
            .onFailure { error ->
                dataFeedback = error.message ?: "导入失败，请检查文件格式。"
            }
    }

    fun submitNewCategory() {
        val cleanCategory = newCategory.trim()
        if (cleanCategory.isNotBlank()) {
            onAddCategoryTag(cleanCategory)
            newCategory = ""
        }
    }

    fun exportData(format: ExportFormat) {
        val timestamp = LocalDateTime.now().format(ExportFileTimestampFormatter)
        pendingExportContent = when (format) {
            ExportFormat.Json -> foodItems.toJsonExport()
            ExportFormat.Csv -> foodItems.toCsvExport()
        }
        when (format) {
            ExportFormat.Json -> jsonExportLauncher.launch("nowaste-foods-$timestamp.json")
            ExportFormat.Csv -> csvExportLauncher.launch("nowaste-foods-$timestamp.csv")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = "每日提醒时间",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                )
                Text(
                    text = "提醒时间 ${settings.reminderHour.toString().padStart(2, '0')}:${settings.reminderMinute.toString().padStart(2, '0')}",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "自定义提前进入临期并提醒",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = settings.nearExpiryDays.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let(onNearExpiryDaysChange)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("提前天数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = "${AppSettings.MIN_NEAR_EXPIRY_DAYS}-${AppSettings.MAX_NEAR_EXPIRY_DAYS} 天，默认 ${AppSettings.DEFAULT_NEAR_EXPIRY_DAYS} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "界面主题",
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                    )
                    Text(
                        text = "当前主题: ${settings.theme.label}",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            SmartParsingSettings(
                settings = settings,
                onEnabledChange = onSmartParsingEnabledChange,
                onApiUrlChange = onSmartParsingApiUrlChange,
                onApiKeyChange = onSmartParsingApiKeyChange,
                onModelChange = onSmartParsingModelChange,
                onTestSmartParsing = onTestSmartParsing,
            )
            CategoryTagSettings(
                tags = settings.categoryTags,
                newCategory = newCategory,
                onNewCategoryChange = { newCategory = it },
                onAddCategory = ::submitNewCategory,
                onMoveCategory = onMoveCategoryTag,
                onDeleteCategoryRequest = { pendingDeleteCategory = it },
            )
            DataManagementSettings(
                itemCount = foodItems.size,
                feedback = dataFeedback,
                onExportJson = { exportData(ExportFormat.Json) },
                onExportCsv = { exportData(ExportFormat.Csv) },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/csv", "text/*", "*/*")) },
            )
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onReminderTimeChange(hour, minute)
                showTimePicker = false
            },
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = settings.theme,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                onThemeChange(theme)
                showThemeDialog = false
            },
        )
    }

    pendingDeleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("删除标签") },
            text = { Text("确定删除“$category”吗？已有食品上的标签文字不会被清空。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategoryTag(category)
                        pendingDeleteCategory = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SmartParsingSettings(
    settings: SettingsState,
    onEnabledChange: (Boolean) -> Unit,
    onApiUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestSmartParsing: ((String) -> Unit, (String) -> Unit) -> Unit,
) {
    var isTesting by remember { mutableStateOf(false) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var testSucceeded by remember { mutableStateOf<Boolean?>(null) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isApiKeyVisible by rememberSaveable { mutableStateOf(false) }
    val canTest = settings.smartParsingApiUrl.isNotBlank() &&
        settings.smartParsingApiKey.isNotBlank() &&
        settings.smartParsingModel.isNotBlank() &&
        !isTesting

    LaunchedEffect(settings.smartParsingEnabled) {
        if (!settings.smartParsingEnabled) {
            isExpanded = false
            isApiKeyVisible = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "智能解析",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "开启后，可从主页进入批量智能录入，一次解析多个食品候选项。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.smartParsingEnabled,
                onCheckedChange = onEnabledChange,
            )
        }

        if (settings.smartParsingEnabled) {
            Button(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
                Text(
                    text = if (isExpanded) "收起配置" else "展开配置",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = settings.smartParsingEnabled && isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = settings.smartParsingApiUrl,
                onValueChange = onApiUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API URL") },
                placeholder = { Text("https://api.deepseek.com/chat/completions") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                value = settings.smartParsingApiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = if (isApiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isApiKeyVisible) "隐藏 API Key" else "显示 API Key",
                        )
                    }
                },
            )
            OutlinedTextField(
                value = settings.smartParsingModel,
                onValueChange = onModelChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型名称") },
                placeholder = { Text("deepseek-chat") },
                singleLine = true,
            )
            Text(
                text = "请填写兼容 OpenAI Chat Completions 的完整接口地址。App 只发送你输入的文字。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    isTesting = true
                    testSucceeded = null
                    testFeedback = "正在测试连接..."
                    onTestSmartParsing(
                        { message ->
                            isTesting = false
                            testSucceeded = true
                            testFeedback = message
                        },
                        { message ->
                            isTesting = false
                            testSucceeded = false
                            testFeedback = message
                        },
                    )
                },
                enabled = canTest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isTesting) "正在测试..." else "测试连接")
            }
            testFeedback?.let { feedback ->
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (testSucceeded) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            }
        }
    }
}

@Composable
private fun DataManagementSettings(
    itemCount: Int,
    feedback: String?,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CollapsibleSectionHeader(
            title = "数据管理",
            subtitle = "导入或导出食品记录。",
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "当前共有 $itemCount 条食品记录，可导出为 JSON/CSV，也可导入此前导出的文件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                    )
                    Text(
                        text = "导入 JSON/CSV",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onExportJson,
                        enabled = itemCount > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                        )
                        Text(
                            text = "JSON",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Button(
                        onClick = onExportCsv,
                        enabled = itemCount > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                        )
                        Text(
                            text = "CSV",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (itemCount == 0) {
                    Text(
                        text = "暂无可导出的食品记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                feedback?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "收起$title" else "展开$title",
        )
    }
}

@Composable
private fun CategoryTagSettings(
    tags: List<String>,
    newCategory: String,
    onNewCategoryChange: (String) -> Unit,
    onAddCategory: () -> Unit,
    onMoveCategory: (String, Int) -> Unit,
    onDeleteCategoryRequest: (String) -> Unit,
) {
    var draggingTag by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val latestTags by rememberUpdatedState(tags)
    val latestOnMoveCategory by rememberUpdatedState(onMoveCategory)
    val moveThreshold = with(LocalDensity.current) { 52.dp.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CollapsibleSectionHeader(
            title = "食品标签",
            subtitle = "管理主页筛选和表单候选标签。",
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "长按标签并上下拖动可调整顺序。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = onNewCategoryChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("新增标签") },
                        singleLine = true,
                    )
                    Button(
                        onClick = onAddCategory,
                        enabled = newCategory.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                        Text("添加")
                    }
                }
                if (tags.isEmpty()) {
                    Text(
                        text = "还没有自定义标签。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    tags.forEach { tag ->
                        key(tag) {
                            val isDragging = draggingTag == tag
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                        shadowElevation = if (isDragging) 10.dp.toPx() else 0f
                                    }
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (isDragging) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                                        },
                                    )
                                    .pointerInput(tag) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingTag = tag
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingTag = null
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                draggingTag = null
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { _, dragAmount ->
                                                dragOffsetY += dragAmount.y
                                                val currentTags = latestTags
                                                val currentIndex = currentTags.indexOf(tag)
                                                when {
                                                    dragOffsetY <= -moveThreshold && currentIndex > 0 -> {
                                                        latestOnMoveCategory(tag, -1)
                                                        dragOffsetY = 0f
                                                    }

                                                    dragOffsetY >= moveThreshold && currentIndex in 0 until currentTags.lastIndex -> {
                                                        latestOnMoveCategory(tag, 1)
                                                        dragOffsetY = 0f
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                IconButton(onClick = { onDeleteCategoryRequest(tag) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除标签",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onSelect: (AppTheme) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择界面主题") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (theme == currentTheme) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable { onSelect(theme) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (theme == currentTheme) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择每日提醒时间") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private enum class ExportFormat {
    Json,
    Csv,
}

private val ExportFileTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private fun writeExportContent(
    context: Context,
    uri: Uri,
    content: String,
): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open export destination.")
    }.isSuccess

private fun parseImportContent(
    context: Context,
    uri: Uri,
): Result<List<FoodItemInput>> =
    runCatching {
        val content = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("无法读取导入文件。")

        when {
            content.trimStart().startsWith("{") || content.trimStart().startsWith("[") -> parseJsonImport(content)
            else -> parseCsvImport(content)
        }
    }

private fun parseJsonImport(content: String): List<FoodItemInput> {
    val trimmed = content.trim()
    val items = if (trimmed.startsWith("[")) {
        JSONArray(trimmed)
    } else {
        JSONObject(trimmed).optJSONArray("items") ?: JSONArray()
    }
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            item.toFoodItemInputOrNull()?.let(::add)
        }
    }
}

private fun JSONObject.toFoodItemInputOrNull(): FoodItemInput? {
    val name = optString("name").trim()
    val expiryDate = optNullableString("expiryDate")?.let(::parseImportDate) ?: return null
    if (name.isBlank()) return null

    return FoodItemInput(
        name = name,
        expiryDate = expiryDate,
        categoryTag = optString("categoryTag").trim(),
        note = optString("note").trim(),
        photoUri = optString("photoUri").trim(),
        quantity = optInt("quantity", 1).coerceAtLeast(1),
        productionDate = optNullableString("productionDate")?.let(::parseImportDate),
        shelfLifeAmount = optNullableLong("shelfLifeAmount"),
        shelfLifeUnit = optNullableString("shelfLifeUnit")?.let(::parseImportShelfLifeUnit),
        reminderDaysBeforeExpiry = optNullableInt("reminderDaysBeforeExpiry"),
    )
}

private fun parseCsvImport(content: String): List<FoodItemInput> {
    val rows = parseCsvRows(content).filter { row -> row.any { it.isNotBlank() } }
    if (rows.isEmpty()) return emptyList()
    val headers = rows.first().map { it.trim() }
    return rows.drop(1).mapNotNull { row ->
        val values = headers.zip(row + List((headers.size - row.size).coerceAtLeast(0)) { "" }).toMap()
        values.toFoodItemInputOrNull()
    }
}

private fun Map<String, String>.toFoodItemInputOrNull(): FoodItemInput? {
    val name = this["name"].orEmpty().trim()
    val expiryDate = this["expiryDate"]?.let(::parseImportDate) ?: return null
    if (name.isBlank()) return null

    return FoodItemInput(
        name = name,
        expiryDate = expiryDate,
        categoryTag = this["categoryTag"].orEmpty().trim(),
        note = this["note"].orEmpty().trim(),
        photoUri = this["photoUri"].orEmpty().trim(),
        quantity = this["quantity"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        productionDate = this["productionDate"]?.let(::parseImportDate),
        shelfLifeAmount = this["shelfLifeAmount"]?.toLongOrNull(),
        shelfLifeUnit = this["shelfLifeUnit"]?.let(::parseImportShelfLifeUnit),
        reminderDaysBeforeExpiry = this["reminderDaysBeforeExpiry"]?.toIntOrNull(),
    )
}

private fun List<FoodItem>.toJsonExport(): String {
    val items = JSONArray()
    forEach { item ->
        items.put(
            JSONObject()
                .put("id", item.id)
                .put("name", item.name)
                .put("expiryDate", item.expiryDate.toString())
                .put("productionDate", item.productionDate?.toString().orJsonNull())
                .put("shelfLifeAmount", item.shelfLifeAmount.orJsonNull())
                .put("shelfLifeUnit", item.shelfLifeUnit?.name.orJsonNull())
                .put("reminderDaysBeforeExpiry", item.reminderDaysBeforeExpiry.orJsonNull())
                .put("categoryTag", item.categoryTag)
                .put("quantity", item.quantity)
                .put("note", item.note)
                .put("photoUri", item.photoUri)
                .put("createdAt", item.createdAt.toString())
                .put("updatedAt", item.updatedAt.toString()),
        )
    }
    return JSONObject()
        .put("schemaVersion", 1)
        .put("exportedAt", LocalDateTime.now().toString())
        .put("items", items)
        .toString(2)
}

private fun List<FoodItem>.toCsvExport(): String {
    val headers = listOf(
        "id",
        "name",
        "expiryDate",
        "productionDate",
        "shelfLifeAmount",
        "shelfLifeUnit",
        "reminderDaysBeforeExpiry",
        "categoryTag",
        "quantity",
        "note",
        "photoUri",
        "createdAt",
        "updatedAt",
    )
    val rows = map { item ->
        listOf(
            item.id.toString(),
            item.name,
            item.expiryDate.toString(),
            item.productionDate?.toString().orEmpty(),
            item.shelfLifeAmount?.toString().orEmpty(),
            item.shelfLifeUnit?.name.orEmpty(),
            item.reminderDaysBeforeExpiry?.toString().orEmpty(),
            item.categoryTag,
            item.quantity.toString(),
            item.note,
            item.photoUri,
            item.createdAt.toString(),
            item.updatedAt.toString(),
        )
    }
    return buildString {
        appendLine(headers.joinToString(",") { it.toCsvCell() })
        rows.forEach { row ->
            appendLine(row.joinToString(",") { it.toCsvCell() })
        }
    }
}

private fun String.toCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun parseCsvRows(content: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var index = 0
    var inQuotes = false

    while (index < content.length) {
        val char = content[index]
        when {
            inQuotes && char == '"' && content.getOrNull(index + 1) == '"' -> {
                cell.append('"')
                index += 1
            }

            char == '"' -> inQuotes = !inQuotes
            !inQuotes && char == ',' -> {
                row.add(cell.toString())
                cell.clear()
            }

            !inQuotes && (char == '\n' || char == '\r') -> {
                if (char == '\r' && content.getOrNull(index + 1) == '\n') {
                    index += 1
                }
                row.add(cell.toString())
                rows.add(row.toList())
                row.clear()
                cell.clear()
            }

            else -> cell.append(char)
        }
        index += 1
    }

    if (cell.isNotEmpty() || row.isNotEmpty()) {
        row.add(cell.toString())
        rows.add(row.toList())
    }

    return rows
}

private fun parseImportDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun parseImportShelfLifeUnit(value: String): ShelfLifeUnit? =
    runCatching { ShelfLifeUnit.valueOf(value.trim()) }.getOrNull()

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun Any?.orJsonNull(): Any = this ?: JSONObject.NULL
