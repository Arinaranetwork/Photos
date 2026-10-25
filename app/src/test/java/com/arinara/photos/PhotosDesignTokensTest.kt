// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arinara.photos.ui.components.presets.DefaultBottomClearance
import com.arinara.photos.ui.theme.ElectricSlateTokens
import com.arinara.photos.ui.theme.SpringPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotosDesignTokensTest {

    @Test
    fun testElectricSlateTokensMatchAndroidMd() {
        assertEquals(Color(0xFF0D1117), ElectricSlateTokens.BgCanvas)
        assertEquals(Color(0xFF161C26), ElectricSlateTokens.SurfacePanel)
        assertEquals(Color(0xFF1F2736), ElectricSlateTokens.SurfaceElevated)
        assertEquals(Color(0xFF262F3E), ElectricSlateTokens.BorderSubtle)
        assertEquals(Color(0xFF283245), ElectricSlateTokens.TrackInactive)
        assertEquals(Color(0xFFF8FAFC), ElectricSlateTokens.TextPrimary)
        assertEquals(Color(0xFF94A3B8), ElectricSlateTokens.TextMuted)
        assertEquals(Color(0xFF7A5AF8), ElectricSlateTokens.PrimaryAccent)
        assertEquals(Color(0xFFDC5A5A), ElectricSlateTokens.Destructive)
    }

    @Test
    fun testBottomClearanceMeetsAndroidMdStandard() {
        assertTrue(DefaultBottomClearance >= 100.dp)
    }

    @Test
    fun testSpringPhysicsParameters() {
        assertEquals(0.5f, SpringPhysics.JigglePressSpec.dampingRatio)
        assertEquals(180f, SpringPhysics.JigglePressSpec.stiffness)
        assertEquals(0.45f, SpringPhysics.ViscousGooeySpec.dampingRatio)
        assertEquals(100f, SpringPhysics.ViscousGooeySpec.stiffness)
        assertEquals(0.6f, SpringPhysics.DismissFadeSpec.dampingRatio)
        assertEquals(300f, SpringPhysics.DismissFadeSpec.stiffness)
    }
}
