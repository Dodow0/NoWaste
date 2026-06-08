package com.nowaste.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nowaste.app.data.FoodItem
import com.nowaste.app.domain.ExpiryProgress
import com.nowaste.app.domain.FoodStatus
import com.nowaste.app.domain.calculateExpiryProgress
import com.nowaste.app.domain.calculateFoodStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val SwipeDeleteMinThreshold = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(
    uiState: FoodListUiState,
    onAddClick: () -> Unit,
    onBatchSmartAddClick: () -> Unit,
    onBatchPhotoAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFoodClick: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onQuantityChange: (FoodItem, Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
) {
    var pendingDeleteItem by remember { mutableStateOf<FoodItem?>(null) }
    val listState = rememberLazyListState()
    var isSpeedDialOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NoWaste",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            AddFoodSpeedDial(
                isOpen = isSpeedDialOpen,
                onToggle = { isSpeedDialOpen = !isSpeedDialOpen },
                onAddClick = {
                    isSpeedDialOpen = false
                    onAddClick()
                },
                onBatchSmartAddClick = {
                    isSpeedDialOpen = false
                    onBatchSmartAddClick()
                },
                onBatchPhotoAddClick = {
                    isSpeedDialOpen = false
                    onBatchPhotoAddClick()
                },
            )
        },
    ) { padding ->
        when (uiState) {
            FoodListUiState.Loading -> LoadingScreen(modifier = Modifier.padding(padding))
            is FoodListUiState.Ready -> {
                val categoryOptions = remember(uiState.items, uiState.settings.categoryTags) {
                    (uiState.settings.categoryTags + uiState.items.map { it.categoryTag }.filter { it.isNotBlank() })
                        .distinct()
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    ListControls(
                        query = uiState.query,
                        onQueryChange = onQueryChange,
                        selectedCategory = uiState.selectedCategory,
                        categories = categoryOptions,
                        onCategorySelected = onCategorySelected,
                    )
                    if (uiState.items.isEmpty()) {
                        EmptyFoodList(
                            modifier = Modifier.weight(1f),
                        )
                    } else if (uiState.filteredItems.isEmpty()) {
                        EmptyFilteredFoodList(
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        FoodList(
                            items = uiState.filteredItems,
                            nearExpiryDays = uiState.settings.nearExpiryDays,
                            listState = listState,
                            onFoodClick = onFoodClick,
                            onDeleteFood = { pendingDeleteItem = it },
                            onQuantityChange = onQuantityChange,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            title = {
                Text(
                    "删除食品",
                )
            },
            text = {
                Text(
                    "删除后无法恢复。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFood(item)
                        pendingDeleteItem = null
                    },
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun AddFoodSpeedDial(
    isOpen: Boolean,
    onToggle: () -> Unit,
    onAddClick: () -> Unit,
    onBatchSmartAddClick: () -> Unit,
    onBatchPhotoAddClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isOpen) {
            ExtendedFloatingActionButton(
                onClick = onBatchSmartAddClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                    )
                },
                text = { Text("智能批量") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            ExtendedFloatingActionButton(
                onClick = onBatchPhotoAddClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                    )
                },
                text = { Text("连续拍照") },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = { Text("添加食品") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = if (isOpen) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (isOpen) "收起添加操作" else "展开添加操作",
            )
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ListControls(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String?,
    categories: List<String>,
    onCategorySelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索名称或备注") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            item(key = "all") {
                CategoryFilterChip(
                    category = "全部",
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                )
            }
            items(
                items = categories,
                key = { it },
            ) { category ->
                CategoryFilterChip(
                    category = category,
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyFoodList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EmptyFoodIllustration(
                modifier = Modifier.size(180.dp),
            )
            Text(
                text = "还没有食品记录",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "从右下角添加第一件食品",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyFilteredFoodList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EmptyFoodIllustration(
                modifier = Modifier.size(150.dp),
            )
            Text(
                text = "没有找到匹配的食品",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "换个关键词或清除分类筛选试试。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyFoodIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        drawOval(
            color = tertiary.copy(alpha = 0.16f),
            topLeft = Offset(size.width * 0.16f, size.height * 0.62f),
            size = Size(size.width * 0.68f, size.height * 0.18f),
        )
        drawRoundRect(
            color = primary.copy(alpha = 0.14f),
            topLeft = Offset(size.width * 0.26f, size.height * 0.24f),
            size = Size(size.width * 0.48f, size.height * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
        )
        drawRoundRect(
            color = primary,
            topLeft = Offset(size.width * 0.36f, size.height * 0.18f),
            size = Size(size.width * 0.28f, size.height * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawCircle(
            color = tertiary,
            radius = size.minDimension * 0.08f,
            center = Offset(size.width * 0.38f, size.height * 0.45f),
        )
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = size.minDimension * 0.07f,
            center = Offset(size.width * 0.58f, size.height * 0.5f),
        )
        val arrow = Path().apply {
            moveTo(size.width * 0.72f, size.height * 0.74f)
            cubicTo(size.width * 0.86f, size.height * 0.82f, size.width * 0.94f, size.height * 0.86f, size.width * 0.98f, size.height * 0.96f)
        }
        drawPath(
            path = arrow,
            color = outline,
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}

@Composable
private fun FoodList(
    items: List<FoodItem>,
    nearExpiryDays: Int,
    listState: LazyListState,
    onFoodClick: (FoodItem) -> Unit,
    onDeleteFood: (FoodItem) -> Unit,
    onQuantityChange: (FoodItem, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { it.id },
            contentType = { "foodItem" },
        ) { item ->
            SwipeFoodItemRow(
                item = item,
                nearExpiryDays = nearExpiryDays,
                today = today,
                onClick = { onFoodClick(item) },
                onDeleteFood = { onDeleteFood(item) },
                onQuantityChange = { delta -> onQuantityChange(item, delta) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeFoodItemRow(
    item: FoodItem,
    nearExpiryDays: Int,
    today: LocalDate,
    onClick: () -> Unit,
    onDeleteFood: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeDeleteMinThresholdPx = with(density) { SwipeDeleteMinThreshold.toPx() }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance ->
            min(
                max(swipeDeleteMinThresholdPx, totalDistance * 0.45f),
                totalDistance * 0.85f,
            )
        },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart,
                -> onDeleteFood()
                SwipeToDismissBoxValue.StartToEnd,
                SwipeToDismissBoxValue.Settled,
                -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val value = dismissState.dismissDirection
            val backgroundColor = if (value == SwipeToDismissBoxValue.Settled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        FoodItemRow(
            item = item,
            nearExpiryDays = nearExpiryDays,
            today = today,
            onClick = onClick,
            onQuantityIncrease = { onQuantityChange(1) },
            onQuantityDecrease = { onQuantityChange(-1) },
        )
    }
}

@Composable
private fun FoodItemRow(
    item: FoodItem,
    nearExpiryDays: Int,
    today: LocalDate,
    onClick: () -> Unit,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
) {
    val effectiveNearExpiryDays = item.reminderDaysBeforeExpiry ?: nearExpiryDays
    val status = remember(item.expiryDate, today, effectiveNearExpiryDays) {
        calculateFoodListItemStatus(item, today, nearExpiryDays)
    }
    val statusColor = statusColor(status)
    val progress = remember(item.createdAt, item.expiryDate, today) {
        calculateExpiryProgress(
            createdDate = item.createdAt.toLocalDate(),
            expiryDate = item.expiryDate,
            today = today,
        )
    }
    val expiryDateText = remember(item.expiryDate) { item.expiryDate.format(DateFormatter) }
    val relativeExpiryText = remember(item.expiryDate, today) { relativeExpiryText(item.expiryDate, today) }
    val cardContainerColor = statusContainerColor(status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.categoryTag.isNotBlank()) {
                        CategoryChip(category = item.categoryTag)
                    }
                    QuantityStepper(
                        quantity = item.quantity,
                        onDecrease = onQuantityDecrease,
                        onIncrease = onQuantityIncrease,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = expiryDateText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = relativeExpiryText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.note.isNotBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ExpiryProgressRow(
                    progress = progress,
                    color = statusColor,
                )
            }
        }
    }
}

internal fun calculateFoodListItemStatus(
    item: FoodItem,
    today: LocalDate,
    globalNearExpiryDays: Int,
): FoodStatus =
    calculateFoodStatus(
        expiryDate = item.expiryDate,
        today = today,
        nearExpiryDays = item.reminderDaysBeforeExpiry ?: globalNearExpiryDays,
    )

@Composable
private fun CategoryChip(category: String) {
    Surface(
        modifier = Modifier.widthIn(max = 112.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDecrease,
                enabled = quantity > 1,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "数量减一",
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = "$quantity",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "数量加一",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpiryProgressRow(
    progress: ExpiryProgress,
    color: Color,
) {
    LinearProgressIndicator(
        progress = { progress.fractionElapsed },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun statusColor(status: FoodStatus): Color =
    when (status) {
        FoodStatus.Safe -> MaterialTheme.colorScheme.tertiary
        FoodStatus.NearExpiry -> Color(0xFFFFD54F)
        FoodStatus.Expired -> MaterialTheme.colorScheme.error
    }

@Composable
private fun statusContainerColor(status: FoodStatus): Color {
    val surface = MaterialTheme.colorScheme.surface
    return when (status) {
        FoodStatus.Safe -> surface
        FoodStatus.NearExpiry -> Color(0xFFFFD54F).copy(alpha = 0.14f).compositeOver(surface)
        FoodStatus.Expired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f).compositeOver(surface)
    }
}

private fun relativeExpiryText(expiryDate: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, expiryDate)
    return when {
        days > 0 -> "${days}天后"
        days == 0L -> "今天"
        else -> "已过期${-days}天"
    }
}
