// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arinara.photos.R

/**
 * Global ElmsSans font family with strictly three static weights:
 * - Light (300): secondary text, captions, timestamps, placeholders, muted labels, metadata
 * - Medium (500): body text, button labels, list items, chips, tabs, form inputs
 * - Bold (700): screen titles, card headings, primary action buttons, key metrics
 */
val ElmsSans = FontFamily(
    Font(R.font.elms_sans_light, FontWeight.Thin),
    Font(R.font.elms_sans_light, FontWeight.ExtraLight),
    Font(R.font.elms_sans_light, FontWeight.Light),

    Font(R.font.elms_sans_medium, FontWeight.Normal),
    Font(R.font.elms_sans_medium, FontWeight.Medium),

    Font(R.font.elms_sans_bold, FontWeight.SemiBold),
    Font(R.font.elms_sans_bold, FontWeight.Bold),
    Font(R.font.elms_sans_bold, FontWeight.ExtraBold),
    Font(R.font.elms_sans_bold, FontWeight.Black)
)

val PhotosTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ElmsSans,
        fontWeight = FontWeight.Light,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
