package com.deepseekchat.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** 壁纸动态取色的特殊 key，其余 key 见 SeedColor */
const val SEED_WALLPAPER = "wallpaper"

private val LightScheme = lightColorScheme(
    // 全部按 M3 色调阶来：主色用 tone 40（不是品牌色的高饱和值），容器用 tone 90/10
    primary = Color(0xFF3A56D4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF),
    onPrimaryContainer = Color(0xFF001158),
    secondary = Color(0xFF4F5A82),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE1F9),
    onSecondaryContainer = Color(0xFF101A34),
    tertiary = Color(0xFF75546F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F0),
    onTertiaryContainer = Color(0xFF2D1229),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceTint = Color(0xFF3A56D4),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFB8C4FF),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    surfaceDim = Color(0xFFDBD9E2),
    surfaceBright = Color(0xFFFAF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3FA),
    surfaceContainer = Color(0xFFEEEDF5),
    surfaceContainerHigh = Color(0xFFE8E7EF),
    surfaceContainerHighest = Color(0xFFE2E1E9)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF0A2B8F),
    primaryContainer = Color(0xFF223FA8),
    onPrimaryContainer = Color(0xFFDDE2FF),
    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF434659),
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE5BBDC),
    onTertiary = Color(0xFF452742),
    tertiaryContainer = Color(0xFF5D3D59),
    onTertiaryContainer = Color(0xFFFFD7F0),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E1E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C6D0),
    surfaceTint = Color(0xFFB8C4FF),
    inverseSurface = Color(0xFFE3E1E9),
    inverseOnSurface = Color(0xFF2F3036),
    inversePrimary = Color(0xFF3A56D4),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    surfaceDim = Color(0xFF111318),
    surfaceBright = Color(0xFF37393F),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191B21),
    surfaceContainer = Color(0xFF1D1F25),
    surfaceContainerHigh = Color(0xFF282A30),
    surfaceContainerHighest = Color(0xFF33353B)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeepChatTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    seedKey: String = SeedColor.DEFAULT.key,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // wallpaper = 跟壁纸取色（Android 12+）；否则用手动种子色（Android 11 及以下走这条路）
    val wallpaper = dynamicColor && supportsDynamic && seedKey == SEED_WALLPAPER
    val manual = SeedColor.of(seedKey)?.takeIf { it != SeedColor.DEFAULT }
    val colorScheme = when {
        wallpaper && dark -> dynamicDarkColorScheme(LocalContext.current)
        wallpaper -> dynamicLightColorScheme(LocalContext.current)
        manual != null -> seedScheme(manual, dark)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = Typography(),
        content = content
    )
}
