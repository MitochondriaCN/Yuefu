package com.xianliticn.yuefu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ==================== 扩展颜色系统 ====================
// 应用特有的语义化颜色，通过 CompositionLocal 注入，供组件统一使用。
// 更换配色方案时，只需修改下方 lightExtendedColors / darkExtendedColors 的映射值。

// 非 Composable 层（如 ViewModel / Parser）使用的默认声部颜色映射
val DefaultPartColorMap: Map<Int, Color> = mapOf(
    0 to Green800,
    1 to Orange800,
    2 to Color.Blue,
    3 to Color.Red
)

data class YuefuExtendedColors(
    // 钢琴键盘
    val pianoWhiteKey: Color,
    val pianoWhiteKeyPressed: Color,
    val pianoBlackKey: Color,
    val pianoBlackKeyPressed: Color,
    val pianoKeyBorder: Color,
    val pianoWhiteKeyGradientStart: Color,
    val pianoWhiteKeyGradientEnd: Color,
    val pianoWhiteKeyPressedGradientStart: Color,
    val pianoWhiteKeyPressedGradientEnd: Color,
    val pianoBlackKeyGradientStart: Color,
    val pianoBlackKeyGradientEnd: Color,
    val pianoBlackKeyPressedGradientStart: Color,
    val pianoBlackKeyPressedGradientEnd: Color,
    val pianoShadow: Color,

    // 扫描/裁剪
    val scanOverlay: Color,
    val scanCropLine: Color,

    // 状态指示
    val statusOnline: Color,
    val statusOffline: Color,

    // 乐谱声部颜色
    val partColors: List<Color>,

    // 主要操作按钮
    val primaryAction: Color,
    val onPrimaryAction: Color,
)

val LocalYuefuExtendedColors = compositionLocalOf {
    // 提供一个默认的亮色扩展颜色，避免未提供时的空异常
    lightExtendedColors
}

val lightExtendedColors = YuefuExtendedColors(
    pianoWhiteKey = Color(0xFFFFFFF0),
    pianoWhiteKeyPressed = Color(0xFFDDDDDD),
    pianoBlackKey = Color(0xFF000000),
    pianoBlackKeyPressed = Color(0xFFBBBBBB),
    pianoKeyBorder = Color(0xFFCCCCCC),
    pianoWhiteKeyGradientStart = Color.White,
    pianoWhiteKeyGradientEnd = Color(0xFFF2F2F2),
    pianoWhiteKeyPressedGradientStart = Color(0xFFBBBBBB),
    pianoWhiteKeyPressedGradientEnd = Color(0xFFDDDDDD),
    pianoBlackKeyGradientStart = Color(0xFF333333),
    pianoBlackKeyGradientEnd = Color(0xFF000000),
    pianoBlackKeyPressedGradientStart = Color(0xFF444444),
    pianoBlackKeyPressedGradientEnd = Color(0xFF222222),
    pianoShadow = Color(0x33000000),

    scanOverlay = Color.Gray.copy(alpha = 0.7f),
    scanCropLine = Orange800,

    statusOnline = Color(0xFF4CAF50),
    statusOffline = Color(0xFFF44336),

    partColors = DefaultPartColorMap.values.toList(),

    primaryAction = Blue800,
    onPrimaryAction = Clouds,
)

val darkExtendedColors = YuefuExtendedColors(
    pianoWhiteKey = Color(0xFFE0E0E0),
    pianoWhiteKeyPressed = Color(0xFFBDBDBD),
    pianoBlackKey = Color(0xFF212121),
    pianoBlackKeyPressed = Color(0xFF424242),
    pianoKeyBorder = Color(0xFF616161),
    pianoWhiteKeyGradientStart = Color(0xFFE0E0E0),
    pianoWhiteKeyGradientEnd = Color(0xFFBDBDBD),
    pianoWhiteKeyPressedGradientStart = Color(0xFFBDBDBD),
    pianoWhiteKeyPressedGradientEnd = Color(0xFF9E9E9E),
    pianoBlackKeyGradientStart = Color(0xFF424242),
    pianoBlackKeyGradientEnd = Color(0xFF212121),
    pianoBlackKeyPressedGradientStart = Color(0xFF616161),
    pianoBlackKeyPressedGradientEnd = Color(0xFF424242),
    pianoShadow = Color(0x33000000),

    scanOverlay = Color.Black.copy(alpha = 0.6f),
    scanCropLine = Orange800,

    statusOnline = Color(0xFF81C784),
    statusOffline = Color(0xFFE57373),

    partColors = DefaultPartColorMap.values.toList(),

    primaryAction = Blue800,
    onPrimaryAction = Clouds,
)

val MaterialTheme.yuefuExtendedColors: YuefuExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalYuefuExtendedColors.current

// ==================== Material 3 主题方案 ====================

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFCAC4D0),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = Color(0xFF6750A4),
    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    scrim = Color.Black,
)

@Composable
fun YuefuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) darkExtendedColors else lightExtendedColors

    CompositionLocalProvider(LocalYuefuExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
