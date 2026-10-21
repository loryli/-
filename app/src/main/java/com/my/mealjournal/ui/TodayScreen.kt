package com.my.mealjournal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.data.EntryView
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.domain.DayStatus
import com.my.mealjournal.domain.MealTiming
import com.my.mealjournal.domain.MealType
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(vm: TodayViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var addingMeal by remember { mutableStateOf<MealType?>(null) }
    var editing by remember { mutableStateOf<EntryView?>(null) }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DateHeader(
            date = state.date,
            editable = state.editable,
            onShift = { days -> vm.selectDate(state.date.plusDays(days)) },
            onToday = { vm.selectDate(LocalDate.now()) },
        )

        ProgressCard(state)

        MealType.entries.forEach { meal ->
            MealSection(
                meal = meal,
                entries = state.entriesByMeal[meal].orEmpty(),
                editable = state.editable,
                onAdd = { addingMeal = meal },
                onEdit = { editing = it },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

        ExtendedFloatingActionButton(
            onClick = { addingMeal = MealTiming.defaultMeal(LocalTime.now()) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("记一笔")
        }
    }

    addingMeal?.let { meal ->
        AddEntrySheet(
            initialMeal = meal,
            onDismiss = { addingMeal = null },
            onAdd = { food, grams, chosen ->
                vm.addEntry(food, grams, chosen)
                addingMeal = null
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (date == LocalDate.now()) "今天" else date.labelFull(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (editable) date.labelFull() else "已超出 7 天补记窗口，仅可查看",
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
        TextButton(onClick = onToday) { Text("今天") }
    }
}

@Composable
private fun ProgressCard(state: TodayUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalorieRing(progress = state.progress, over = state.totalKcal > state.goalKcal)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("今日摄入", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${state.totalKcal.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "目标 ${state.goalKcal} kcal · " +
                        if (state.remainingKcal >= 0) "还剩 ${state.remainingKcal.toInt()}" else "超出 ${(-state.remainingKcal).toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(state.status)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "今日打卡：${state.status.label}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "连续 ${state.streak} 天",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: DayStatus) {
    val color = when (status) {
        DayStatus.COMPLETE -> MaterialTheme.colorScheme.primary
        DayStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
        DayStatus.MISSING -> MaterialTheme.colorScheme.outline
    }
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun CalorieRing(progress: Float, over: Boolean) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val active = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(104.dp)) {
        val stroke = 12.dp.toPx()
        val inset = stroke / 2
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = active,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun MealSection(
    meal: MealType,
    entries: List<EntryView>,
    editable: Boolean,
    onAdd: () -> Unit,
    onEdit: (EntryView) -> Unit,
) {
    val subtotal = entries.sumOf { it.kcal }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (subtotal > 0) "${subtotal.toInt()} kcal" else "未记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd, enabled = editable) {
                    Icon(Icons.Filled.Add, contentDescription = "添加${meal.label}")
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = "还没有条目",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            } else {
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = editable) { onEdit(entry) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.foodName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${entry.grams.trimZero()} g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${entry.kcal.toInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                        if (editable) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.outline,
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
private fun AddEntrySheet(
    initialMeal: MealType,
    onDismiss: () -> Unit,
    onAdd: (FoodEntity, Double, MealType) -> Unit,
) {
    val picker: FoodPickerViewModel = appViewModel { FoodPickerViewModel(it.repository) }
    var query by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(initialMeal) }
    var selected by remember { mutableStateOf<FoodEntity?>(null) }
    var grams by remember { mutableStateOf("") }
    var customDialog by remember { mutableStateOf(false) }

    val foods by picker.foods.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("添加食物", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { customDialog = true }) { Text("自定义") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { option ->
                    Surface(
                        shape = CircleShape,
                        color = if (option == meal) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { meal = option },
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (option == meal) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    picker.search(it)
                },
                label = { Text("搜索食物") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(Modifier.weight(1f, fill = false).height(360.dp)) {
                items(foods, key = { it.id }) { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = food
                                grams = (food.unitGrams ?: 100.0).trimZero()
                            }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = grams,
                        onValueChange = { grams = it },
                        label = { Text("克数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = {
                            val g = grams.toDoubleOrNull() ?: return@FilledTonalButton
                            if (g > 0) onAdd(food, g, meal)
                        },
                    ) { Text("添加") }
                }
                Text(
                    text = "约 ${(food.kcalPer100g * (grams.toDoubleOrNull() ?: 0.0) / 100).toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (customDialog) {
        CustomFoodDialog(
            onDismiss = { customDialog = false },
            onConfirm = { name, kcal, category ->
                picker.addCustom(name, kcal, category)
                customDialog = false
            },
        )
    }
}

@Composable
fun CustomFoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("自定义") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义食物") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text("每 100g 热量 (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分类") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = kcal.toDoubleOrNull() ?: return@TextButton
                    if (name.isNotBlank() && value > 0) onConfirm(name.trim(), value, category.trim().ifBlank { "自定义" })
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
    var grams by remember { mutableStateOf(entry.grams.trimZero()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.foodName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${entry.meal.label} · 原记录 ${entry.kcal.toInt()} kcal")
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("克数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { grams.toDoubleOrNull()?.let { if (it > 0) onSave(it) } }) { Text("保存") }
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

fun Double.trimZero(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
