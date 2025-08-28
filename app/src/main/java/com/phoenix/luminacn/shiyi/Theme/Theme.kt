package com.phoenix.luminacn.shiyi.Theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ShiyiColors {
    val Primary = Color(0xFF8B5CF6)
    val PrimaryLight = Color(0xFFA78BFA)
    val PrimaryDark = Color(0xFF7C3AED)
    val OnPrimary = Color.White

    val Secondary = Color(0xFF06B6D4)
    val SecondaryVariant = Color(0xFF0891B2)
    val SecondaryLight = Color(0xFF22D3EE)
    val OnSecondary = Color.White

    val Accent = Color(0xFFEC4899)
    val AccentLight = Color(0xFFF472B6)
    val AccentDark = Color(0xFFDB2777)

    val Background = Color(0xFF0C0A1A)
    val Surface = Color(0xFF1A1625)
    val SurfaceVariant = Color(0xFF2D2438)
    val SurfaceContainer = Color(0xFF252030)

    val OnBackground = Color(0xFFF8FAFC)
    val OnSurface = Color(0xFFE2E8F0)
    val OnSurfaceVariant = Color(0xFF94A3B8)

    val Error = Color(0xFFEF4444)
    val ErrorLight = Color(0xFFF87171)

    val Border = Color(0xFF3D3349)
    val BorderLight = Color(0xFF4C4A5A)

    val Overlay = Color(0x80000000)

    val MinimapBackground = Color(0xCC000000)
    val MinimapGrid = Color(0x66A9A9A9)
    val MinimapCrosshair = Color(0x80808080)
    val MinimapPlayerMarker = Color(0xFFFFFFFF)
    val MinimapNorth = Color(0xFF0000FF)
    val MinimapEntityClose = Color(0xFFFF0000)
    val MinimapEntityFar = Color(0xFFFFFF00)
    val MinimapZoom = 1.0f
    val MinimapDotSize = 5
}

private val NovaDarkColorScheme = darkColorScheme(
    primary = ShiyiColors.Primary,
    onPrimary = ShiyiColors.OnPrimary,
    primaryContainer = ShiyiColors.PrimaryDark,
    onPrimaryContainer = ShiyiColors.PrimaryLight,
    secondary = ShiyiColors.Secondary,
    onSecondary = ShiyiColors.OnSecondary,
    secondaryContainer = ShiyiColors.SecondaryVariant,
    onSecondaryContainer = ShiyiColors.SecondaryLight,
    tertiary = ShiyiColors.Accent,
    onTertiary = Color.White,
    tertiaryContainer = ShiyiColors.AccentDark.copy(alpha = 0.2f),
    onTertiaryContainer = ShiyiColors.AccentLight,
    background = ShiyiColors.Background,
    onBackground = ShiyiColors.OnBackground,
    surface = ShiyiColors.Surface,
    onSurface = ShiyiColors.OnSurface,
    surfaceVariant = ShiyiColors.SurfaceVariant,
    onSurfaceVariant = ShiyiColors.OnSurfaceVariant,
    surfaceContainer = ShiyiColors.SurfaceContainer,
    error = ShiyiColors.Error,
    onError = Color.White,
    errorContainer = ShiyiColors.Error.copy(alpha = 0.2f),
    onErrorContainer = ShiyiColors.ErrorLight,
    outline = ShiyiColors.Border,
    outlineVariant = ShiyiColors.BorderLight.copy(alpha = 0.5f),
    scrim = ShiyiColors.Overlay,
    inverseSurface = ShiyiColors.OnSurface,
    inverseOnSurface = ShiyiColors.Surface,
    inversePrimary = ShiyiColors.PrimaryDark
)

private val NovaLightColorScheme = lightColorScheme(
    primary = ShiyiColors.Primary,
    onPrimary = ShiyiColors.OnPrimary,
    primaryContainer = ShiyiColors.Primary.copy(alpha = 0.1f),
    onPrimaryContainer = ShiyiColors.Primary,
    secondary = ShiyiColors.Secondary,
    onSecondary = ShiyiColors.OnSecondary,
    secondaryContainer = ShiyiColors.Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = ShiyiColors.Secondary,
    tertiary = ShiyiColors.Accent,
    onTertiary = ShiyiColors.OnPrimary,
    tertiaryContainer = ShiyiColors.Accent.copy(alpha = 0.1f),
    onTertiaryContainer = ShiyiColors.Accent,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF666666),
    surfaceContainer = Color(0xFFE5E5E5),
    error = ShiyiColors.Error,
    onError = ShiyiColors.OnPrimary,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0)
)

val NovaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun ShiyiClientTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NovaDarkColorScheme
        else -> NovaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovaTypography,
        content = content
    )
}