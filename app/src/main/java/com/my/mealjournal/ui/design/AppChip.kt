package com.my.mealjournal.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 选择型胶囊：选中用浅绿底 + 深绿字，不用实心主色。
 * 过滤、档位、单位切换都用它，保证选中态强度一致。
 */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null
        else BorderStroke(Spacing.border, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
