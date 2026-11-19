package dev.imhamid.messages.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.imhamid.messages.R

private val AppFont = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_medium, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val base = Typography()
private val AppTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = AppFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = AppFont),
    bodySmall = base.bodySmall.copy(fontFamily = AppFont),
    labelLarge = base.labelLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = AppFont),
    labelSmall = base.labelSmall.copy(fontFamily = AppFont)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5FEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E3FF),
    onPrimaryContainer = Color(0xFF14165C),
    secondary = Color(0xFF6B6F8F),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF1A1B22),
    surface = Color(0xFFF7F7FB),
    onSurface = Color(0xFF1A1B22),
    onSurfaceVariant = Color(0xFF666878),
    surfaceContainerLow = Color(0xFFF1F1F8),
    surfaceContainer = Color(0xFFEBEBF4),
    surfaceContainerHigh = Color(0xFFE6E6F0),
    surfaceContainerHighest = Color(0xFFDFDFEB),
    outline = Color(0xFF9A9CAE),
    outlineVariant = Color(0xFFDADBE6),
    error = Color(0xFFD8434B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA2FF),
    onPrimary = Color(0xFF16186B),
    primaryContainer = Color(0xFF34378F),
    onPrimaryContainer = Color(0xFFE2E3FF),
    secondary = Color(0xFFB4B7D6),
    background = Color(0xFF0F1015),
    onBackground = Color(0xFFE6E6F0),
    surface = Color(0xFF0F1015),
    onSurface = Color(0xFFE6E6F0),
    onSurfaceVariant = Color(0xFFA6A8BA),
    surfaceContainerLow = Color(0xFF15161D),
    surfaceContainer = Color(0xFF1A1B24),
    surfaceContainerHigh = Color(0xFF22232E),
    surfaceContainerHighest = Color(0xFF2A2B38),
    outline = Color(0xFF6E7084),
    outlineVariant = Color(0xFF2E2F3B),
    error = Color(0xFFFF7A82)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalGradient = staticCompositionLocalOf<List<Color>?> { null }

/** Brush for primary surfaces: the chosen gradient, or the plain accent colour. */
@Composable
fun accentBrush(): Brush =
    LocalGradient.current?.let { Brush.linearGradient(it) } ?: SolidColor(MaterialTheme.colorScheme.primary)

/** Content colour that is readable on [accentBrush]. */
@Composable
fun onAccent(): Color = if (LocalGradient.current != null) Color.White else MaterialTheme.colorScheme.onPrimary

private fun schemeFor(dark: Boolean, c: Color): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    val tint = 0.05f
    return if (dark) base.copy(
        primary = lerp(c, Color.White, 0.35f),
        onPrimary = lerp(c, Color.Black, 0.75f),
        primaryContainer = lerp(c, Color.Black, 0.5f),
        onPrimaryContainer = lerp(c, Color.White, 0.85f),
        surfaceContainerLow = lerp(base.surfaceContainerLow, c, tint),
        surfaceContainer = lerp(base.surfaceContainer, c, tint),
        surfaceContainerHigh = lerp(base.surfaceContainerHigh, c, tint),
        surfaceContainerHighest = lerp(base.surfaceContainerHighest, c, tint)
    ) else base.copy(
        primary = c,
        onPrimary = Color.White,
        primaryContainer = lerp(c, Color.White, 0.84f),
        onPrimaryContainer = lerp(c, Color.Black, 0.62f),
        surfaceContainerLow = lerp(base.surfaceContainerLow, c, tint),
        surfaceContainer = lerp(base.surfaceContainer, c, tint),
        surfaceContainerHigh = lerp(base.surfaceContainerHigh, c, tint),
        surfaceContainerHighest = lerp(base.surfaceContainerHighest, c, tint)
    )
}

@Composable
fun MessagesTheme(dark: Boolean, bubble: Int, content: @Composable () -> Unit) {
    val style = BubbleStyles[bubble.coerceIn(0, BubbleStyles.lastIndex)]
    // The accent follows the bubble colour (middle of the gradient for two-colour styles).
    val c = if (style.size > 1) lerp(style.first(), style.last(), 0.5f) else style.first()
    val scheme = remember(dark, bubble) { schemeFor(dark, c) }
    CompositionLocalProvider(LocalGradient provides style.takeIf { it.size > 1 }) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
