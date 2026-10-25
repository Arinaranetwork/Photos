// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.arinara.photos.ui.theme.ElectricSlateTokens

val PhotosDarkColorScheme = darkColorScheme(
    primary = ElectricSlateTokens.PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color.White,
    inversePrimary = BrandPrimaryLight,

    secondary = ElectricSlateTokens.TrackInactive,
    onSecondary = ElectricSlateTokens.TextPrimary,
    secondaryContainer = ElectricSlateTokens.SurfaceElevated,
    onSecondaryContainer = ElectricSlateTokens.TextPrimary,

    background = ElectricSlateTokens.BgCanvas,
    onBackground = ElectricSlateTokens.TextPrimary,

    surface = ElectricSlateTokens.BgCanvas,
    onSurface = ElectricSlateTokens.TextPrimary,

    surfaceContainerLowest = SurfaceLowestDark,
    surfaceContainerLow = ElectricSlateTokens.SurfacePanel,
    surfaceContainer = ElectricSlateTokens.SurfacePanel,
    surfaceContainerHigh = ElectricSlateTokens.SurfaceElevated,
    surfaceContainerHighest = SurfaceLevel3Dark,

    surfaceVariant = ElectricSlateTokens.SurfaceElevated,
    onSurfaceVariant = ElectricSlateTokens.TextMuted,

    outline = ElectricSlateTokens.BorderSubtle,
    outlineVariant = OutlineVariantDark,

    error = ElectricSlateTokens.Destructive,
    onError = Color.White
)

@Composable
fun PhotosTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PhotosDarkColorScheme,
        typography = PhotosTypography,
        shapes = Shapes,
        content = content
    )
}
