package com.my.mealjournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.my.mealjournal.ui.design.EmptyState
import com.my.mealjournal.ui.design.ListRow
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import kotlinx.coroutines.launch

@Composable
fun FoodsScreen(vm: FoodsViewModel) {
    val foods by vm.foods.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
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
            Text("我的食物", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = "内置约 150 种常见食物，这里只列你自己加的。热量都按每 100g 记。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (foods.isEmpty()) {
                EmptyState(
                    title = "还没有自定义食物",
                    hint = "点右下角新建一个，比如「楼下那家牛肉面」",
                )
            } else {
                SurfaceCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    Column {
                        foods.forEachIndexed { index, food ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ListRow(
                                title = food.name,
                                subtitle = buildString {
                                    append("${food.kcalPer100g.toInt()} kcal/100g · ${food.category}")
                                    val unit = food.unitLabel
                                    val unitGrams = food.unitGrams
                                    if (unit != null && unitGrams != null && unitGrams > 0) {
                                        append(" · 1 $unit = ${unitGrams.trimZero()} g")
                                    }
                                },
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                                trailing = {
                                    IconButton(onClick = {
                                        vm.delete(food)
                                        offerUndo("已删除 ${food.name}", "撤销") { vm.undoDelete() }
                                    }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.outline,
                                        )
                                    }
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
}
