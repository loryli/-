package com.my.mealjournal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * 品牌色板：暖白纸感底 + 低饱和自然绿。
 * 规范见 docs/ui-redesign.md §2；除本文件外不要在页面里写死颜色。
 */

// 中性（浅色）
internal val Paper = Color(0xFFF7F6F1)
internal val SurfaceLight = Color(0xFFFFFFFF)
internal val MutedLight = Color(0xFFEFEDE5)
internal val LineLight = Color(0xFFE5E2D9)
internal val Ink = Color(0xFF1E241E)
internal val InkSecondary = Color(0xFF5E645A)
internal val InkTertiary = Color(0xFF8B9186)

// 品牌绿与语义色（浅色）
internal val Green = Color(0xFF4E7A58)
internal val GreenDeep = Color(0xFF3F6549)
internal val GreenSoft = Color(0xFFDDEBE0)
internal val GreenInk = Color(0xFF1C3324)
internal val Sand = Color(0xFFF0E6D2)
internal val SandInk = Color(0xFF4A3A1C)
internal val Amber = Color(0xFFB08748)
internal val Danger = Color(0xFFB25A45)
internal val DangerSoft = Color(0xFFF6E3DD)
internal val DangerInk = Color(0xFF4A1F15)

// 中性（深色）
internal val DarkPaper = Color(0xFF12140F)
internal val DarkSurface = Color(0xFF1B1E17)
internal val DarkMuted = Color(0xFF23271F)
internal val DarkLine = Color(0xFF2E332A)
internal val DarkInk = Color(0xFFE9EDE4)
internal val DarkInkSecondary = Color(0xFFA8AFA3)
internal val DarkInkTertiary = Color(0xFF7C8377)

// 品牌绿与语义色（深色）
internal val DarkGreen = Color(0xFF8FB79A)
internal val DarkGreenDeep = Color(0xFF6E9878)
internal val DarkGreenSoft = Color(0xFF243026)
internal val DarkGreenInk = Color(0xFFC7E0CD)
internal val DarkSand = Color(0xFF3A3324)
internal val DarkSandInk = Color(0xFFD6B47C)
internal val DarkDanger = Color(0xFFE0927F)
internal val DarkDangerSoft = Color(0xFF3A221B)

internal val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenSoft,
    onPrimaryContainer = GreenInk,
    secondary = GreenDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EFE6),
    onSecondaryContainer = GreenInk,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E6D0),
    onTertiaryContainer = SandInk,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = MutedLight,
    onSurfaceVariant = InkSecondary,
    surfaceTint = Green,
    surfaceDim = Color(0xFFE8E5DC),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFAF5),
    surfaceContainer = Color(0xFFF2F0E8),
    surfaceContainerHigh = Color(0xFFECE9E0),
    surfaceContainerHighest = Color(0xFFE6E3D9),
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = DangerInk,
    outline = Color(0xFFC9C6BC),
    outlineVariant = LineLight,
    scrim = Color.Black,
)

internal val DarkColors = darkColorScheme(
    primary = DarkGreen,
    onPrimary = Color(0xFF16261B),
    primaryContainer = DarkGreenSoft,
    onPrimaryContainer = DarkGreenInk,
    secondary = DarkGreenDeep,
    onSecondary = Color(0xFF16261B),
    secondaryContainer = Color(0xFF252E27),
    onSecondaryContainer = DarkGreenInk,
    tertiary = DarkSandInk,
    onTertiary = Color(0xFF3A2A10),
    tertiaryContainer = Color(0xFF3A3324),
    onTertiaryContainer = DarkSandInk,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkInkSecondary,
    surfaceTint = DarkGreen,
    surfaceDim = Color(0xFF0E100C),
    surfaceBright = Color(0xFF33362D),
    surfaceContainerLowest = Color(0xFF0A0C08),
    surfaceContainerLow = Color(0xFF171A13),
    surfaceContainer = Color(0xFF1B1E17),
    surfaceContainerHigh = Color(0xFF23271F),
    surfaceContainerHighest = Color(0xFF2B2F26),
    inverseSurface = DarkInk,
    inverseOnSurface = DarkPaper,
    error = DarkDanger,
    onError = Color(0xFF3A1209),
    errorContainer = DarkDangerSoft,
    onErrorContainer = Color(0xFFF0C6BB),
    outline = Color(0xFF5A6157),
    outlineVariant = DarkLine,
    scrim = Color.Black,
)
