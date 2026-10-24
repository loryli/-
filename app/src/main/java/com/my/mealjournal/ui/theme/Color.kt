package com.my.mealjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * 品牌色板（Phase 4 薄荷风）：清新薄荷绿主色 + 纯白大圆角卡片 + 冷白底。
 * 规范见 docs/ui-redesign.md §10；除本文件外不要在页面里写死颜色。
 * 三大营养素色不走 ColorScheme（M3 没有对应槽位），见下方 NutrientColors。
 */

// 中性（浅色）
internal val Paper = Color(0xFFF5F7F6)
internal val SurfaceLight = Color(0xFFFFFFFF)
internal val MutedLight = Color(0xFFECF1EE)
internal val LineLight = Color(0xFFE3E9E5)
internal val Ink = Color(0xFF1C2420)
internal val InkSecondary = Color(0xFF5F6E66)
internal val InkTertiary = Color(0xFF8A9790)

// 品牌绿与语义色（浅色）
internal val Mint = Color(0xFF31B672)
internal val MintDeep = Color(0xFF1F9C5B)
internal val MintSoft = Color(0xFFDDF5E7)
internal val MintInk = Color(0xFF0D4E2C)
internal val Tangerine = Color(0xFFE8A23D)
internal val TangerineSoft = Color(0xFFFBEED8)
internal val TangerineInk = Color(0xFF5C3D10)
internal val Danger = Color(0xFFE05D4B)
internal val DangerSoft = Color(0xFFFBE4DF)
internal val DangerInk = Color(0xFF5A1B12)

// 中性（深色）
internal val DarkPaper = Color(0xFF0E1512)
internal val DarkSurface = Color(0xFF18201B)
internal val DarkMuted = Color(0xFF202A24)
internal val DarkLine = Color(0xFF2A362E)
internal val DarkInk = Color(0xFFE4EDE7)
internal val DarkInkSecondary = Color(0xFF9BAFA4)
internal val DarkInkTertiary = Color(0xFF6F7F76)

// 品牌绿与语义色（深色）
internal val DarkMint = Color(0xFF6FD598)
internal val DarkMintDeep = Color(0xFF57BE86)
internal val DarkMintSoft = Color(0xFF1D4433)
internal val DarkMintInk = Color(0xFFC2EED4)
internal val DarkTangerine = Color(0xFFEDB566)
internal val DarkTangerineSoft = Color(0xFF46351A)
internal val DarkTangerineInk = Color(0xFFF4DFB9)
internal val DarkDanger = Color(0xFFE9907F)
internal val DarkDangerSoft = Color(0xFF4A221B)

// 三大营养素固定色：碳水蓝 / 蛋白紫 / 脂肪橙，与品牌绿区分开
data class NutrientPalette(val carbs: Color, val protein: Color, val fat: Color)

private val LightNutrients = NutrientPalette(
    carbs = Color(0xFF4D8DE8),
    protein = Color(0xFF9B7BD8),
    fat = Color(0xFFF2A04E),
)

private val DarkNutrients = NutrientPalette(
    carbs = Color(0xFF7FB0F2),
    protein = Color(0xFFBCA3E8),
    fat = Color(0xFFF6BE7E),
)

@androidx.compose.runtime.Composable
fun nutrientColors(): NutrientPalette = if (isSystemInDarkTheme()) DarkNutrients else LightNutrients

internal val LightColors = lightColorScheme(
    primary = Mint,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = MintInk,
    secondary = MintDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2E6),
    onSecondaryContainer = MintInk,
    tertiary = Tangerine,
    onTertiary = Color.White,
    tertiaryContainer = TangerineSoft,
    onTertiaryContainer = TangerineInk,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = MutedLight,
    onSurfaceVariant = InkSecondary,
    surfaceTint = Mint,
    surfaceDim = Color(0xFFDEE4E1),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFCFB),
    surfaceContainer = Color(0xFFF1F5F3),
    surfaceContainerHigh = Color(0xFFEBF0ED),
    surfaceContainerHighest = Color(0xFFE5EBE8),
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = DangerInk,
    outline = Color(0xFFC6D0C9),
    outlineVariant = LineLight,
    scrim = Color.Black,
)

internal val DarkColors = darkColorScheme(
    primary = DarkMint,
    onPrimary = Color(0xFF06301B),
    primaryContainer = DarkMintSoft,
    onPrimaryContainer = DarkMintInk,
    secondary = DarkMintDeep,
    onSecondary = Color(0xFF06301B),
    secondaryContainer = Color(0xFF1D3E30),
    onSecondaryContainer = DarkMintInk,
    tertiary = DarkTangerine,
    onTertiary = Color(0xFF3A2A10),
    tertiaryContainer = DarkTangerineSoft,
    onTertiaryContainer = DarkTangerineInk,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkInkSecondary,
    surfaceTint = DarkMint,
    surfaceDim = Color(0xFF0A0F0D),
    surfaceBright = Color(0xFF33403A),
    surfaceContainerLowest = Color(0xFF070B09),
    surfaceContainerLow = Color(0xFF151D18),
    surfaceContainer = Color(0xFF18201B),
    surfaceContainerHigh = Color(0xFF202A24),
    surfaceContainerHighest = Color(0xFF28332C),
    inverseSurface = DarkInk,
    inverseOnSurface = DarkPaper,
    error = DarkDanger,
    onError = Color(0xFF3A1209),
    errorContainer = DarkDangerSoft,
    onErrorContainer = Color(0xFFF5D5CD),
    outline = Color(0xFF465449),
    outlineVariant = DarkLine,
    scrim = Color.Black,
)
