package com.my.mealjournal.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class AppButtonStyle { Primary, Secondary, Text }

/**
 * 统一按钮：主操作实心绿，次操作描边，文字操作用主色。
 * 最小高度 48dp，字号随系统缩放。
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val size = Modifier.heightIn(min = Spacing.buttonHeight)
    val pad = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm)
    when (style) {
        AppButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = modifier.then(size),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = pad,
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }

        AppButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.then(size),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(Spacing.border, MaterialTheme.colorScheme.outlineVariant),
            contentPadding = pad,
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }

        AppButtonStyle.Text -> TextButton(
            onClick = onClick,
            modifier = modifier.then(size),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = pad,
        ) { Text(text, style = MaterialTheme.typography.labelLarge) }
    }
}
