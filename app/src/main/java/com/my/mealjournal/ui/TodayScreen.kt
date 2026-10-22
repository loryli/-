package com.my.mealjournal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.data.EntryView
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.domain.DayStatus
import com.my.mealjournal.domain.MealTiming
import com.my.mealjournal.domain.MealType
import com.my.mealjournal.domain.Portion
import com.my.mealjournal.ui.design.AppButton
import com.my.mealjournal.ui.design.AppButtonStyle
import com.my.mealjournal.ui.design.AppChip
import com.my.mealjournal.ui.design.ListRow
import com.my.mealjournal.ui.design.SectionHeader
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(vm: TodayViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var addingMeal by remember { mutableStateOf<MealType?>(null) }
    var addingFromFab by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EntryView?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun offerUndo(message: String, action: String, onAction: () -> Unit) {
        scope.launch {
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = action,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.md, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            DateHeader(
                date = state.date,
                editable = state.editable,
                onShift = { days -> vm.selectDate(state.date.plusDays(days)) },
                onToday = { vm.selectDate(LocalDate.now()) },
            )

            SummaryCard(state)

            SectionHeader(
                title = "四餐",
                subtitle = if (state.editable) "点任意一餐添加，点条目修改" else "仅可查看，超出 7 天补记窗口",
            )

            MealsCard(
                state = state,
                onAdd = { meal ->
                    addingMeal = meal
                    addingFromFab = false
                },
                onEdit = { editing = it },
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = Spacing.page, end = Spacing.page, bottom = 96.dp),
        )

        ExtendedFloatingActionButton(
            onClick = {
                addingMeal = MealTiming.defaultMeal(LocalTime.now())
                addingFromFab = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.xl),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("记一笔", style = MaterialTheme.typography.labelLarge) },
        )
    }

    addingMeal?.let { meal ->
        AddEntrySheet(
            initialMeal = meal,
            showMealPicker = addingFromFab,
            onDismiss = { addingMeal = null },
            onAdd = { food, grams, chosen ->
                vm.addEntry(food, grams, chosen)
                addingMeal = null
                offerUndo("已记 ${food.name}", "撤销") { vm.undoAdd() }
            },
        )
    }

    editing?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { grams ->
                vm.updateGrams(entry.id, grams)
                editing = null
            },
            onDelete = {
                vm.deleteEntry(entry.id)
                editing = null
                offerUndo("已删除 ${entry.foodName}", "撤销") { vm.undoDelete() }
            },
        )
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    editable: Boolean,
    onShift: (Long) -> Unit,
    onToday: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (isToday) "今天" else date.labelShort(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = buildString {
                    append(date.labelFull())
                    if (!editable) append(" · 仅可查看")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前一天")
        }
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "后一天")
        }
        if (!isToday) {
            AppButton(
                text = "回到今天",
                onClick = onToday,
                style = AppButtonStyle.Text,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(state: TodayUiState) {
    val over = state.remainingKcal < 0
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "今日摄入",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${state.totalKcal.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    text = "目标 ${state.goalKcal} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (over) "已超出" else "还剩",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${abs(state.remainingKcal).toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        ProgressBar(progress = state.progress, over = over)
        Spacer(Modifier.height(Spacing.md))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            StatusPill(state.status)
            StreakBadge(state.streak)
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, over: Boolean) {
    val active = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(active),
        )
    }
}

@Composable
private fun StatusPill(status: DayStatus) {
    val (bg, fg, label) = when (status) {
        DayStatus.COMPLETE -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "完整打卡",
        )
        DayStatus.PARTIAL -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "部分打卡",
        )
        DayStatus.MISSING -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "未记录",
        )
    }
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(fg, CircleShape))
            Spacer(Modifier.width(Spacing.xs + 2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (streak > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "连续 $streak 天",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MealsCard(
    state: TodayUiState,
    onAdd: (MealType) -> Unit,
    onEdit: (EntryView) -> Unit,
) {
    SurfaceCard(padding = 0.dp) {
        MealType.entries.forEachIndexed { index, meal ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MealBlock(
                meal = meal,
                entries = state.entriesByMeal[meal].orEmpty(),
                editable = state.editable,
                onAdd = { onAdd(meal) },
                onEdit = onEdit,
            )
        }
    }
}

@Composable
private fun MealBlock(
    meal: MealType,
    entries: List<EntryView>,
    editable: Boolean,
    onAdd: () -> Unit,
    onEdit: (EntryView) -> Unit,
) {
    val subtotal = entries.sumOf { it.kcal }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = editable, onClick = onAdd)
                .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.md, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(meal.label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = when {
                    subtotal > 0 -> "${subtotal.toInt()} kcal"
                    editable -> "未记录 · 点这里记一笔"
                    else -> "还没有条目"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd, enabled = editable) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加${meal.label}",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (entries.isNotEmpty()) {
            entries.forEach { entry ->
                ListRow(
                    title = entry.foodName,
                    subtitle = Portion.describe(entry.grams, entry.unitLabel, entry.unitGrams),
                    onClick = if (editable) ({ onEdit(entry) }) else null,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${entry.kcal.toInt()} kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (editable) {
                                Spacer(Modifier.width(Spacing.xs))
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AddEntrySheet(
    initialMeal: MealType,
    showMealPicker: Boolean,
    onDismiss: () -> Unit,
    onAdd: (FoodEntity, Double, MealType) -> Unit,
) {
    val picker: FoodPickerViewModel = appViewModel { FoodPickerViewModel(it.repository) }
    var meal by remember { mutableStateOf(initialMeal) }
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    var amount by remember { mutableStateOf("") }
    var portionMode by remember { mutableStateOf(false) }
    var customDialog by remember { mutableStateOf(false) }

    val query by picker.query.collectAsStateWithLifecycle()
    val category by picker.category.collectAsStateWithLifecycle()
    val categories by picker.categories.collectAsStateWithLifecycle()
    val recent by picker.recent.collectAsStateWithLifecycle()
    val foods by picker.foods.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val choose: (FoodEntity) -> Unit = { food ->
        selected = food
        val unit = food.unitGrams
        if (food.unitLabel != null && unit != null && unit > 0) {
            portionMode = true
            amount = "1"
        } else {
            portionMode = false
            amount = (unit ?: 100.0).trimZero()
        }
    }

    DisposableEffect(Unit) {
        onDispose { picker.clearQuery() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("添加食物", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                AppButton(text = "自定义", onClick = { customDialog = true }, style = AppButtonStyle.Text)
            }
            if (showMealPicker) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MealType.entries.forEach { option ->
                        AppChip(
                            text = option.label,
                            selected = option == meal,
                            onClick = { meal = option },
                        )
                    }
                }
            } else {
                Text(
                    text = "记到 ${meal.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = query,
                onValueChange = { picker.search(it) },
                label = { Text("搜索食物") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppChip(
                    text = "全部",
                    selected = category == null,
                    onClick = { picker.selectCategory(null) },
                )
                categories.forEach { item ->
                    AppChip(
                        text = item,
                        selected = category == item,
                        onClick = { picker.selectCategory(item) },
                    )
                }
            }
            if (query.isBlank() && category == null && recent.isNotEmpty()) {
                Text(
                    text = "最近记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    recent.forEach { item ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(Spacing.border, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.combinedClickable(
                                onClick = { onAdd(item.food, item.grams, meal) },
                                onLongClick = { choose(item.food) },
                            ),
                        ) {
                            Text(
                                text = item.food.name,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f, fill = false).height(300.dp)) {
                items(foods, key = { it.id }) { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { choose(food) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${food.kcalPer100g.toInt()} kcal/100g · ${food.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected?.id == food.id) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            selected?.let { food ->
                val unitLabel = food.unitLabel
                val unitGrams = food.unitGrams
                val hasUnit = unitLabel != null && unitGrams != null && unitGrams > 0
                val gramsNow = amount.toDoubleOrNull()?.let { value ->
                    if (portionMode && hasUnit) Portion.gramsOf(value, unitGrams!!) else value
                }

                if (hasUnit) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !portionMode,
                            onClick = {
                                val value = amount.toDoubleOrNull()
                                if (portionMode && value != null && value > 0) {
                                    amount = Portion.label(Portion.gramsOf(value, unitGrams!!))
                                }
                                portionMode = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {},
                        ) { Text("克") }
                        SegmentedButton(
                            selected = portionMode,
                            onClick = {
                                val value = amount.toDoubleOrNull()
                                if (!portionMode && value != null && value > 0) {
                                    amount = Portion.label(Portion.portionOf(value, unitGrams!!))
                                }
                                portionMode = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {},
                        ) { Text(unitLabel!!) }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val inPortion = portionMode && hasUnit
                    AmountInput(
                        amount = amount,
                        onAmountChange = { amount = it },
                        label = if (inPortion) "数量" else "克数",
                        step = if (inPortion) 0.5 else 10.0,
                        longStep = if (inPortion) 5.0 else 50.0,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    AppButton(
                        text = "添加",
                        enabled = (gramsNow ?: 0.0) > 0.0,
                        onClick = { onAdd(food, gramsNow ?: 0.0, meal) },
                    )
                }
                Text(
                    text = "约 ${(food.kcalPer100g * (gramsNow ?: 0.0) / 100).toInt()} kcal" +
                        if (portionMode && hasUnit) " · ${Portion.label(gramsNow ?: 0.0)} g" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }

    if (customDialog) {
        CustomFoodDialog(
            onDismiss = { customDialog = false },
            onConfirm = { name, kcal, category, unitLabel, unitGrams ->
                picker.addCustom(name, kcal, category, unitLabel, unitGrams)
                customDialog = false
            },
        )
    }
}

@Composable
fun CustomFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String?, Double?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    var unitLabel by remember { mutableStateOf("") }
    var unitGrams by remember { mutableStateOf("") }

    val kcalValue = kcal.toDoubleOrNull()
    val gramsValue = unitGrams.toDoubleOrNull()
    val unitPairOk = (unitLabel.isBlank() && unitGrams.isBlank()) ||
        (unitLabel.isNotBlank() && gramsValue != null && gramsValue > 0)
    val canSave = name.isNotBlank() && kcalValue != null && kcalValue > 0 && unitPairOk

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义食物") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("每 100g 热量 (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分类") }, singleLine = true)
                Text(
                    text = "常用份量（可留空，留空就只能填克数）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text("单位，如 碗") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = unitGrams,
                        onValueChange = { unitGrams = it },
                        label = { Text("每份克数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!unitPairOk) {
                    Text(
                        text = "单位和每份克数要么都填，要么都留空",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    if (!canSave) return@TextButton
                    onConfirm(
                        name.trim(),
                        kcalValue!!,
                        category.trim().ifBlank { "自定义" },
                        unitLabel.trim().ifBlank { null },
                        gramsValue,
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EditEntryDialog(
    entry: EntryView,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val unitLabel = entry.unitLabel
    val unitGrams = entry.unitGrams
    val hasUnit = unitLabel != null && unitGrams != null && unitGrams > 0
    var portionMode by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(entry.grams.trimZero()) }
    val gramsNow = amount.toDoubleOrNull()?.let { value ->
        if (portionMode && hasUnit) Portion.gramsOf(value, unitGrams!!) else value
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.foodName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("${entry.meal.label} · 原记录 ${entry.kcal.toInt()} kcal")
                if (hasUnit) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !portionMode,
                            onClick = {
                                val value = amount.toDoubleOrNull()
                                if (portionMode && value != null && value > 0) {
                                    amount = Portion.label(Portion.gramsOf(value, unitGrams!!))
                                }
                                portionMode = false
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {},
                        ) { Text("克") }
                        SegmentedButton(
                            selected = portionMode,
                            onClick = {
                                val value = amount.toDoubleOrNull()
                                if (!portionMode && value != null && value > 0) {
                                    amount = Portion.label(Portion.portionOf(value, unitGrams!!))
                                }
                                portionMode = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {},
                        ) { Text(unitLabel!!) }
                    }
                }
                val inPortion = portionMode && hasUnit
                AmountInput(
                    amount = amount,
                    onAmountChange = { amount = it },
                    label = if (inPortion) "数量" else "克数",
                    step = if (inPortion) 0.5 else 10.0,
                    longStep = if (inPortion) 5.0 else 50.0,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "现在约 ${(entry.kcalPer100g * (gramsNow ?: 0.0) / 100).toInt()} kcal" +
                        if (portionMode && hasUnit) " · ${Portion.label(gramsNow ?: 0.0)} g" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { gramsNow?.let { if (it > 0) onSave(it) } }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除")
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

fun Double.trimZero(): String = Portion.label(this)

/** 数量输入 + 加减台阶：点一下 ±10g / ±0.5 份，长按 ±50g / ±5 份。 */
@Composable
private fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    label: String,
    step: Double,
    longStep: Double,
    modifier: Modifier = Modifier,
) {
    fun bump(delta: Double) {
        val current = amount.toDoubleOrNull() ?: 0.0
        onAmountChange(Portion.label((current + delta).coerceAtLeast(0.0)))
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            icon = Icons.Filled.Remove,
            description = "减少",
            onClick = { bump(-step) },
            onLongClick = { bump(-longStep) },
        )
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        StepButton(
            icon = Icons.Filled.Add,
            description = "增加",
            onClick = { bump(step) },
            onLongClick = { bump(longStep) },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
    }
}
