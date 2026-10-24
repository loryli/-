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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.my.mealjournal.ui.design.AppTextField
import com.my.mealjournal.ui.design.CalorieRing
import com.my.mealjournal.ui.design.EmptyState
import com.my.mealjournal.ui.design.ListRow
import com.my.mealjournal.ui.design.SectionHeader
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import com.my.mealjournal.ui.design.statusStyle
import com.my.mealjournal.ui.theme.nutrientColors
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

            state.entriesByMeal.forEach { (meal, entries) ->
                MealCard(
                    meal = meal,
                    entries = entries,
                    editable = state.editable,
                    onAdd = {
                        addingMeal = meal
                        addingFromFab = false
                    },
                    onEdit = { editing = it },
                )
            }
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
    val nutrients = nutrientColors()
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CalorieRing(
                progress = state.progress,
                over = over,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (over) "已超出" else "还可摄入",
                        style = MaterialTheme.typography.labelMedium,
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
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "已摄入 ${state.totalKcal.toInt()} / ${state.goalKcal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                NutrientBar("碳水", state.todayMacro.carbs, state.macroTargets.carbs, nutrients.carbs)
                NutrientBar("蛋白质", state.todayMacro.protein, state.macroTargets.protein, nutrients.protein)
                NutrientBar("脂肪", state.todayMacro.fat, state.macroTargets.fat, nutrients.fat)
            }
        }

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

/** 三色营养素条：名称 + 摄入/目标克数 + 彩色进度。 */
@Composable
private fun NutrientBar(label: String, value: Double, target: Double, color: Color) {
    val progress = if (target <= 0.0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${value.toInt()}/${target.toInt()} g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun StatusPill(status: DayStatus) {
    val style = status.statusStyle()
    Surface(shape = CircleShape, color = style.container, contentColor = style.content) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(style.content, CircleShape))
            Spacer(Modifier.width(Spacing.xs + 2.dp))
            Text(style.label, style = MaterialTheme.typography.labelSmall)
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

/** 一餐一张卡：头行可整行点击添加，条目行点击进编辑。 */
@Composable
private fun MealCard(
    meal: MealType,
    entries: List<EntryView>,
    editable: Boolean,
    onAdd: () -> Unit,
    onEdit: (EntryView) -> Unit,
) {
    SurfaceCard(padding = 0.dp) {
        MealBlock(
            meal = meal,
            entries = entries,
            editable = editable,
            onAdd = onAdd,
            onEdit = onEdit,
        )
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
    LaunchedEffect(Unit) { picker.refreshRecent() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .imePadding(),
        ) {
            // 第一层：标题 + 餐次（从餐次卡片进入时餐次收进副标题，不再单独占层）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("添加食物", style = MaterialTheme.typography.titleLarge)
                    if (!showMealPicker) {
                        Text(
                            text = "记到 ${meal.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                AppButton(text = "自定义", onClick = { customDialog = true }, style = AppButtonStyle.Text)
            }
            if (showMealPicker) {
                Row(
                    modifier = Modifier.padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    MealType.entries.forEach { option ->
                        AppChip(
                            text = option.label,
                            selected = option == meal,
                            onClick = { meal = option },
                        )
                    }
                }
            }

            // 第二层：搜索 + 分类
            Spacer(Modifier.height(Spacing.sm))
            AppTextField(
                value = query,
                onValueChange = { picker.search(it) },
                label = "搜索食物",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
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

            // 第三层：结果 = 最近记录捷径 + 食物列表
            val showRecent = query.isBlank() && category == null && recent.isNotEmpty()
            if (showRecent) {
                Text(
                    text = "最近记录 · 点一下按上次份量记",
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
            LazyColumn(Modifier.weight(1f, fill = false)) {
                if (foods.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(
                            title = if (query.isBlank()) "这个分类下还没有食物" else "没有找到「$query」",
                            hint = "换个关键词或分类，或点右上角「自定义」新建",
                        )
                    }
                }
                items(foods, key = { it.id }) { food ->
                    ListRow(
                        title = food.name,
                        subtitle = "${food.kcalPer100g.toInt()} kcal/100g · ${food.category}",
                        onClick = { choose(food) },
                        trailing = {
                            if (selected?.id == food.id) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }
            }

            // 选中食物后的份量录入区，浮在结果区下方
            selected?.let { food ->
                val gramsNow = amount.toDoubleOrNull()?.let { value ->
                    if (portionMode && food.unitLabel != null && (food.unitGrams ?: 0.0) > 0) {
                        Portion.gramsOf(value, food.unitGrams!!)
                    } else {
                        value
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                PortionEditor(
                    kcalPer100g = food.kcalPer100g,
                    unitLabel = food.unitLabel,
                    unitGrams = food.unitGrams,
                    amount = amount,
                    onAmountChange = { amount = it },
                    portionMode = portionMode,
                    onPortionModeChange = { portionMode = it },
                    trailing = {
                        AppButton(
                            text = "添加",
                            enabled = (gramsNow ?: 0.0) > 0.0,
                            onClick = { onAdd(food, gramsNow ?: 0.0, meal) },
                        )
                    },
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }

    if (customDialog) {
        CustomFoodDialog(
            onDismiss = { customDialog = false },
            onConfirm = { name, kcal, category, unitLabel, unitGrams, protein, carbs, fat ->
                picker.addCustom(name, kcal, category, unitLabel, unitGrams, protein, carbs, fat)
                customDialog = false
            },
        )
    }
}

@Composable
fun CustomFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String, kcal: Double, category: String,
        unitLabel: String?, unitGrams: Double?,
        protein: Double?, carbs: Double?, fat: Double?,
    ) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    var unitLabel by remember { mutableStateOf("") }
    var unitGrams by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val kcalValue = kcal.toDoubleOrNull()
    val gramsValue = unitGrams.toDoubleOrNull()
    val unitPairOk = (unitLabel.isBlank() && unitGrams.isBlank()) ||
        (unitLabel.isNotBlank() && gramsValue != null && gramsValue > 0)
    val macroOk = listOf(protein, carbs, fat).all { it.isBlank() || (it.toDoubleOrNull()?.let { v -> v >= 0 } == true) }
    val proteinValue = protein.toDoubleOrNull()
    val carbsValue = carbs.toDoubleOrNull()
    val fatValue = fat.toDoubleOrNull()
    val canSave = name.isNotBlank() && kcalValue != null && kcalValue > 0 && unitPairOk && macroOk

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义食物") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppTextField(value = name, onValueChange = { name = it }, label = "名称")
                AppTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = "每 100g 热量 (kcal)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                AppTextField(value = category, onValueChange = { category = it }, label = "分类")
                Text(
                    text = "常用份量（可留空，留空就只能填克数）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    AppTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = "单位，如 碗",
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = unitGrams,
                        onValueChange = { unitGrams = it },
                        label = "每份克数",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "营养素 g/100g（可留空）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AppTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = "蛋白质",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = "碳水",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = "脂肪",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            AppButton(
                text = "保存",
                style = AppButtonStyle.Text,
                enabled = canSave,
                onClick = {
                    if (!canSave) return@AppButton
                    onConfirm(
                        name.trim(),
                        kcalValue!!,
                        category.trim().ifBlank { "自定义" },
                        unitLabel.trim().ifBlank { null },
                        gramsValue,
                        proteinValue,
                        carbsValue,
                        fatValue,
                    )
                },
            )
        },
        dismissButton = {
            AppButton(text = "取消", onClick = onDismiss, style = AppButtonStyle.Text)
        },
    )
}

/** 克/份切换 + 数量步进输入 + 热量预览；添加面板与编辑弹窗共用。trailing 挂在输入行右侧。 */
@Composable
private fun PortionEditor(
    kcalPer100g: Double,
    unitLabel: String?,
    unitGrams: Double?,
    amount: String,
    onAmountChange: (String) -> Unit,
    portionMode: Boolean,
    onPortionModeChange: (Boolean) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val hasUnit = unitLabel != null && unitGrams != null && unitGrams > 0

    if (hasUnit) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !portionMode,
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (portionMode && value != null && value > 0) {
                        onAmountChange(Portion.label(Portion.gramsOf(value, unitGrams!!)))
                    }
                    onPortionModeChange(false)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {},
            ) { Text("克") }
            SegmentedButton(
                selected = portionMode,
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (!portionMode && value != null && value > 0) {
                        onAmountChange(Portion.label(Portion.portionOf(value, unitGrams!!)))
                    }
                    onPortionModeChange(true)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
            ) { Text(unitLabel!!) }
        }
        Spacer(Modifier.height(Spacing.sm))
    }

    val inPortion = portionMode && hasUnit
    val gramsNow = amount.toDoubleOrNull()?.let { value ->
        if (inPortion) Portion.gramsOf(value, unitGrams!!) else value
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AmountInput(
            amount = amount,
            onAmountChange = onAmountChange,
            label = if (inPortion) "数量" else "克数",
            step = if (inPortion) 0.5 else 10.0,
            longStep = if (inPortion) 5.0 else 50.0,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.md))
            trailing()
        }
    }
    Text(
        text = "约 ${(kcalPer100g * (gramsNow ?: 0.0) / 100).toInt()} kcal" +
            if (inPortion) " · ${Portion.label(gramsNow ?: 0.0)} g" else "",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                PortionEditor(
                    kcalPer100g = entry.kcalPer100g,
                    unitLabel = unitLabel,
                    unitGrams = unitGrams,
                    amount = amount,
                    onAmountChange = { amount = it },
                    portionMode = portionMode,
                    onPortionModeChange = { portionMode = it },
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "保存",
                style = AppButtonStyle.Text,
                enabled = (gramsNow ?: 0.0) > 0.0,
                onClick = { gramsNow?.let { if (it > 0) onSave(it) } },
            )
        },
        dismissButton = {
            Row {
                AppButton(text = "删除", onClick = onDelete, style = AppButtonStyle.Text, destructive = true)
                AppButton(text = "取消", onClick = onDismiss, style = AppButtonStyle.Text)
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
        AppTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = label,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
