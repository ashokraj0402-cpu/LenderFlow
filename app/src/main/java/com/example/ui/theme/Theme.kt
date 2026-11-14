package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TrustBlue,
    onPrimary = CrispWhite,
    primaryContainer = TrustBlueSoft,
    onPrimaryContainer = TrustBlue,
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

val LendFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LendFlowTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Always render the simple, pure-white financial color scheme in both light and dark modes
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        shapes = LendFlowShapes,
        content = content,
    )
}
