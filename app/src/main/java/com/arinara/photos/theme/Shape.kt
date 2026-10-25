// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape tokens adhering strictly to Android.md and Shape hierarchy:
 * - Capsule / Pill: CircleShape or RoundedCornerShape(999.dp)
 * - Bottom Sheet: RoundedCornerShape(28.dp top)
 * - Primary Cards: RoundedCornerShape(24.dp)
 * - Contextual Panels: RoundedCornerShape(20.dp) / RoundedCornerShape(16.dp)
 * - List Rows / Inputs: RoundedCornerShape(12.dp) / RoundedCornerShape(14.dp)
 * - Circle: CircleShape (FABs, controls, avatars)
 */
object PhotosShapes {
    val FullPill = CircleShape
    val Pill = RoundedCornerShape(999.dp)
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val CardLarge = RoundedCornerShape(24.dp)
    val Panel = RoundedCornerShape(20.dp)
    val Card = RoundedCornerShape(16.dp)
    val Row = RoundedCornerShape(12.dp)
    val InputField = RoundedCornerShape(12.dp)
    val RowComfortable = RoundedCornerShape(14.dp)
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
