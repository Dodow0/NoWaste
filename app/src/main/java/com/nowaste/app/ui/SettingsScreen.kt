package com.nowaste.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.nowaste.app.dataio.FoodDataIo
import com.nowaste.app.dataio.FoodExportFormat
import com.nowaste.app.domain.AppTheme
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.settings.AppSettings
import com.nowaste.app.settings.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
    foodItems: List<FoodItem>,
    onNavigateBack: () -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
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
    val scope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }
    var dataFeedback by remember { mutableStateOf<String?>(null) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val export = pendingExport
        if (uri == null) {
            "已取消导出。"
                .also { dataFeedback = it }
            pendingExport = null
            return@rememberLauncherForActivityResult
        }
        if (export == null) {
            dataFeedback = "导出失败，请重试。"
            return@rememberLauncherForActivityResult
        }
        dataFeedback = "正在导出..."
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                writeExportContent(context, uri, export.items, export.format)
            }
            dataFeedback = if (success) {
                "已导出 ${export.format.label} 文件。"
            } else {
                "导出失败，请稍后重试。"
            }
            pendingExport = null
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val export = pendingExport
        if (uri == null) {
            "已取消导出。"
                .also { dataFeedback = it }
            pendingExport = null
            return@rememberLauncherForActivityResult
        }
        if (export == null) {
            dataFeedback = "导出失败，请重试。"
            return@rememberLauncherForActivityResult
        }
        dataFeedback = "正在导出..."
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                writeExportContent(context, uri, export.items, export.format)
            }
            dataFeedback = if (success) {
                "已导出 ${export.format.label} 文件。"
            } else {
                "导出失败，请稍后重试。"
            }
            pendingExport = null
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            dataFeedback = "已取消导入。"
            return@rememberLauncherForActivityResult
        }
        dataFeedback = "正在导入..."
        scope.launch {
            withContext(Dispatchers.IO) {
                parseImportContent(context, uri)
            }
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
    }

    fun submitNewCategory() {
        val cleanCategory = newCategory.trim()
        if (cleanCategory.isNotBlank()) {
            onAddCategoryTag(cleanCategory)
            newCategory = ""
        }
    }

    fun exportData(format: FoodExportFormat) {
        val timestamp = LocalDateTime.now().format(ExportFileTimestampFormatter)
        pendingExport = PendingExport(
            format = format,
            items = foodItems.toList(),
        )
        when (format) {
            FoodExportFormat.Json -> jsonExportLauncher.launch("nowaste-foods-$timestamp.json")
            FoodExportFormat.Csv -> csvExportLauncher.launch("nowaste-foods-$timestamp.csv")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsGroupCard(title = "常规") {
                ReminderTimeSettingItem(
                    hour = settings.reminderHour,
                    minute = settings.reminderMinute,
                    onClick = { showTimePicker = true },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                NotificationPermissionSettingItem(
                    granted = notificationPermissionGranted,
                    onEnableClick = onRequestNotificationPermission,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                NearExpiryDaysSetting(
                    days = settings.nearExpiryDays,
                    onDaysChange = onNearExpiryDaysChange,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ThemeSettingItem(
                    theme = settings.theme,
                    onClick = { showThemeDialog = true },
                )
            }

            SettingsGroupCard(title = "智能解析") {
                SmartParsingSettings(
                    settings = settings,
                    onEnabledChange = onSmartParsingEnabledChange,
                    onApiUrlChange = onSmartParsingApiUrlChange,
                    onApiKeyChange = onSmartParsingApiKeyChange,
                    onModelChange = onSmartParsingModelChange,
                    onTestSmartParsing = onTestSmartParsing,
                )
            }

            SettingsGroupCard(title = "食品标签") {
                CategoryTagSettings(
                    tags = settings.categoryTags,
                    newCategory = newCategory,
                    onNewCategoryChange = { newCategory = it },
                    onAddCategory = ::submitNewCategory,
                    onMoveCategory = onMoveCategoryTag,
                    onDeleteCategoryRequest = { pendingDeleteCategory = it },
                )
            }

            SettingsGroupCard(title = "数据管理") {
                DataManagementSettings(
                    itemCount = foodItems.size,
                    feedback = dataFeedback,
                    onExportJson = { exportData(FoodExportFormat.Json) },
                    onExportCsv = { exportData(FoodExportFormat.Csv) },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/csv", "text/*", "*/*")) },
                )
            }
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
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            content()
        }
    }
}

@Composable
private fun ReminderTimeSettingItem(
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
) {
    val timeText = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text("每日提醒时间") },
        supportingContent = { Text("当前 $timeText") },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun NotificationPermissionSettingItem(
    granted: Boolean,
    onEnableClick: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text("通知权限") },
        supportingContent = {
            Text(
                if (granted) {
                    "已允许，到期提醒可以显示通知。"
                } else {
                    "未允许，授权后才能收到每日到期提醒。"
                },
            )
        },
        trailingContent = {
            if (!granted) {
                Button(onClick = onEnableClick) {
                    Text("启用")
                }
            }
        },
    )
}

@Composable
private fun NearExpiryDaysSetting(
    days: Int,
    onDaysChange: (Int) -> Unit,
) {
    var daysText by rememberSaveable { mutableStateOf(days.toString()) }
    var isFocused by remember { mutableStateOf(false) }
    val parsedDays = remember(daysText) { parseNearExpiryDaysText(daysText) }
    val isValid = daysText.isBlank() || parsedDays != null

    fun restoreIfNeeded() {
        val committedDays = parseNearExpiryDaysText(daysText)
        if (committedDays == null) {
            daysText = days.toString()
        } else {
            daysText = committedDays.toString()
            if (committedDays != days) {
                onDaysChange(committedDays)
            }
        }
    }

    LaunchedEffect(days, isFocused) {
        if (!isFocused && daysText != days.toString()) {
            daysText = days.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "临期提醒提前量",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            value = daysText,
            onValueChange = { value ->
                val digits = value.filter(Char::isDigit)
                daysText = digits
                val newDays = parseNearExpiryDaysText(digits)
                if (newDays != null && newDays != days) {
                    onDaysChange(newDays)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (isFocused && !focusState.isFocused) {
                        restoreIfNeeded()
                    }
                    isFocused = focusState.isFocused
                },
            label = { Text("提前天数") },
            isError = !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text(
            text = if (isValid) {
                "${AppSettings.MIN_NEAR_EXPIRY_DAYS}-${AppSettings.MAX_NEAR_EXPIRY_DAYS} 天，默认 ${AppSettings.DEFAULT_NEAR_EXPIRY_DAYS} 天"
            } else {
                "请输入 ${AppSettings.MIN_NEAR_EXPIRY_DAYS}-${AppSettings.MAX_NEAR_EXPIRY_DAYS} 天。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isValid) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
        )
    }
}

@Composable
private fun ThemeSettingItem(
    theme: AppTheme,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text("界面主题") },
        supportingContent = { Text("当前 ${theme.label}") },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
                    text = "批量智能录入",
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
            CollapsibleSectionHeader(
                title = "解析配置",
                subtitle = "API 地址、密钥和模型名称。",
                isExpanded = isExpanded,
                onToggle = { isExpanded = !isExpanded },
            )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CollapsibleSectionHeader(
            title = "导入与导出",
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
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val latestTags by rememberUpdatedState(tags)
    val latestOnMoveCategory by rememberUpdatedState(onMoveCategory)
    val moveThreshold = with(LocalDensity.current) { 52.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CollapsibleSectionHeader(
            title = "标签列表",
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
                    text = "长按左侧手柄可调整顺序。",
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
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
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
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragIndicator,
                                        contentDescription = "拖动标签排序",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
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

private data class PendingExport(
    val format: FoodExportFormat,
    val items: List<FoodItem>,
)

private val FoodExportFormat.label: String
    get() = when (this) {
        FoodExportFormat.Json -> "JSON"
        FoodExportFormat.Csv -> "CSV"
    }

private val ExportFileTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private fun writeExportContent(
    context: Context,
    uri: Uri,
    items: List<FoodItem>,
    format: FoodExportFormat,
): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                FoodDataIo.exportFoods(items, format, writer)
            }
        } ?: error("Unable to open export destination.")
    }.isSuccess

private fun parseImportContent(
    context: Context,
    uri: Uri,
): Result<List<FoodItemInput>> =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                FoodDataIo.importFoods(reader)
            }
        }
            ?: error("无法读取导入文件。")
    }

internal fun parseNearExpiryDaysText(input: String): Int? =
    input.trim()
        .toIntOrNull()
        ?.takeIf { it in AppSettings.MIN_NEAR_EXPIRY_DAYS..AppSettings.MAX_NEAR_EXPIRY_DAYS }
