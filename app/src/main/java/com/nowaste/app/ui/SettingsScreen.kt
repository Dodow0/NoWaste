package com.nowaste.app.ui

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowaste.app.domain.AppTheme
import com.nowaste.app.settings.AppSettings
import com.nowaste.app.settings.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsState,
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
    onTestSmartParsing: ((String) -> Unit, (String) -> Unit) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }

    fun submitNewCategory() {
        val cleanCategory = newCategory.trim()
        if (cleanCategory.isNotBlank()) {
            onAddCategoryTag(cleanCategory)
            newCategory = ""
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
    val canTest = settings.smartParsingApiUrl.isNotBlank() &&
        settings.smartParsingApiKey.isNotBlank() &&
        settings.smartParsingModel.isNotBlank() &&
        !isTesting

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
                visualTransformation = PasswordVisualTransformation(),
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
    val latestTags by rememberUpdatedState(tags)
    val latestOnMoveCategory by rememberUpdatedState(onMoveCategory)
    val moveThreshold = with(LocalDensity.current) { 52.dp.toPx() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "食品标签",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "这里管理主页筛选和表单候选标签的显示顺序。长按标签并上下拖动可调整顺序。",
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
