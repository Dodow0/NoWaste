package com.nowaste.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nowaste.app.domain.FoodItemInput
import com.nowaste.app.domain.ShelfLifeDuration
import com.nowaste.app.domain.extractExpiryDateFromText
import com.nowaste.app.domain.extractProductionDateFromText
import com.nowaste.app.domain.extractShelfLifeDurationFromText
import com.nowaste.app.network.SmartFoodParseResult
import com.nowaste.app.settings.AppSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val BatchDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchSmartParseScreen(
    smartParsingEnabled: Boolean,
    categoryTags: List<String>,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onParseText: (String, (List<SmartFoodParseResult>) -> Unit, (String) -> Unit) -> Unit,
    onSaveFoods: (List<FoodItemInput>) -> Unit,
) {
    var rawText by remember { mutableStateOf("") }
    var drafts by remember { mutableStateOf<List<BatchSmartFoodDraft>>(emptyList()) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var nextDraftId by remember { mutableIntStateOf(1) }

    fun updateDraft(id: Int, transform: (BatchSmartFoodDraft) -> BatchSmartFoodDraft) {
        drafts = drafts.map { draft -> if (draft.id == id) transform(draft) else draft }
    }

    val inputs = drafts.mapNotNull { it.toFoodItemInput() }
    val canSave = drafts.isNotEmpty() && inputs.size == drafts.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量智能录入") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!smartParsingEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "批量智能录入未开启",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "请先在设置中开启智能解析，并填写兼容 OpenAI Chat Completions 的 API URL、Key 和模型名称。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onOpenSettings) {
                            Text("去设置")
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "一次输入多件商品",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "例：牛奶三盒，生产日期25年12月29，保质期3个月。饼干两包，生产日期26年3月13，保质期2个月。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("批量录入文本") },
                            minLines = 4,
                            maxLines = 8,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        )
                        Button(
                            onClick = {
                                isParsing = true
                                feedback = "正在识别多个商品..."
                                onParseText(
                                    rawText,
                                    { results ->
                                        val parsedDrafts = results.mapIndexed { index, result ->
                                            result.toBatchDraft(nextDraftId + index)
                                        }
                                        nextDraftId += parsedDrafts.size
                                        drafts = parsedDrafts
                                        isParsing = false
                                        feedback = "已识别 ${parsedDrafts.size} 个候选商品，请检查后保存。"
                                    },
                                    { message ->
                                        isParsing = false
                                        feedback = message
                                    },
                                )
                            },
                            enabled = rawText.isNotBlank() && !isParsing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                )
                            }
                            Text(
                                text = if (isParsing) "解析中..." else "批量智能解析",
                                modifier = Modifier.padding(start = if (isParsing) 0.dp else 8.dp),
                            )
                        }
                        feedback?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isParsing) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                drafts.forEachIndexed { index, draft ->
                    key(draft.id) {
                        BatchDraftCard(
                            index = index,
                            draft = draft,
                            categoryTags = categoryTags,
                            onChange = { updated -> updateDraft(draft.id) { updated } },
                            onDelete = { drafts = drafts.filterNot { it.id == draft.id } },
                        )
                    }
                }

                if (drafts.isNotEmpty()) {
                    Button(
                        onClick = { onSaveFoods(inputs) },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                        )
                        Text(
                            text = "保存 ${drafts.size} 个商品",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (!canSave) {
                        Text(
                            text = "请确保每个候选商品都有名称，并能得到到期日。到期日可直接填写，也可由生产日期 + 保质期计算。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchDraftCard(
    index: Int,
    draft: BatchSmartFoodDraft,
    categoryTags: List<String>,
    onChange: (BatchSmartFoodDraft) -> Unit,
    onDelete: () -> Unit,
) {
    val validation = draft.validationMessage()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
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
                Text(
                    text = "候选商品 ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除候选商品",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("食品名称") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.productionDateText,
                    onValueChange = { onChange(draft.copy(productionDateText = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("生产日期") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = draft.shelfLifeText,
                    onValueChange = { onChange(draft.copy(shelfLifeText = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("保质期") },
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = draft.expiryDateText,
                onValueChange = { onChange(draft.copy(expiryDateText = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("到期日") },
                singleLine = true,
            )
            OutlinedTextField(
                value = draft.categoryTag,
                onValueChange = { onChange(draft.copy(categoryTag = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类标签") },
                singleLine = true,
            )
            BatchCategorySelector(
                categoryTags = categoryTags,
                selectedCategory = draft.categoryTag,
                onCategorySelected = { onChange(draft.copy(categoryTag = it)) },
            )
            OutlinedTextField(
                value = draft.reminderDaysBeforeExpiryText,
                onValueChange = { value ->
                    onChange(draft.copy(reminderDaysBeforeExpiryText = value.filter(Char::isDigit)))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("单独提醒提前天数（可选）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { onChange(draft.copy(note = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注") },
                minLines = 2,
                maxLines = 4,
            )
            validation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun BatchCategorySelector(
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

private data class BatchSmartFoodDraft(
    val id: Int,
    val name: String = "",
    val productionDateText: String = "",
    val shelfLifeText: String = "",
    val expiryDateText: String = "",
    val categoryTag: String = "",
    val reminderDaysBeforeExpiryText: String = "",
    val note: String = "",
)

private fun SmartFoodParseResult.toBatchDraft(id: Int): BatchSmartFoodDraft =
    BatchSmartFoodDraft(
        id = id,
        name = name.orEmpty(),
        productionDateText = productionDate?.format(BatchDateFormatter).orEmpty(),
        shelfLifeText = shelfLife?.toDisplayText().orEmpty(),
        expiryDateText = expiryDate?.format(BatchDateFormatter).orEmpty(),
        categoryTag = categoryTag.orEmpty(),
        note = note.orEmpty(),
    )

private fun BatchSmartFoodDraft.toFoodItemInput(): FoodItemInput? {
    val cleanName = name.trim()
    if (cleanName.isBlank()) return null

    val productionDate = parseBatchProductionDate(productionDateText)
    val shelfLife = parseBatchShelfLife(shelfLifeText)
    val reminderDaysBeforeExpiry = parseBatchReminderDays(reminderDaysBeforeExpiryText)
    val expiryDate = parseBatchExpiryDate(expiryDateText)
        ?: productionDate?.let { date -> shelfLife?.addTo(date) }
        ?: return null

    if (productionDateText.isNotBlank() && productionDate == null) return null
    if (shelfLifeText.isNotBlank() && shelfLife == null) return null
    if (expiryDateText.isNotBlank() && parseBatchExpiryDate(expiryDateText) == null) return null
    if (reminderDaysBeforeExpiryText.isNotBlank() && reminderDaysBeforeExpiry == null) return null

    return FoodItemInput(
        name = cleanName,
        expiryDate = expiryDate,
        categoryTag = categoryTag.trim(),
        note = note.trim(),
        productionDate = productionDate,
        shelfLifeAmount = shelfLife?.amount,
        shelfLifeUnit = shelfLife?.unit,
        reminderDaysBeforeExpiry = reminderDaysBeforeExpiry,
    )
}

private fun BatchSmartFoodDraft.validationMessage(): String? {
    val productionDate = parseBatchProductionDate(productionDateText)
    val shelfLife = parseBatchShelfLife(shelfLifeText)
    val expiryDate = parseBatchExpiryDate(expiryDateText)
    val reminderDaysBeforeExpiry = parseBatchReminderDays(reminderDaysBeforeExpiryText)
    val calculatedExpiryDate = productionDate?.let { date -> shelfLife?.addTo(date) }

    return when {
        name.isBlank() -> "请补充食品名称。"
        productionDateText.isNotBlank() && productionDate == null -> "生产日期格式不对。"
        shelfLifeText.isNotBlank() && shelfLife == null -> "保质期格式不对，可以填 35日、十八个月、6个月。"
        expiryDateText.isNotBlank() && expiryDate == null -> "到期日格式不对。"
        reminderDaysBeforeExpiryText.isNotBlank() && reminderDaysBeforeExpiry == null ->
            "单独提醒提前天数需为 ${AppSettings.MIN_NEAR_EXPIRY_DAYS}-${AppSettings.MAX_NEAR_EXPIRY_DAYS} 天。"
        expiryDate == null && calculatedExpiryDate == null -> "请填写到期日，或填写生产日期和保质期。"
        else -> null
    }
}

private fun parseBatchProductionDate(input: String): LocalDate? =
    input.trim().takeIf { it.isNotBlank() }?.let(::extractProductionDateFromText)

private fun parseBatchExpiryDate(input: String): LocalDate? =
    input.trim().takeIf { it.isNotBlank() }?.let(::extractExpiryDateFromText)

private fun parseBatchShelfLife(input: String): ShelfLifeDuration? =
    input.trim().takeIf { it.isNotBlank() }?.let(::extractShelfLifeDurationFromText)

private fun parseBatchReminderDays(input: String): Int? =
    input.trim()
        .takeIf { it.isNotBlank() }
        ?.toIntOrNull()
        ?.takeIf { it in AppSettings.MIN_NEAR_EXPIRY_DAYS..AppSettings.MAX_NEAR_EXPIRY_DAYS }
