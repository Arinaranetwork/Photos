// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.ui.theme

import androidx.compose.ui.graphics.Color

object ElectricSlateTokens {
    val BgCanvas = Color(0xFF0D1117)        // Main screen background canvas and dot-grid background
    val SurfacePanel = Color(0xFF161C26)    // Primary cards, bottom docks, settings panels
    val SurfaceElevated = Color(0xFF1F2736) // Dropdowns, floating overlays, tooltips
    val BorderSubtle = Color(0xFF262F3E)    // 1dp subtle structural outline and dividers
    val TrackInactive = Color(0xFF283245)   // Inactive slider tracks, neutral containers
    val TextPrimary = Color(0xFFF8FAFC)     // High-contrast primary text, headings
    val TextSecondary = Color(0xFF94A3B8)   // Secondary labels, descriptions, subtitles
    val TextMuted = Color(0xFF94A3B8)       // Secondary labels and descriptions
    val PrimaryAccent = Color(0xFF7A5AF8)   // Electric violet: active states, slider thumbs, FABs
    val Destructive = Color(0xFFDC5A5A)     // Destructive actions, delete buttons, error alerts

    data class Swatch(
        val name: String,
        val color: Color,
        val hex: String,
        val description: String
    )

    val AllSwatches: List<Swatch> = listOf(
        Swatch("BgCanvas", BgCanvas, "#0D1117", "Main background canvas"),
        Swatch("SurfacePanel", SurfacePanel, "#161C26", "Primary cards and bottom docks"),
        Swatch("SurfaceElevated", SurfaceElevated, "#1F2736", "Dropdowns and overlays"),
        Swatch("BorderSubtle", BorderSubtle, "#262F3E", "1dp subtle structural outline"),
        Swatch("TrackInactive", TrackInactive, "#283245", "Inactive tracks and neutral containers"),
        Swatch("TextPrimary", TextPrimary, "#F8FAFC", "High-contrast primary text"),
        Swatch("TextMuted", TextMuted, "#94A3B8", "Secondary labels and descriptions"),
        Swatch("PrimaryAccent", PrimaryAccent, "#7A5AF8", "Electric violet primary accent"),
        Swatch("Destructive", Destructive, "#DC5A5A", "Destructive actions and alerts")
    )
}
