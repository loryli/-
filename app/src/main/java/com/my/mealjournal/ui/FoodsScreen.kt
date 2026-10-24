package com.my.mealjournal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.ui.design.AppChip
import com.my.mealjournal.ui.design.AppTextField
import com.my.mealjournal.ui.design.EmptyState
import com.my.mealjournal.ui.design.ListRow
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import com.my.mealjournal.ui.theme.nutrientColors
import kotlinx.coroutines.launch

@Composable
fun FoodsScreen(vm: FoodsViewModel) {
    val foods by vm.foods.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<FoodEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.md, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("食物库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "内置 ${summary.first} 种参考值 · 自定义 ${summary.second} 种",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AppTextField(
                value = query,
                onValueChange = { vm.search(it) },
                label = "搜索食物",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AppChip(text = "全部", selected = category == null, onClick = { vm.selectCategory(null) })
                categories.forEach { item ->
                    AppChip(text = item, selected = category == item, onClick = { vm.selectCategory(item) })
                }
            }

            if (foods.isEmpty()) {
                EmptyState(
                    title = if (query.isBlank()) "这个分类下还没有食物" else "没有找到「$query」",
                    hint = "换个关键词或分类，或点右下角新建自定义食物",
                )
            } else {
                SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    Column {
                        foods.forEachIndexed { index, food ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            FoodLibraryRow(
                                food = food,
                                onClick = { detail = food },
                                onDelete = if (!food.isBuiltin) {
                                    {
                                        vm.delete(food)
                                        scope.launch {
                                            val result = snackbar.showSnackbar(
                                                message = "已删除 ${food.name}",
                                                actionLabel = "撤销",
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = Spacing.page, end = Spacing.page, bottom = 96.dp),
        )

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.xl),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("新建食物", style = MaterialTheme.typography.labelLarge) },
        )
    }

    if (showAdd) {
        CustomFoodDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, kcal, category, unitLabel, unitGrams, protein, carbs, fat ->
                vm.add(name, kcal, category, unitLabel, unitGrams, protein, carbs, fat)
                showAdd = false
            },
        )
    }

    detail?.let { food ->
        FoodDetailDialog(food = food, onDismiss = { detail = null })
    }
}

/** 库里的一行：内置食物右侧是三色营养速览，自建食物右侧是删除。 */
@Composable
private fun FoodLibraryRow(
    food: FoodEntity,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val unit = food.unitLabel
    val unitGrams = food.unitGrams
    ListRow(
        title = food.name + if (food.isBuiltin) "" else " · 自定义",
        subtitle = buildString {
            append("${food.kcalPer100g.toInt()} kcal/100g · ${food.category}")
            if (unit != null && unitGrams != null && unitGrams > 0) {
                append(" · 1 $unit = ${unitGrams.trimZero()} g")
            }
        },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = Spacing.lg),
        trailing = {
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                MacroSummary(food)
            }
        },
    )
}

/** 蛋白/碳水/脂肪三色速览（每 100g 克数），未填的营养素不显示。 */
@Composable
private fun MacroSummary(food: FoodEntity) {
    val nutrients = nutrientColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "蛋白 ${food.proteinPer100g?.let { it.toInt().toString() } ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = nutrients.protein,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "碳 ${food.carbsPer100g?.let { it.toInt().toString() } ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = nutrients.carbs,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "脂 ${food.fatPer100g?.let { it.toInt().toString() } ?: "—"}",
            style = MaterialTheme.typography.labelSmall,
            color = nutrients.fat,
        )
    }
}

/** 食物详情：热量、三色营养条、常用份量。 */
@Composable
private fun FoodDetailDialog(food: FoodEntity, onDismiss: () -> Unit) {
    val nutrients = nutrientColors()
    val max = maxOf(food.proteinPer100g ?: 0.0, food.carbsPer100g ?: 0.0, food.fatPer100g ?: 0.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "${food.kcalPer100g.toInt()} kcal/100g · ${food.category} · 参考值",
                    style = MaterialTheme.typography.bodyMedium,
                )
                food.unitLabel?.takeIf { (food.unitGrams ?: 0.0) > 0 }?.let { unit ->
                    Text(
                        text = "常用份量：1 $unit ≈ ${food.unitGrams!!.trimZero()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                listOf(
                    Triple("蛋白质", food.proteinPer100g, nutrients.protein),
                    Triple("碳水", food.carbsPer100g, nutrients.carbs),
                    Triple("脂肪", food.fatPer100g, nutrients.fat),
                ).forEach { (label, value, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.width(52.dp), style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { if (max <= 0.0) 0f else ((value ?: 0.0) / max).toFloat() },
                            modifier = Modifier.weight(1f).height(8.dp),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = value?.let { "${it.trimZero()} g" } ?: "—",
                            modifier = Modifier.width(56.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            com.my.mealjournal.ui.design.AppButton(text = "关闭", onClick = onDismiss, style = com.my.mealjournal.ui.design.AppButtonStyle.Text)
        },
    )
}
