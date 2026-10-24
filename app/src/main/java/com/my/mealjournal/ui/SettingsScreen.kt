package com.my.mealjournal.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.data.SettingsStore
import com.my.mealjournal.ui.design.AppButton
import com.my.mealjournal.ui.design.AppButtonStyle
import com.my.mealjournal.ui.design.AppChip
import com.my.mealjournal.ui.design.AppTextField
import com.my.mealjournal.ui.design.ListRow
import com.my.mealjournal.ui.design.SectionHeader
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var goalText by remember(state.goalKcal) { mutableStateOf(state.goalKcal.toString()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) vm.setReminderEnabled(true)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csv = withContext(Dispatchers.IO) { vm.buildCsv() }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                }.isSuccess
            }
            exportMessage = if (ok) "已导出 ${csv.lines().size - 1} 条记录" else "导出失败，请重试"
        }
    }

    val reminderToggle: (Boolean) -> Unit = { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setReminderEnabled(enabled)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page)
            .padding(top = Spacing.md, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader("每日目标热量", subtitle = "点档位直接生效，或填自定义值保存")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SettingsStore.GOAL_PRESETS.forEach { preset ->
                        AppChip(
                            text = "$preset",
                            selected = preset == state.goalKcal,
                            onClick = { vm.setGoal(preset) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { ch -> ch.isDigit() } },
                        label = "自定义 kcal",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    AppButton(
                        text = "保存",
                        style = AppButtonStyle.Secondary,
                        enabled = goalText.toIntOrNull()?.let { it in 600..6000 } == true,
                        onClick = { goalText.toIntOrNull()?.let { if (it in 600..6000) vm.setGoal(it) } },
                    )
                }
                Text(
                    text = "自定义范围 600–6000 kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SectionHeader("每日打卡提醒", subtitle = "到点还没记满三餐就提醒")
                ListRow(
                    title = "开启提醒",
                    onClick = { reminderToggle(!state.reminderEnabled) },
                    trailing = {
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = reminderToggle,
                        )
                    },
                )
                ListRow(
                    title = "提醒时间",
                    onClick = {
                        val hour = state.reminderMinute / 60
                        val minute = state.reminderMinute % 60
                        TimePickerDialog(
                            context,
                            { _, h, m -> vm.setReminderMinute(h * 60 + m) },
                            hour,
                            minute,
                            true,
                        ).show()
                    },
                    trailing = {
                        Text(
                            text = state.reminderMinute.minuteText(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader("导出数据", subtitle = "导出全部记录为 CSV，Excel / 数字手账都能直接打开。")
                AppButton(
                    text = "导出 CSV",
                    onClick = { exportLauncher.launch("三餐记-${LocalDate.now()}.csv") },
                )
                exportMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text(
            "三餐记 v0.1.0 · 数据只存在本机，不联网、不上传。热量为参考值。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
