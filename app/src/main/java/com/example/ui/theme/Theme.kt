package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TrustBlue,
    onPrimary = CrispWhite,
    primaryContainer = TrustBlueSoft,
    onPrimaryContainer = TrustNavy,
    secondary = TrustNavy,
    onSecondary = CrispWhite,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = SlateTextPrimary,
    tertiary = EmeraldCollected,
    onTertiary = CrispWhite,
    tertiaryContainer = EmeraldSoft,
    onTertiaryContainer = EmeraldCollected,
    background = CrispWhite,
    onBackground = SlateTextPrimary,
    surface = CrispWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateOutline,
    outlineVariant = SlateOutline,
    error = CrimsonOverdue,
    onError = CrispWhite,
    errorContainer = CrimsonSoft,
    onErrorContainer = CrimsonOverdue,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = CrispWhite,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkPrimary,
    onSecondary = CrispWhite,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = EmeraldCollected,
    onTertiary = CrispWhite,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = EmeraldBorder,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = CrimsonOverdue,
    onError = CrispWhite,
    errorContainer = DarkSurfaceVariant,
    onErrorContainer = CrimsonBorder,
)

val LendFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LendFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = LendFlowShapes,
        content = content,
    )
}
