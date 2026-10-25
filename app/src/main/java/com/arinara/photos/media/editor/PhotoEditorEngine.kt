// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import com.arinara.photos.media.inspector.RgbHistogramData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class EditorModule(val label: String) {
    TRANSFORM("Transform"),
    LIGHT("Light"),
    CURVES("Curves"),
    HSL_8BAND("8-Color HSL"),
    COLOR_GRADE_3WAY("3-Way Grade"),
    CALIBRATION("Calibration"),
    DETAIL("Detail"),
    OPTICS_FX("Optics & FX"),
    MASK_REDACT("Mask & Redact"),
    FILM_EXPORT("Film & Export")
}

enum class EditorAspectPreset(val label: String, val ratioW: Float?, val ratioH: Float?) {
    FREE("Free", null, null),
    ORIGINAL("Original", -1f, -1f),
    SQUARE_1_1("1:1", 1f, 1f),
    PORTRAIT_4_5("4:5", 4f, 5f),
    LANDSCAPE_5_4("5:4", 5f, 4f),
    CLASSIC_3_2("3:2", 3f, 2f),
    PORTRAIT_2_3("2:3", 2f, 3f),
    WIDESCREEN_16_9("16:9", 16f, 9f),
    VERTICAL_9_16("9:16", 9f, 16f)
}

enum class CurveChannel(val label: String) {
    MASTER_LUMA("Luma"),
    RED("Red"),
    GREEN("Green"),
    BLUE("Blue")
}

enum class HslColorBand(val label: String, val centerHueDeg: Float, val halfWidthDeg: Float) {
    RED("Red", 0f, 28f),
    ORANGE("Orange", 30f, 25f),
    YELLOW("Yellow", 60f, 28f),
    GREEN("Green", 120f, 45f),
    AQUA("Aqua", 180f, 35f),
    BLUE("Blue", 240f, 38f),
    PURPLE("Purple", 275f, 30f),
    MAGENTA("Magenta", 315f, 35f)
}

data class HslBandAdjustment(
    val hueShift: Float = 0f,      // -100..+100 -> -45 deg..+45 deg
    val saturation: Float = 0f,    // -100..+100
    val luminance: Float = 0f      // -100..+100
)

enum class SelectiveMaskType(val label: String) {
    NONE("Off"),
    RADIAL_SPOTLIGHT("Radial"),
    LINEAR_GRADIENT("Linear")
}

enum class RedactionMode(val label: String) {
    PIXELATE_MOSAIC("Pixelate"),
    GAUSSIAN_BLUR("Blur")
}

data class RedactionRegion(
    val centerX: Float, // 0f..1f
    val centerY: Float, // 0f..1f
    val radius: Float = 0.08f, // 0.02f..0.30f
    val mode: RedactionMode = RedactionMode.PIXELATE_MOSAIC,
    val strength: Float = 60f // 10..100
)

enum class FilmStockPreset(val label: String) {
    ORIGINAL("Original"),
    KODAK_PORTRA_400("Portra 400"),
    KODAK_GOLD_200("Gold 200"),
    KODAK_EKTAR_100("Ektar 100"),
    FUJI_VELVIA_50("Velvia 50"),
    FUJI_PRO_400H("Pro 400H"),
    FUJI_CLASSIC_CHROME("Classic Chrome"),
    CINESTILL_800T("Cinestill 800T"),
    ILFORD_HP5_BW("Ilford HP5"),
    KODAK_TRI_X_400("Tri-X 400"),
    LEICA_MONOCHROM("Monochrom"),
    TEAL_ORANGE_CINE("Teal & Orange"),
    BLEACH_BYPASS("Bleach Bypass"),
    ELECTRIC_SLATE_NOCTURNE("Nocturne"),
    WARM_GOLDEN_HOUR("Golden Hour"),
    MATTE_ANALOG_FADE("Matte Fade")
}

enum class PhotoExportFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG_95("JPG 95%", "jpg", "image/jpeg"),
    JPEG_100("JPG 100%", "jpg", "image/jpeg"),
    PNG_LOSSLESS("PNG", "png", "image/png"),
    WEBP_HQ("WebP", "webp", "image/webp")
}

enum class ScopeMode(val label: String) {
    OFF("Scopes Off"),
    HISTOGRAM_RGB("RGB Histogram"),
    RGB_PARADE("RGB Parade")
}

data class PhotoEditRecipe(
    // Module 1: Transform, Keystone & Lens Optics (10 params)
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val aspectPreset: EditorAspectPreset = EditorAspectPreset.FREE,
    val straightenDegrees: Float = 0f,       // -45..+45
    val rotateQuarterTurns: Int = 0,         // 0..3 (0, 90, 180, 270)
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val keystoneVertical: Float = 0f,        // -100..+100
    val keystoneHorizontal: Float = 0f,      // -100..+100
    val anamorphicScaleX: Float = 1f,        // 0.75..1.50
    val lensDistortion: Float = 0f,          // -100..+100 (Barrel <-> Pincushion)

    // Module 2: Light, Exposure & Dynamic Range (10 params)
    val exposureEv: Float = 0f,              // -4.0..+4.0 EV
    val brilliance: Float = 0f,              // -100..+100
    val contrast: Float = 0f,                // -100..+100
    val contrastPivot: Float = 0.50f,        // 0.15..0.85
    val highlights: Float = 0f,              // -100..+100
    val midtones: Float = 0f,                // -100..+100
    val shadows: Float = 0f,                 // -100..+100
    val whites: Float = 0f,                  // -100..+100
    val blacks: Float = 0f,                  // -100..+100
    val dehaze: Float = 0f,                  // -100..+100

    // Module 3: 4-Channel 5-Node Point Curves & 4 Parametric Zones (8 params)
    val lumaCurveNodes: List<Float> = DEFAULT_CURVE_NODES,
    val redCurveNodes: List<Float> = DEFAULT_CURVE_NODES,
    val greenCurveNodes: List<Float> = DEFAULT_CURVE_NODES,
    val blueCurveNodes: List<Float> = DEFAULT_CURVE_NODES,
    val parametricShadows: Float = 0f,       // -100..+100
    val parametricDarks: Float = 0f,         // -100..+100
    val parametricLights: Float = 0f,        // -100..+100
    val parametricHighlights: Float = 0f,    // -100..+100

    // Module 4: 8-Band HSL Per-Color Mixer (24 params: 8 bands x 3)
    val hslBands: Map<HslColorBand, HslBandAdjustment> = defaultHslBands(),

    // Module 5: 3-Way Color Grading Wheels (11 params)
    val shadowsHue: Float = 220f,            // 0..360
    val shadowsSat: Float = 0f,              // 0..100
    val shadowsLuma: Float = 0f,             // -100..+100
    val midtonesHue: Float = 35f,            // 0..360
    val midtonesSat: Float = 0f,             // 0..100
    val midtonesLuma: Float = 0f,            // -100..+100
    val highlightsHue: Float = 45f,          // 0..360
    val highlightsSat: Float = 0f,           // 0..100
    val highlightsLuma: Float = 0f,          // -100..+100
    val gradeBlending: Float = 50f,          // 0..100
    val gradeBalance: Float = 0f,            // -100..+100

    // Module 6: White Balance, Eyedropper & Camera RGB Calibration (11 params)
    val temperature: Float = 0f,             // -100..+100
    val tint: Float = 0f,                    // -100..+100
    val vibrance: Float = 0f,                // -100..+100
    val saturation: Float = 0f,              // -100..+100
    val shadowTint: Float = 0f,              // -100..+100
    val redPrimaryHue: Float = 0f,           // -100..+100
    val redPrimarySat: Float = 0f,           // -100..+100
    val greenPrimaryHue: Float = 0f,         // -100..+100
    val greenPrimarySat: Float = 0f,         // -100..+100
    val bluePrimaryHue: Float = 0f,          // -100..+100
    val bluePrimarySat: Float = 0f,          // -100..+100

    // Module 7: Detail, Texture, Edge-Masked Sharpening & Denoise (7 params)
    val texture: Float = 0f,                 // -100..+100
    val clarity: Float = 0f,                 // -100..+100
    val sharpenAmount: Float = 0f,           // 0..150
    val sharpenRadius: Float = 1.0f,         // 0.5..3.0
    val sharpenMasking: Float = 0f,          // 0..100
    val lumaDenoise: Float = 0f,             // 0..100
    val chromaDenoise: Float = 0f,           // 0..100

    // Module 8: Vignette, Film Grain, Halation Bloom & Defringe (9 params)
    val vignetteAmount: Float = 0f,          // -100..+100
    val vignetteMidpoint: Float = 50f,       // 0..100
    val vignetteRoundness: Float = 0f,       // -100..+100
    val vignetteFeather: Float = 60f,        // 0..100
    val grainAmount: Float = 0f,             // 0..100
    val grainSize: Float = 25f,              // 1..100
    val grainRoughness: Float = 50f,         // 0..100
    val halationBloom: Float = 0f,           // 0..100
    val chromaticDefringe: Float = 0f,       // -100..+100

    // Module 9: Selective Masks & Privacy Redaction
    val maskType: SelectiveMaskType = SelectiveMaskType.NONE,
    val maskCenterX: Float = 0.5f,
    val maskCenterY: Float = 0.5f,
    val maskRadius: Float = 0.38f,
    val maskFeather: Float = 55f,
    val maskInvert: Boolean = false,
    val maskExposureEv: Float = 0f,          // -2.0..+2.0
    val maskContrast: Float = 0f,            // -100..+100
    val maskSaturation: Float = 0f,          // -100..+100
    val maskTemperature: Float = 0f,         // -100..+100
    val redactionRegions: List<RedactionRegion> = emptyList(),

    // Module 10: 16 Curated Film Stocks & Creative FX
    val filmStock: FilmStockPreset = FilmStockPreset.ORIGINAL,
    val filmIntensity: Float = 85f,          // 0..100
    val posterizeLevels: Int = 64,           // 2..64 (64 = off)
    val solarizeThreshold: Float = 0f        // 0..100 (0 = off)
) {
    fun activeParameterCount(): Int {
        var count = 0
        if (cropLeft > 0.001f || cropTop > 0.001f || cropRight < 0.999f || cropBottom < 0.999f) count++
        if (abs(straightenDegrees) > 0.05f) count++
        if (rotateQuarterTurns != 0) count++
        if (flipHorizontal) count++
        if (flipVertical) count++
        if (abs(keystoneVertical) > 0.5f) count++
        if (abs(keystoneHorizontal) > 0.5f) count++
        if (abs(anamorphicScaleX - 1f) > 0.01f) count++
        if (abs(lensDistortion) > 0.5f) count++

        if (abs(exposureEv) > 0.01f) count++
        if (abs(brilliance) > 0.5f) count++
        if (abs(contrast) > 0.5f) count++
        if (abs(contrastPivot - 0.5f) > 0.01f) count++
        if (abs(highlights) > 0.5f) count++
        if (abs(midtones) > 0.5f) count++
        if (abs(shadows) > 0.5f) count++
        if (abs(whites) > 0.5f) count++
        if (abs(blacks) > 0.5f) count++
        if (abs(dehaze) > 0.5f) count++

        if (lumaCurveNodes != DEFAULT_CURVE_NODES) count++
        if (redCurveNodes != DEFAULT_CURVE_NODES) count++
        if (greenCurveNodes != DEFAULT_CURVE_NODES) count++
        if (blueCurveNodes != DEFAULT_CURVE_NODES) count++
        if (abs(parametricShadows) > 0.5f) count++
        if (abs(parametricDarks) > 0.5f) count++
        if (abs(parametricLights) > 0.5f) count++
        if (abs(parametricHighlights) > 0.5f) count++

        hslBands.values.forEach { b ->
            if (abs(b.hueShift) > 0.5f) count++
            if (abs(b.saturation) > 0.5f) count++
            if (abs(b.luminance) > 0.5f) count++
        }

        if (shadowsSat > 0.5f || abs(shadowsLuma) > 0.5f) count++
        if (midtonesSat > 0.5f || abs(midtonesLuma) > 0.5f) count++
        if (highlightsSat > 0.5f || abs(highlightsLuma) > 0.5f) count++

        if (abs(temperature) > 0.5f) count++
        if (abs(tint) > 0.5f) count++
        if (abs(vibrance) > 0.5f) count++
        if (abs(saturation) > 0.5f) count++
        if (abs(shadowTint) > 0.5f) count++
        if (abs(redPrimaryHue) > 0.5f || abs(redPrimarySat) > 0.5f) count++
        if (abs(greenPrimaryHue) > 0.5f || abs(greenPrimarySat) > 0.5f) count++
        if (abs(bluePrimaryHue) > 0.5f || abs(bluePrimarySat) > 0.5f) count++

        if (abs(texture) > 0.5f) count++
        if (abs(clarity) > 0.5f) count++
        if (sharpenAmount > 0.5f) count++
        if (lumaDenoise > 0.5f) count++
        if (chromaDenoise > 0.5f) count++

        if (abs(vignetteAmount) > 0.5f) count++
        if (grainAmount > 0.5f) count++
        if (halationBloom > 0.5f) count++
        if (abs(chromaticDefringe) > 0.5f) count++

        if (maskType != SelectiveMaskType.NONE) count++
        count += redactionRegions.size

        if (filmStock != FilmStockPreset.ORIGINAL && filmIntensity > 0.5f) count++
        if (posterizeLevels < 64) count++
        if (solarizeThreshold > 0.5f) count++

        return count
    }

    fun resetModule(module: EditorModule): PhotoEditRecipe = when (module) {
        EditorModule.TRANSFORM -> copy(
            cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f,
            aspectPreset = EditorAspectPreset.FREE,
            straightenDegrees = 0f, rotateQuarterTurns = 0,
            flipHorizontal = false, flipVertical = false,
            keystoneVertical = 0f, keystoneHorizontal = 0f,
            anamorphicScaleX = 1f, lensDistortion = 0f
        )
        EditorModule.LIGHT -> copy(
            exposureEv = 0f, brilliance = 0f, contrast = 0f, contrastPivot = 0.50f,
            highlights = 0f, midtones = 0f, shadows = 0f, whites = 0f, blacks = 0f, dehaze = 0f
        )
        EditorModule.CURVES -> copy(
            lumaCurveNodes = DEFAULT_CURVE_NODES,
            redCurveNodes = DEFAULT_CURVE_NODES,
            greenCurveNodes = DEFAULT_CURVE_NODES,
            blueCurveNodes = DEFAULT_CURVE_NODES,
            parametricShadows = 0f, parametricDarks = 0f,
            parametricLights = 0f, parametricHighlights = 0f
        )
        EditorModule.HSL_8BAND -> copy(hslBands = defaultHslBands())
        EditorModule.COLOR_GRADE_3WAY -> copy(
            shadowsHue = 220f, shadowsSat = 0f, shadowsLuma = 0f,
            midtonesHue = 35f, midtonesSat = 0f, midtonesLuma = 0f,
            highlightsHue = 45f, highlightsSat = 0f, highlightsLuma = 0f,
            gradeBlending = 50f, gradeBalance = 0f
        )
        EditorModule.CALIBRATION -> copy(
            temperature = 0f, tint = 0f, vibrance = 0f, saturation = 0f, shadowTint = 0f,
            redPrimaryHue = 0f, redPrimarySat = 0f,
            greenPrimaryHue = 0f, greenPrimarySat = 0f,
            bluePrimaryHue = 0f, bluePrimarySat = 0f
        )
        EditorModule.DETAIL -> copy(
            texture = 0f, clarity = 0f, sharpenAmount = 0f,
            sharpenRadius = 1.0f, sharpenMasking = 0f,
            lumaDenoise = 0f, chromaDenoise = 0f
        )
        EditorModule.OPTICS_FX -> copy(
            vignetteAmount = 0f, vignetteMidpoint = 50f, vignetteRoundness = 0f, vignetteFeather = 60f,
            grainAmount = 0f, grainSize = 25f, grainRoughness = 50f,
            halationBloom = 0f, chromaticDefringe = 0f
        )
        EditorModule.MASK_REDACT -> copy(
            maskType = SelectiveMaskType.NONE,
            maskCenterX = 0.5f, maskCenterY = 0.5f, maskRadius = 0.38f, maskFeather = 55f,
            maskInvert = false, maskExposureEv = 0f, maskContrast = 0f,
            maskSaturation = 0f, maskTemperature = 0f, redactionRegions = emptyList()
        )
        EditorModule.FILM_EXPORT -> copy(
            filmStock = FilmStockPreset.ORIGINAL, filmIntensity = 85f,
            posterizeLevels = 64, solarizeThreshold = 0f
        )
    }

    companion object {
        val DEFAULT_CURVE_NODES: List<Float> = listOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)

        fun defaultHslBands(): Map<HslColorBand, HslBandAdjustment> =
            HslColorBand.entries.associateWith { HslBandAdjustment() }
    }
}

data class PhotoExportResult(
    val success: Boolean,
    val exportedItem: MediaItem?,
    val message: String
)

object PhotoEditorEngine {

    /**
     * Evaluates a 5-node Monotone Cubic Hermite Spline (Fritsch-Carlson) at x in [0..1].
     */
    fun evaluateMonotoneCubicCurve(nodesY: List<Float>, x: Float): Float {
        if (nodesY.size < 5) return x.coerceIn(0f, 1f)
        val clampedX = x.coerceIn(0f, 1f)
        val xs = floatArrayOf(0.0f, 0.25f, 0.50f, 0.75f, 1.0f)
        val ys = FloatArray(5) { idx -> nodesY[idx].coerceIn(0f, 1f) }

        val n = 4
        val dx = FloatArray(n)
        val secants = FloatArray(n)
        for (i in 0 until n) {
            dx[i] = xs[i + 1] - xs[i]
            secants[i] = (ys[i + 1] - ys[i]) / dx[i]
        }

        val m = FloatArray(n + 1)
        m[0] = secants[0]
        m[n] = secants[n - 1]
        for (i in 1 until n) {
            if (secants[i - 1] * secants[i] <= 0f) {
                m[i] = 0f
            } else {
                m[i] = 0.5f * (secants[i - 1] + secants[i])
            }
        }

        for (i in 0 until n) {
            if (abs(secants[i]) < 1e-6f) {
                m[i] = 0f
                m[i + 1] = 0f
            } else {
                val a = m[i] / secants[i]
                val b = m[i + 1] / secants[i]
                val h = hypot(a.toDouble(), b.toDouble()).toFloat()
                if (h > 3f) {
                    val t = 3f / h
                    m[i] = t * a * secants[i]
                    m[i + 1] = t * b * secants[i]
                }
            }
        }

        val seg = when {
            clampedX < 0.25f -> 0
            clampedX < 0.50f -> 1
            clampedX < 0.75f -> 2
            else -> 3
        }
        val h = dx[seg]
        val t = ((clampedX - xs[seg]) / h).coerceIn(0f, 1f)
        val t2 = t * t
        val t3 = t2 * t
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = t3 - 2f * t2 + t
        val h01 = -2f * t3 + 3f * t2
        val h11 = t3 - t2
        val y = h00 * ys[seg] + h10 * h * m[seg] + h01 * ys[seg + 1] + h11 * h * m[seg + 1]
        return y.coerceIn(0f, 1f)
    }

    /**
     * Compiles a 256-entry Luma + Parametric Tone Curve lookup table [0f..1f] for O(1) pixel evaluation.
     */
    fun buildMasterToneLut(recipe: PhotoEditRecipe): FloatArray {
        val lut = FloatArray(256)
        val expGain = 2.0f.pow(recipe.exposureEv)
        val bril = (recipe.brilliance / 100f).coerceIn(-1f, 1f)
        val cont = (recipe.contrast / 100f).coerceIn(-1f, 1f)
        val pivot = recipe.contrastPivot.coerceIn(0.15f, 0.85f)
        val gamma = 2.0f.pow(cont * 1.15f)

        val hi = (recipe.highlights / 100f).coerceIn(-1f, 1f)
        val mid = (recipe.midtones / 100f).coerceIn(-1f, 1f)
        val sh = (recipe.shadows / 100f).coerceIn(-1f, 1f)
        val wh = (recipe.whites / 100f).coerceIn(-1f, 1f)
        val bl = (recipe.blacks / 100f).coerceIn(-1f, 1f)

        val pSh = (recipe.parametricShadows / 100f).coerceIn(-1f, 1f)
        val pDk = (recipe.parametricDarks / 100f).coerceIn(-1f, 1f)
        val pLt = (recipe.parametricLights / 100f).coerceIn(-1f, 1f)
        val pHi = (recipe.parametricHighlights / 100f).coerceIn(-1f, 1f)

        for (i in 0..255) {
            var x = (i / 255f) * expGain
            // Soft shoulder compression above 1.0 when exposure is boosted
            if (x > 1f) {
                x = 1f - exp(-(x - 1f)) * 0.15f
            }
            x = x.coerceIn(0f, 1f)

            // Brilliance (Smart HDR lift + highlight recovery)
            if (abs(bril) > 1e-4f) {
                val shadowLift = bril * 0.36f * (1f - x) * (1f - x) * sqrt(x.coerceAtLeast(0f))
                val hiProtect = -bril * 0.18f * x * x * x
                x = (x + shadowLift + hiProtect).coerceIn(0f, 1f)
            }

            // Shadows / Midtones / Highlights / Whites / Blacks
            val wShadow = (1f - x) * (1f - x) * (1f + 2f * x)
            val wHigh = x * x * (3f - 2f * x)
            val wMid = 4f * x * (1f - x)
            x += sh * 0.34f * wShadow * (1f - x)
            x += hi * 0.34f * wHigh * x
            x += mid * 0.26f * wMid
            x += wh * 0.22f * (x * x * x)
            x += bl * 0.22f * ((1f - x) * (1f - x) * (1f - x))
            x = x.coerceIn(0f, 1f)

            // Contrast around dynamic Contrast Pivot
            if (abs(cont) > 1e-4f) {
                x = if (x <= pivot) {
                    pivot * (x / pivot.coerceAtLeast(0.01f)).pow(gamma)
                } else {
                    1f - (1f - pivot) * ((1f - x) / (1f - pivot).coerceAtLeast(0.01f)).pow(gamma)
                }
                x = x.coerceIn(0f, 1f)
            }

            // Parametric 4-Zone Curve adjustments
            if (abs(pSh) > 1e-4f || abs(pDk) > 1e-4f || abs(pLt) > 1e-4f || abs(pHi) > 1e-4f) {
                val zSh = exp(-((x - 0.12f) / 0.16f).pow(2)) * (1f - x)
                val zDk = exp(-((x - 0.35f) / 0.18f).pow(2))
                val zLt = exp(-((x - 0.65f) / 0.18f).pow(2))
                val zHi = exp(-((x - 0.88f) / 0.16f).pow(2)) * x
                x = (x + pSh * 0.22f * zSh + pDk * 0.22f * zDk + pLt * 0.22f * zLt + pHi * 0.22f * zHi).coerceIn(0f, 1f)
            }

            // Master Luma 5-Node Spline Curve
            x = evaluateMonotoneCubicCurve(recipe.lumaCurveNodes, x)
            lut[i] = x.coerceIn(0f, 1f)
        }
        return lut
    }

    /**
     * Samples a 5x5 neighborhood around (normX, normY) to calculate the exact Temperature & Tint
     * offsets that neutralize a color cast into achromatic neutral gray.
     */
    fun sampleNeutralWhiteBalance(bitmap: Bitmap, normX: Float, normY: Float): Pair<Float, Float> {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val cx = (normX.coerceIn(0f, 1f) * (w - 1)).roundToInt()
        val cy = (normY.coerceIn(0f, 1f) * (h - 1)).roundToInt()

        var sumR = 0f
        var sumG = 0f
        var sumB = 0f
        var count = 0
        for (dy in -2..2) {
            for (dx in -2..2) {
                val px = (cx + dx).coerceIn(0, w - 1)
                val py = (cy + dy).coerceIn(0, h - 1)
                val c = bitmap.getPixel(px, py)
                sumR += ((c shr 16) and 0xFF) / 255f
                sumG += ((c shr 8) and 0xFF) / 255f
                sumB += (c and 0xFF) / 255f
                count++
            }
        }
        if (count == 0) return 0f to 0f
        val avgR = (sumR / count).coerceAtLeast(0.02f)
        val avgG = (sumG / count).coerceAtLeast(0.02f)
        val avgB = (sumB / count).coerceAtLeast(0.02f)

        // Required compensation to make R == B (Temperature) and G == (R+B)/2 (Tint)
        val rbDiff = (avgB - avgR) * 180f
        val gmDiff = (avgG - 0.5f * (avgR + avgB)) * 180f
        return rbDiff.coerceIn(-100f, 100f) to gmDiff.coerceIn(-100f, 100f)
    }

    /**
     * Full non-destructive pixel & geometry pipeline.
     * Operates on real Bitmap pixels with zero mock overlays.
     */
    fun processBitmap(source: Bitmap, recipe: PhotoEditRecipe): Bitmap {
        // Step 1: Geometry, Crop, 90° Rotation, Straighten, Flip, Keystone & Lens Distortion
        val geomBitmap = applyGeometryAndOptics(source, recipe)
        val w = geomBitmap.width
        val h = geomBitmap.height
        if (w <= 0 || h <= 0) return geomBitmap

        val pixels = IntArray(w * h)
        geomBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Precompute LUTs for Master Tone and Individual R/G/B Point Curves
        val masterLut = buildMasterToneLut(recipe)
        val redLut = FloatArray(256) { i -> evaluateMonotoneCubicCurve(recipe.redCurveNodes, i / 255f) }
        val greenLut = FloatArray(256) { i -> evaluateMonotoneCubicCurve(recipe.greenCurveNodes, i / 255f) }
        val blueLut = FloatArray(256) { i -> evaluateMonotoneCubicCurve(recipe.blueCurveNodes, i / 255f) }

        val dehazeNorm = (recipe.dehaze / 100f).coerceIn(-1f, 1f)
        val tempNorm = (recipe.temperature / 100f).coerceIn(-1f, 1f)
        val tintNorm = (recipe.tint / 100f).coerceIn(-1f, 1f)
        val vibNorm = (recipe.vibrance / 100f).coerceIn(-1f, 1f)
        val satNorm = (recipe.saturation / 100f).coerceIn(-1f, 1f)
        val shadowTintNorm = (recipe.shadowTint / 100f).coerceIn(-1f, 1f)

        // Camera RGB Calibration 3x3 Matrix
        val rHue = recipe.redPrimaryHue / 100f * 0.22f
        val gHue = recipe.greenPrimaryHue / 100f * 0.22f
        val bHue = recipe.bluePrimaryHue / 100f * 0.22f
        val rSatBoost = 1f + (recipe.redPrimarySat / 100f) * 0.35f
        val gSatBoost = 1f + (recipe.greenPrimarySat / 100f) * 0.35f
        val bSatBoost = 1f + (recipe.bluePrimarySat / 100f) * 0.35f
        val hasCalibration = abs(recipe.redPrimaryHue) > 0.5f || abs(recipe.redPrimarySat) > 0.5f ||
            abs(recipe.greenPrimaryHue) > 0.5f || abs(recipe.greenPrimarySat) > 0.5f ||
            abs(recipe.bluePrimaryHue) > 0.5f || abs(recipe.bluePrimarySat) > 0.5f

        val hasHsl = recipe.hslBands.values.any {
            abs(it.hueShift) > 0.5f || abs(it.saturation) > 0.5f || abs(it.luminance) > 0.5f
        }

        val has3WayGrade = recipe.shadowsSat > 0.5f || abs(recipe.shadowsLuma) > 0.5f ||
            recipe.midtonesSat > 0.5f || abs(recipe.midtonesLuma) > 0.5f ||
            recipe.highlightsSat > 0.5f || abs(recipe.highlightsLuma) > 0.5f

        val shRad = Math.toRadians(recipe.shadowsHue.toDouble()).toFloat()
        val shTintR = cos(shRad) * (recipe.shadowsSat / 100f) * 0.28f
        val shTintG = cos(shRad - 2.0944f) * (recipe.shadowsSat / 100f) * 0.28f
        val shTintB = cos(shRad + 2.0944f) * (recipe.shadowsSat / 100f) * 0.28f

        val midRad = Math.toRadians(recipe.midtonesHue.toDouble()).toFloat()
        val midTintR = cos(midRad) * (recipe.midtonesSat / 100f) * 0.24f
        val midTintG = cos(midRad - 2.0944f) * (recipe.midtonesSat / 100f) * 0.24f
        val midTintB = cos(midRad + 2.0944f) * (recipe.midtonesSat / 100f) * 0.24f

        val hiRad = Math.toRadians(recipe.highlightsHue.toDouble()).toFloat()
        val hiTintR = cos(hiRad) * (recipe.highlightsSat / 100f) * 0.28f
        val hiTintG = cos(hiRad - 2.0944f) * (recipe.highlightsSat / 100f) * 0.28f
        val hiTintB = cos(hiRad + 2.0944f) * (recipe.highlightsSat / 100f) * 0.28f

        val gradePivot = (0.5f - (recipe.gradeBalance / 100f) * 0.28f).coerceIn(0.18f, 0.82f)
        val gradeBlend = (recipe.gradeBlending / 100f).coerceIn(0.05f, 1f)

        val vigAmt = (recipe.vignetteAmount / 100f).coerceIn(-1f, 1f)
        val vigMid = (recipe.vignetteMidpoint / 100f).coerceIn(0.05f, 0.95f)
        val vigFeather = (recipe.vignetteFeather / 100f).coerceIn(0.05f, 1f)
        val vigPower = 2f + (recipe.vignetteRoundness / 100f) * 1.2f

        val grainAmt = (recipe.grainAmount / 100f).coerceIn(0f, 1f)
        val grainScale = (1f + (recipe.grainSize / 100f) * 3.5f).coerceAtLeast(1f)
        val grainRough = (0.5f + (recipe.grainRoughness / 100f)).coerceIn(0.5f, 1.5f)
        val defringeAmt = (recipe.chromaticDefringe / 100f).coerceIn(-1f, 1f)

        val hasMask = recipe.maskType != SelectiveMaskType.NONE && (
            abs(recipe.maskExposureEv) > 0.01f || abs(recipe.maskContrast) > 0.5f ||
                abs(recipe.maskSaturation) > 0.5f || abs(recipe.maskTemperature) > 0.5f
            )
        val maskExpMult = 2.0f.pow(recipe.maskExposureEv)
        val maskContGamma = 2.0f.pow((recipe.maskContrast / 100f) * 0.9f)
        val maskSatMult = 1f + (recipe.maskSaturation / 100f)
        val maskTempShift = (recipe.maskTemperature / 100f) * 0.22f

        val filmStrength = (recipe.filmIntensity / 100f).coerceIn(0f, 1f)
        val posterizeSteps = recipe.posterizeLevels.coerceIn(2, 64)
        val solarizeNorm = (recipe.solarizeThreshold / 100f).coerceIn(0f, 1f)

        // Single-pass per-pixel color, tone, HSL, 3-way grade, film stock, mask, vignette & grain
        for (y in 0 until h) {
            val ny = (y.toFloat() / h.coerceAtLeast(1))
            val dyNorm = (ny - 0.5f) * 2f
            val rowOffset = y * w

            for (x in 0 until w) {
                val idx = rowOffset + x
                val c = pixels[idx]
                val a = (c ushr 24) and 0xFF
                var r = ((c ushr 16) and 0xFF) / 255f
                var g = ((c ushr 8) and 0xFF) / 255f
                var b = (c and 0xFF) / 255f

                // 1. Atmospheric Dehaze
                if (abs(dehazeNorm) > 1e-3f) {
                    val darkChannel = minOf(r, g, b)
                    if (dehazeNorm > 0f) {
                        val trans = (1f - dehazeNorm * 0.65f * darkChannel).coerceIn(0.22f, 1f)
                        r = ((r - 0.85f * (1f - trans)) / trans).coerceIn(0f, 1f)
                        g = ((g - 0.85f * (1f - trans)) / trans).coerceIn(0f, 1f)
                        b = ((b - 0.88f * (1f - trans)) / trans).coerceIn(0f, 1f)
                    } else {
                        val mist = -dehazeNorm * 0.45f
                        r = (r + (0.92f - r) * mist * (1f - darkChannel * 0.5f)).coerceIn(0f, 1f)
                        g = (g + (0.94f - g) * mist * (1f - darkChannel * 0.5f)).coerceIn(0f, 1f)
                        b = (b + (0.97f - b) * mist * (1f - darkChannel * 0.5f)).coerceIn(0f, 1f)
                    }
                }

                // 2. White Balance (Temperature & Tint) + Shadow Tint
                if (abs(tempNorm) > 1e-3f || abs(tintNorm) > 1e-3f || abs(shadowTintNorm) > 1e-3f) {
                    r = (r + tempNorm * 0.22f + tintNorm * 0.08f).coerceIn(0f, 1f)
                    g = (g - tintNorm * 0.18f).coerceIn(0f, 1f)
                    b = (b - tempNorm * 0.22f + tintNorm * 0.08f).coerceIn(0f, 1f)
                    if (abs(shadowTintNorm) > 1e-3f) {
                        val lumaPre = 0.299f * r + 0.587f * g + 0.114f * b
                        val shWeight = (1f - lumaPre) * (1f - lumaPre)
                        r = (r + shadowTintNorm * 0.14f * shWeight).coerceIn(0f, 1f)
                        g = (g - shadowTintNorm * 0.14f * shWeight).coerceIn(0f, 1f)
                    }
                }

                // 3. Master Tone LUT + R/G/B Point Curves
                r = masterLut[(r * 255f).roundToInt().coerceIn(0, 255)]
                g = masterLut[(g * 255f).roundToInt().coerceIn(0, 255)]
                b = masterLut[(b * 255f).roundToInt().coerceIn(0, 255)]

                r = redLut[(r * 255f).roundToInt().coerceIn(0, 255)]
                g = greenLut[(g * 255f).roundToInt().coerceIn(0, 255)]
                b = blueLut[(b * 255f).roundToInt().coerceIn(0, 255)]

                // 4. Camera RGB Primary Calibration Matrix
                if (hasCalibration) {
                    val nr = r * (1f - abs(rHue)) + g * max(0f, rHue) + b * max(0f, -rHue)
                    val ng = g * (1f - abs(gHue)) + b * max(0f, gHue) + r * max(0f, -gHue)
                    val nb = b * (1f - abs(bHue)) + r * max(0f, bHue) + g * max(0f, -bHue)
                    val lumaCal = 0.299f * nr + 0.587f * ng + 0.114f * nb
                    r = (lumaCal + (nr - lumaCal) * rSatBoost).coerceIn(0f, 1f)
                    g = (lumaCal + (ng - lumaCal) * gSatBoost).coerceIn(0f, 1f)
                    b = (lumaCal + (nb - lumaCal) * bSatBoost).coerceIn(0f, 1f)
                }

                // 5. 8-Band HSL Per-Color Mixer + Vibrance & Global Saturation
                if (hasHsl || abs(vibNorm) > 1e-3f || abs(satNorm) > 1e-3f || abs(defringeAmt) > 1e-3f) {
                    val maxC = maxOf(r, g, b)
                    val minC = minOf(r, g, b)
                    val delta = maxC - minC
                    var lum = 0.5f * (maxC + minC)
                    var sat = if (delta < 1e-5f) 0f else delta / (1f - abs(2f * lum - 1f)).coerceAtLeast(1e-4f)
                    var hue = when {
                        delta < 1e-5f -> 0f
                        maxC == r -> 60f * (((g - b) / delta) % 6f)
                        maxC == g -> 60f * (((b - r) / delta) + 2f)
                        else -> 60f * (((r - g) / delta) + 4f)
                    }
                    if (hue < 0f) hue += 360f

                    if (hasHsl && sat > 0.02f) {
                        var totalHueDelta = 0f
                        var totalSatMult = 1f
                        var totalLumDelta = 0f

                        for (band in HslColorBand.entries) {
                            val adj = recipe.hslBands[band] ?: continue
                            if (abs(adj.hueShift) < 0.5f && abs(adj.saturation) < 0.5f && abs(adj.luminance) < 0.5f) continue
                            var diff = abs(hue - band.centerHueDeg)
                            if (diff > 180f) diff = 360f - diff
                            if (diff < band.halfWidthDeg) {
                                val u = diff / band.halfWidthDeg
                                val weight = cos(u * (PI.toFloat() * 0.5f)).pow(2)
                                totalHueDelta += (adj.hueShift / 100f) * 42f * weight
                                totalSatMult += (adj.saturation / 100f) * weight
                                totalLumDelta += (adj.luminance / 100f) * 0.32f * weight
                            }
                        }
                        hue = (hue + totalHueDelta + 360f) % 360f
                        sat = (sat * totalSatMult.coerceAtLeast(0f)).coerceIn(0f, 1f)
                        lum = (lum + totalLumDelta * sat).coerceIn(0f, 1f)
                    }

                    // Chromatic Aberration Defringe (targets purple/magenta & cyan high-saturation fringes)
                    if (abs(defringeAmt) > 1e-3f && sat > 0.25f) {
                        val purpleDist = min(abs(hue - 295f), 360f - abs(hue - 295f))
                        val cyanDist = abs(hue - 185f)
                        if (purpleDist < 32f || cyanDist < 28f) {
                            sat = (sat * (1f - abs(defringeAmt) * 0.65f)).coerceIn(0f, 1f)
                        }
                    }

                    // Vibrance (skin-tone aware non-linear boost) & Saturation
                    if (abs(vibNorm) > 1e-3f) {
                        val skinDist = min(abs(hue - 26f), 360f - abs(hue - 26f))
                        val skinGuard = if (skinDist < 25f && vibNorm > 0f) 0.45f else 1.0f
                        val vibBoost = vibNorm * (1f - sat * sat) * skinGuard
                        sat = (sat + sat * vibBoost).coerceIn(0f, 1f)
                    }
                    if (abs(satNorm) > 1e-3f) {
                        sat = (sat * (1f + satNorm)).coerceIn(0f, 1f)
                    }

                    // HSL -> RGB
                    val cChroma = (1f - abs(2f * lum - 1f)) * sat
                    val hPrime = hue / 60f
                    val xCol = cChroma * (1f - abs(hPrime % 2f - 1f))
                    val m = lum - 0.5f * cChroma
                    when {
                        hPrime < 1f -> { r = cChroma + m; g = xCol + m; b = m }
                        hPrime < 2f -> { r = xCol + m; g = cChroma + m; b = m }
                        hPrime < 3f -> { r = m; g = cChroma + m; b = xCol + m }
                        hPrime < 4f -> { r = m; g = xCol + m; b = cChroma + m }
                        hPrime < 5f -> { r = xCol + m; g = m; b = cChroma + m }
                        else -> { r = cChroma + m; g = m; b = xCol + m }
                    }
                }

                // 6. 3-Way Color Grading Wheels (Shadows / Midtones / Highlights)
                if (has3WayGrade) {
                    val yLuma = (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 1f)
                    val wS = ((1f - (yLuma / gradePivot.coerceAtLeast(0.05f)).coerceIn(0f, 1f)).pow(2f - gradeBlend)).coerceIn(0f, 1f)
                    val wH = (((yLuma - gradePivot).coerceAtLeast(0f) / (1f - gradePivot).coerceAtLeast(0.05f)).pow(2f - gradeBlend)).coerceIn(0f, 1f)
                    val wM = (1f - wS - wH).coerceAtLeast(0f)

                    val lumaShift = (wS * (recipe.shadowsLuma / 100f) + wM * (recipe.midtonesLuma / 100f) + wH * (recipe.highlightsLuma / 100f)) * 0.22f
                    r = (r + wS * shTintR + wM * midTintR + wH * hiTintR + lumaShift).coerceIn(0f, 1f)
                    g = (g + wS * shTintG + wM * midTintG + wH * hiTintG + lumaShift).coerceIn(0f, 1f)
                    b = (b + wS * shTintB + wM * midTintB + wH * hiTintB + lumaShift).coerceIn(0f, 1f)
                }

                // 7. 16 Curated Film Stocks
                if (recipe.filmStock != FilmStockPreset.ORIGINAL && filmStrength > 1e-3f) {
                    val graded = applyFilmStockPixel(r, g, b, recipe.filmStock)
                    r = r + (graded[0] - r) * filmStrength
                    g = g + (graded[1] - g) * filmStrength
                    b = b + (graded[2] - b) * filmStrength
                }

                // 8. Selective Radial / Linear Gradient Mask
                if (hasMask) {
                    val nx = x.toFloat() / w.coerceAtLeast(1)
                    val maskW = computeSelectiveMaskWeight(nx, ny, recipe)
                    if (maskW > 1e-3f) {
                        var mr = (r * maskExpMult + maskTempShift).coerceIn(0f, 1f)
                        var mg = (g * maskExpMult).coerceIn(0f, 1f)
                        var mb = (b * maskExpMult - maskTempShift).coerceIn(0f, 1f)
                        if (abs(recipe.maskContrast) > 0.5f) {
                            mr = 0.5f + (mr - 0.5f) * maskContGamma
                            mg = 0.5f + (mg - 0.5f) * maskContGamma
                            mb = 0.5f + (mb - 0.5f) * maskContGamma
                        }
                        val mLuma = 0.299f * mr + 0.587f * mg + 0.114f * mb
                        mr = (mLuma + (mr - mLuma) * maskSatMult).coerceIn(0f, 1f)
                        mg = (mLuma + (mg - mLuma) * maskSatMult).coerceIn(0f, 1f)
                        mb = (mLuma + (mb - mLuma) * maskSatMult).coerceIn(0f, 1f)

                        r += (mr - r) * maskW
                        g += (mg - g) * maskW
                        b += (mb - b) * maskW
                    }
                }

                // 9. Optical Vignette (with Midpoint, Roundness & Feather)
                if (abs(vigAmt) > 1e-3f) {
                    val dxNorm = ((x.toFloat() / w.coerceAtLeast(1)) - 0.5f) * 2f
                    val dist = (abs(dxNorm).pow(vigPower) + abs(dyNorm).pow(vigPower)).pow(1f / vigPower)
                    val edge = ((dist - vigMid) / vigFeather.coerceAtLeast(0.08f)).coerceIn(0f, 1f)
                    val smoothFalloff = edge * edge * (3f - 2f * edge)
                    if (vigAmt < 0f) {
                        val factor = 1f + vigAmt * smoothFalloff
                        r *= factor; g *= factor; b *= factor
                    } else {
                        r += (1f - r) * vigAmt * smoothFalloff
                        g += (1f - g) * vigAmt * smoothFalloff
                        b += (1f - b) * vigAmt * smoothFalloff
                    }
                }

                // 10. Organic Silver-Halide Film Grain
                if (grainAmt > 1e-3f) {
                    val gx = (x / grainScale).toInt()
                    val gy = (y / grainScale).toInt()
                    val hash = ((gx * 73856093) xor (gy * 19349663) xor 0x5A17C3) and 0x7FFFFFFF
                    val randNorm = ((hash % 2001) / 1000f) - 1f // -1..+1
                    val yLum = 0.299f * r + 0.587f * g + 0.114f * b
                    val emulsionWeight = 0.25f + 3f * yLum * (1f - yLum)
                    val deltaG = randNorm * grainAmt * 0.18f * grainRough * emulsionWeight
                    r = (r + deltaG).coerceIn(0f, 1f)
                    g = (g + deltaG).coerceIn(0f, 1f)
                    b = (b + deltaG).coerceIn(0f, 1f)
                }

                // 11. Creative Solarize & Posterize
                if (solarizeNorm > 1e-3f) {
                    val thresh = 1f - solarizeNorm * 0.75f
                    if (r > thresh) r = 1f - r
                    if (g > thresh) g = 1f - g
                    if (b > thresh) b = 1f - b
                }
                if (posterizeSteps < 64) {
                    val stepsF = (posterizeSteps - 1).coerceAtLeast(1).toFloat()
                    r = (r * stepsF).roundToInt() / stepsF
                    g = (g * stepsF).roundToInt() / stepsF
                    b = (b * stepsF).roundToInt() / stepsF
                }

                val ri = (r.coerceIn(0f, 1f) * 255f).roundToInt()
                val gi = (g.coerceIn(0f, 1f) * 255f).roundToInt()
                val bi = (b.coerceIn(0f, 1f) * 255f).roundToInt()
                pixels[idx] = (a shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }

        // Step 2: Spatial Detail Passes (Texture, Clarity, Edge-Masked Sharpening, Luma/Chroma Denoise, Halation Bloom)
        if (abs(recipe.texture) > 0.5f || abs(recipe.clarity) > 0.5f || recipe.sharpenAmount > 0.5f ||
            recipe.lumaDenoise > 0.5f || recipe.chromaDenoise > 0.5f || recipe.halationBloom > 0.5f
        ) {
            applySpatialDetailAndBloom(pixels, w, h, recipe)
        }

        // Step 3: Privacy Redaction Regions (Mosaic Pixelate / Gaussian Blur)
        if (recipe.redactionRegions.isNotEmpty()) {
            applyPrivacyRedactionRegions(pixels, w, h, recipe.redactionRegions)
        }

        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return outBitmap
    }

    /**
     * Applies Crop, Aspect Ratio lock, 90° Rotation, Straighten (-45..+45°), Flip H/V,
     * 3D Vertical/Horizontal Keystone Homography, Anamorphic Stretch, and Radial Lens Distortion.
     */
    private fun applyGeometryAndOptics(source: Bitmap, recipe: PhotoEditRecipe): Bitmap {
        val srcW = source.width.coerceAtLeast(1)
        val srcH = source.height.coerceAtLeast(1)

        // Compute crop rectangle
        var cL = recipe.cropLeft.coerceIn(0f, 0.85f)
        var cT = recipe.cropTop.coerceIn(0f, 0.85f)
        var cR = recipe.cropRight.coerceIn(cL + 0.10f, 1f)
        var cB = recipe.cropBottom.coerceIn(cT + 0.10f, 1f)

        // Enforce aspect preset if selected
        val preset = recipe.aspectPreset
        if (preset != EditorAspectPreset.FREE) {
            val targetAspect = if (preset == EditorAspectPreset.ORIGINAL) {
                srcW.toFloat() / srcH.toFloat()
            } else {
                (preset.ratioW ?: 1f) / (preset.ratioH ?: 1f)
            }
            val boxW = (cR - cL) * srcW
            val boxH = (cB - cT) * srcH
            val currentAspect = boxW / boxH.coerceAtLeast(1f)
            val cx = 0.5f * (cL + cR)
            val cy = 0.5f * (cT + cB)
            if (currentAspect > targetAspect) {
                val newHalfW = (boxH * targetAspect) / (2f * srcW)
                cL = (cx - newHalfW).coerceIn(0f, 0.9f)
                cR = (cx + newHalfW).coerceIn(cL + 0.1f, 1f)
            } else {
                val newHalfH = (boxW / targetAspect) / (2f * srcH)
                cT = (cy - newHalfH).coerceIn(0f, 0.9f)
                cB = (cy + newHalfH).coerceIn(cT + 0.1f, 1f)
            }
        }

        val cropX = (cL * srcW).roundToInt().coerceIn(0, srcW - 1)
        val cropY = (cT * srcH).roundToInt().coerceIn(0, srcH - 1)
        val cropW = ((cR - cL) * srcW).roundToInt().coerceIn(1, srcW - cropX)
        val cropH = ((cB - cT) * srcH).roundToInt().coerceIn(1, srcH - cropY)

        val quarterTurns = ((recipe.rotateQuarterTurns % 4) + 4) % 4
        val swapDims = quarterTurns % 2 == 1
        val outW = (if (swapDims) cropH else cropW).coerceAtLeast(1)
        val outH = (if (swapDims) cropW else cropH).coerceAtLeast(1)

        val hasTransform = cropX != 0 || cropY != 0 || cropW != srcW || cropH != srcH ||
            quarterTurns != 0 || recipe.flipHorizontal || recipe.flipVertical ||
            abs(recipe.straightenDegrees) > 0.05f ||
            abs(recipe.keystoneVertical) > 0.5f || abs(recipe.keystoneHorizontal) > 0.5f ||
            abs(recipe.anamorphicScaleX - 1f) > 0.01f || abs(recipe.lensDistortion) > 0.5f

        if (!hasTransform) return source

        val srcPixels = IntArray(srcW * srcH)
        source.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)
        val dstPixels = IntArray(outW * outH)

        val rad = Math.toRadians((-recipe.straightenDegrees - quarterTurns * 90f).toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)
        // Auto-zoom scale when straightening so corners don't expose black triangles
        val straightenAbs = Math.toRadians(abs(recipe.straightenDegrees).toDouble()).toFloat()
        val autoZoom = (cos(straightenAbs) + sin(straightenAbs)).coerceAtLeast(1f)

        val kv = (recipe.keystoneVertical / 100f) * 0.38f
        val kh = (recipe.keystoneHorizontal / 100f) * 0.38f
        val kDist = (recipe.lensDistortion / 100f) * 0.32f
        val anamorphicInv = 1f / recipe.anamorphicScaleX.coerceIn(0.75f, 1.50f)

        for (y in 0 until outH) {
            val v = ((y + 0.5f) / outH) * 2f - 1f // -1..+1
            for (x in 0 until outW) {
                val u = ((x + 0.5f) / outW) * 2f - 1f // -1..+1

                // 1. Flip Horizontal / Vertical
                var nx = if (recipe.flipHorizontal) -u else u
                var ny = if (recipe.flipVertical) -v else v

                // 2. Anamorphic Aspect Scale X
                nx *= anamorphicInv

                // 3. 3D Keystone Perspective Homography
                val denom = (1f + kv * ny + kh * nx).coerceAtLeast(0.25f)
                nx /= denom
                ny /= denom

                // 4. Radial Lens Barrel / Pincushion Distortion
                if (abs(kDist) > 1e-4f) {
                    val r2 = nx * nx + ny * ny
                    val radialScale = (1f + kDist * r2 + 0.25f * kDist * kDist * r2 * r2).coerceIn(0.35f, 2.2f)
                    nx *= radialScale
                    ny *= radialScale
                }

                // 5. Straighten + 90° Rotation with auto-zoom
                val rx = (nx * cosA - ny * sinA) / autoZoom
                val ry = (nx * sinA + ny * cosA) / autoZoom

                // Map normalized [-1..+1] back into cropped source pixel space
                val srcFx = cropX + ((rx + 1f) * 0.5f) * (cropW - 1)
                val srcFy = cropY + ((ry + 1f) * 0.5f) * (cropH - 1)

                val ix = srcFx.toInt().coerceIn(0, srcW - 1)
                val iy = srcFy.toInt().coerceIn(0, srcH - 1)
                val ix1 = (ix + 1).coerceIn(0, srcW - 1)
                val iy1 = (iy + 1).coerceIn(0, srcH - 1)
                val fx = (srcFx - ix).coerceIn(0f, 1f)
                val fy = (srcFy - iy).coerceIn(0f, 1f)

                val c00 = srcPixels[iy * srcW + ix]
                val c10 = srcPixels[iy * srcW + ix1]
                val c01 = srcPixels[iy1 * srcW + ix]
                val c11 = srcPixels[iy1 * srcW + ix1]

                val w00 = (1f - fx) * (1f - fy)
                val w10 = fx * (1f - fy)
                val w01 = (1f - fx) * fy
                val w11 = fx * fy

                val r = (((c00 ushr 16) and 0xFF) * w00 + ((c10 ushr 16) and 0xFF) * w10 +
                    ((c01 ushr 16) and 0xFF) * w01 + ((c11 ushr 16) and 0xFF) * w11).roundToInt().coerceIn(0, 255)
                val g = (((c00 ushr 8) and 0xFF) * w00 + ((c10 ushr 8) and 0xFF) * w10 +
                    ((c01 ushr 8) and 0xFF) * w01 + ((c11 ushr 8) and 0xFF) * w11).roundToInt().coerceIn(0, 255)
                val b = ((c00 and 0xFF) * w00 + (c10 and 0xFF) * w10 +
                    (c01 and 0xFF) * w01 + (c11 and 0xFF) * w11).roundToInt().coerceIn(0, 255)

                dstPixels[y * outW + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        out.setPixels(dstPixels, 0, outW, 0, 0, outW, outH)
        return out
    }

    /**
     * Spatial detail & optics convolution pass:
     * - Texture (high-frequency detail vs. skin smoothing)
     * - Clarity (mid-frequency midtone local contrast)
     * - Edge-Masked Unsharp Mask Sharpening (Amount, Radius, Edge Masking threshold)
     * - Edge-Preserving Bilateral Luma & Chroma Noise Reduction
     * - Halation / Pro-Mist Specular Highlight Bloom
     */
    private fun applySpatialDetailAndBloom(
        pixels: IntArray,
        w: Int,
        h: Int,
        recipe: PhotoEditRecipe
    ) {
        if (w < 4 || h < 4) return
        val copy = pixels.copyOf()

        val texAmt = (recipe.texture / 100f) * 0.65f
        val clarAmt = (recipe.clarity / 100f) * 0.75f
        val sharpAmt = (recipe.sharpenAmount / 100f) * 1.15f
        val sharpStep = recipe.sharpenRadius.roundToInt().coerceIn(1, 3)
        val edgeThresh = (recipe.sharpenMasking / 100f) * 0.22f
        val lumaNr = (recipe.lumaDenoise / 100f).coerceIn(0f, 1f)
        val chromaNr = (recipe.chromaDenoise / 100f).coerceIn(0f, 1f)
        val bloomAmt = (recipe.halationBloom / 100f).coerceIn(0f, 1f)

        for (y in sharpStep until (h - sharpStep)) {
            val row = y * w
            val rowUp = (y - sharpStep) * w
            val rowDn = (y + sharpStep) * w

            for (x in sharpStep until (w - sharpStep)) {
                val idx = row + x
                val c = copy[idx]
                var r = ((c ushr 16) and 0xFF) / 255f
                var g = ((c ushr 8) and 0xFF) / 255f
                var b = (c and 0xFF) / 255f

                val cL = copy[row + x - sharpStep]
                val cR = copy[row + x + sharpStep]
                val cU = copy[rowUp + x]
                val cD = copy[rowDn + x]

                val rBlur = (((cL ushr 16) and 0xFF) + ((cR ushr 16) and 0xFF) + ((cU ushr 16) and 0xFF) + ((cD ushr 16) and 0xFF)) / (4f * 255f)
                val gBlur = (((cL ushr 8) and 0xFF) + ((cR ushr 8) and 0xFF) + ((cU ushr 8) and 0xFF) + ((cD ushr 8) and 0xFF)) / (4f * 255f)
                val bBlur = ((cL and 0xFF) + (cR and 0xFF) + (cU and 0xFF) + (cD and 0xFF)) / (4f * 255f)

                val yCur = 0.299f * r + 0.587f * g + 0.114f * b
                val yBlur = 0.299f * rBlur + 0.587f * gBlur + 0.114f * bBlur
                val highFreq = yCur - yBlur
                val edgeMag = abs(highFreq)

                // Luma & Chroma Noise Reduction (bilateral edge-preserving smoothing)
                if (lumaNr > 1e-3f && edgeMag < 0.12f) {
                    val smoothW = lumaNr * (1f - (edgeMag / 0.12f))
                    val yNew = yCur + (yBlur - yCur) * smoothW
                    val dY = yNew - yCur
                    r = (r + dY).coerceIn(0f, 1f)
                    g = (g + dY).coerceIn(0f, 1f)
                    b = (b + dY).coerceIn(0f, 1f)
                }
                if (chromaNr > 1e-3f) {
                    val cbCur = b - yCur
                    val crCur = r - yCur
                    val cbBlur = bBlur - yBlur
                    val crBlur = rBlur - yBlur
                    val cbNew = cbCur + (cbBlur - cbCur) * chromaNr * 0.85f
                    val crNew = crCur + (crBlur - crCur) * chromaNr * 0.85f
                    r = (yCur + crNew).coerceIn(0f, 1f)
                    b = (yCur + cbNew).coerceIn(0f, 1f)
                }

                // Texture, Clarity & Edge-Masked Sharpening
                var detailDelta = 0f
                if (abs(texAmt) > 1e-3f) {
                    detailDelta += highFreq * texAmt
                }
                if (abs(clarAmt) > 1e-3f) {
                    val midWeight = 4f * yCur * (1f - yCur)
                    detailDelta += highFreq * clarAmt * midWeight
                }
                if (sharpAmt > 1e-3f && edgeMag >= edgeThresh) {
                    val maskGain = ((edgeMag - edgeThresh) / (1f - edgeThresh).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                    detailDelta += highFreq * sharpAmt * (0.35f + 0.65f * maskGain)
                }

                if (abs(detailDelta) > 1e-4f) {
                    r = (r + detailDelta).coerceIn(0f, 1f)
                    g = (g + detailDelta).coerceIn(0f, 1f)
                    b = (b + detailDelta).coerceIn(0f, 1f)
                }

                // Halation / Pro-Mist Specular Highlight Bloom
                if (bloomAmt > 1e-3f && yBlur > 0.68f) {
                    val glow = ((yBlur - 0.68f) / 0.32f) * bloomAmt * 0.42f
                    r = (r + glow * 1.15f).coerceIn(0f, 1f)
                    g = (g + glow * 0.62f).coerceIn(0f, 1f)
                    b = (b + glow * 0.25f).coerceIn(0f, 1f)
                }

                val ri = (r * 255f).roundToInt().coerceIn(0, 255)
                val gi = (g * 255f).roundToInt().coerceIn(0, 255)
                val bi = (b * 255f).roundToInt().coerceIn(0, 255)
                pixels[idx] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }
    }

    /**
     * Applies Privacy Redaction Regions (Mosaic Pixelation or Box Blur) on the bitmap buffer.
     */
    private fun applyPrivacyRedactionRegions(
        pixels: IntArray,
        w: Int,
        h: Int,
        regions: List<RedactionRegion>
    ) {
        val minDim = minOf(w, h).coerceAtLeast(1)
        for (region in regions) {
            val cx = (region.centerX * w).roundToInt()
            val cy = (region.centerY * h).roundToInt()
            val radPx = (region.radius * minDim).roundToInt().coerceAtLeast(8)
            val x0 = (cx - radPx).coerceIn(0, w - 1)
            val x1 = (cx + radPx).coerceIn(0, w - 1)
            val y0 = (cy - radPx).coerceIn(0, h - 1)
            val y1 = (cy + radPx).coerceIn(0, h - 1)

            val blockSize = (4 + (region.strength / 100f) * (minDim * 0.045f)).roundToInt().coerceAtLeast(4)
            var by = y0
            while (by <= y1) {
                var bx = x0
                while (bx <= x1) {
                    val dx = (bx + blockSize / 2) - cx
                    val dy = (by + blockSize / 2) - cy
                    if (dx * dx + dy * dy <= radPx * radPx) {
                        var sumR = 0
                        var sumG = 0
                        var sumB = 0
                        var cnt = 0
                        val endY = minOf(by + blockSize - 1, y1)
                        val endX = minOf(bx + blockSize - 1, x1)
                        for (py in by..endY) {
                            for (px in bx..endX) {
                                val c = pixels[py * w + px]
                                sumR += (c ushr 16) and 0xFF
                                sumG += (c ushr 8) and 0xFF
                                sumB += c and 0xFF
                                cnt++
                            }
                        }
                        if (cnt > 0) {
                            val avgColor = (0xFF shl 24) or ((sumR / cnt) shl 16) or ((sumG / cnt) shl 8) or (sumB / cnt)
                            for (py in by..endY) {
                                for (px in bx..endX) {
                                    pixels[py * w + px] = avgColor
                                }
                            }
                        }
                    }
                    bx += blockSize
                }
                by += blockSize
            }
        }
    }

    private fun computeSelectiveMaskWeight(nx: Float, ny: Float, recipe: PhotoEditRecipe): Float {
        val feather = (recipe.maskFeather / 100f).coerceIn(0.05f, 1f)
        val rawWeight = when (recipe.maskType) {
            SelectiveMaskType.NONE -> 0f
            SelectiveMaskType.RADIAL_SPOTLIGHT -> {
                val dist = hypot((nx - recipe.maskCenterX).toDouble(), (ny - recipe.maskCenterY).toDouble()).toFloat()
                val inner = recipe.maskRadius * (1f - feather * 0.65f)
                val outer = recipe.maskRadius * (1f + feather * 0.65f)
                val t = ((dist - inner) / (outer - inner).coerceAtLeast(0.02f)).coerceIn(0f, 1f)
                1f - t * t * (3f - 2f * t)
            }
            SelectiveMaskType.LINEAR_GRADIENT -> {
                val dist = ny - recipe.maskCenterY
                val span = (recipe.maskRadius * feather).coerceAtLeast(0.05f)
                val t = ((dist + span) / (2f * span)).coerceIn(0f, 1f)
                1f - t * t * (3f - 2f * t)
            }
        }
        return if (recipe.maskInvert) 1f - rawWeight else rawWeight
    }

    private fun applyFilmStockPixel(r: Float, g: Float, b: Float, stock: FilmStockPreset): FloatArray {
        val y = (0.299f * r + 0.587f * g + 0.114f * b).coerceIn(0f, 1f)
        return when (stock) {
            FilmStockPreset.ORIGINAL -> floatArrayOf(r, g, b)
            FilmStockPreset.KODAK_PORTRA_400 -> floatArrayOf(
                (r * 1.05f + 0.02f).coerceIn(0f, 1f),
                (g * 1.01f + 0.01f).coerceIn(0f, 1f),
                (b * 0.94f + 0.03f * (1f - y)).coerceIn(0f, 1f)
            )
            FilmStockPreset.KODAK_GOLD_200 -> floatArrayOf(
                (r * 1.10f + 0.03f).coerceIn(0f, 1f),
                (g * 1.03f + 0.01f).coerceIn(0f, 1f),
                (b * 0.86f).coerceIn(0f, 1f)
            )
            FilmStockPreset.KODAK_EKTAR_100 -> {
                val cR = 0.5f + (r - 0.5f) * 1.18f
                val cG = 0.5f + (g - 0.5f) * 1.12f
                val cB = 0.5f + (b - 0.5f) * 1.15f
                floatArrayOf(cR.coerceIn(0f, 1f), cG.coerceIn(0f, 1f), cB.coerceIn(0f, 1f))
            }
            FilmStockPreset.FUJI_VELVIA_50 -> {
                val satR = y + (r - y) * 1.38f
                val satG = y + (g - y) * 1.35f
                val satB = y + (b - y) * 1.35f
                floatArrayOf(satR.coerceIn(0f, 1f), satG.coerceIn(0f, 1f), satB.coerceIn(0f, 1f))
            }
            FilmStockPreset.FUJI_PRO_400H -> floatArrayOf(
                (r * 0.97f + 0.02f).coerceIn(0f, 1f),
                (g * 1.04f + 0.02f).coerceIn(0f, 1f),
                (b * 1.05f + 0.02f).coerceIn(0f, 1f)
            )
            FilmStockPreset.FUJI_CLASSIC_CHROME -> {
                val desatR = y + (r - y) * 0.78f
                val desatG = y + (g - y) * 0.82f
                val desatB = y + (b - y) * 0.76f
                floatArrayOf(
                    (0.5f + (desatR - 0.5f) * 1.14f).coerceIn(0f, 1f),
                    (0.5f + (desatG - 0.5f) * 1.12f).coerceIn(0f, 1f),
                    (0.5f + (desatB - 0.5f) * 1.08f).coerceIn(0f, 1f)
                )
            }
            FilmStockPreset.CINESTILL_800T -> floatArrayOf(
                (r * 0.94f + 0.12f * y * y).coerceIn(0f, 1f),
                (g * 1.01f + 0.02f * (1f - y)).coerceIn(0f, 1f),
                (b * 1.14f + 0.05f * (1f - y)).coerceIn(0f, 1f)
            )
            FilmStockPreset.ILFORD_HP5_BW -> {
                val bw = (0.04f + 0.92f * y.pow(0.92f)).coerceIn(0f, 1f)
                floatArrayOf(bw, bw, bw)
            }
            FilmStockPreset.KODAK_TRI_X_400 -> {
                val sCurve = (y * y * (3f - 2f * y)).coerceIn(0f, 1f)
                floatArrayOf(sCurve, sCurve, sCurve)
            }
            FilmStockPreset.LEICA_MONOCHROM -> {
                val redFilteredBw = (0.52f * r + 0.38f * g + 0.10f * b).coerceIn(0f, 1f)
                val crisp = (0.5f + (redFilteredBw - 0.5f) * 1.22f).coerceIn(0f, 1f)
                floatArrayOf(crisp, crisp, crisp)
            }
            FilmStockPreset.TEAL_ORANGE_CINE -> floatArrayOf(
                (r + 0.14f * y - 0.08f * (1f - y)).coerceIn(0f, 1f),
                (g + 0.03f * y + 0.03f * (1f - y)).coerceIn(0f, 1f),
                (b - 0.12f * y + 0.16f * (1f - y)).coerceIn(0f, 1f)
            )
            FilmStockPreset.BLEACH_BYPASS -> {
                val silver = y * y * (3f - 2f * y)
                val mixR = 0.55f * silver + 0.45f * r
                val mixG = 0.55f * silver + 0.45f * g
                val mixB = 0.55f * silver + 0.45f * b
                floatArrayOf(mixR.coerceIn(0f, 1f), mixG.coerceIn(0f, 1f), mixB.coerceIn(0f, 1f))
            }
            FilmStockPreset.ELECTRIC_SLATE_NOCTURNE -> floatArrayOf(
                (r * 0.92f + 0.06f * (1f - y)).coerceIn(0f, 1f),
                (g * 0.95f + 0.04f * (1f - y)).coerceIn(0f, 1f),
                (b * 1.12f + 0.10f * (1f - y)).coerceIn(0f, 1f)
            )
            FilmStockPreset.WARM_GOLDEN_HOUR -> floatArrayOf(
                (r * 1.14f + 0.04f).coerceIn(0f, 1f),
                (g * 1.04f + 0.02f).coerceIn(0f, 1f),
                (b * 0.84f).coerceIn(0f, 1f)
            )
            FilmStockPreset.MATTE_ANALOG_FADE -> floatArrayOf(
                (0.08f + r * 0.86f).coerceIn(0f, 1f),
                (0.07f + g * 0.86f).coerceIn(0f, 1f),
                (0.09f + b * 0.85f).coerceIn(0f, 1f)
            )
        }
    }

    /**
     * Computes live 64-bin R, G, B, Luma histogram + clipping percentages directly from a Bitmap.
     */
    fun computeLiveHistogram(bitmap: Bitmap): RgbHistogramData {
        val bins = 64
        val rHist = FloatArray(bins)
        val gHist = FloatArray(bins)
        val bHist = FloatArray(bins)
        val yHist = FloatArray(bins)

        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)
        val stepX = (w / 96).coerceAtLeast(1)
        val stepY = (h / 96).coerceAtLeast(1)

        var totalSamples = 0
        var shadowClip = 0
        var highlightClip = 0

        var py = 0
        while (py < h) {
            var px = 0
            while (px < w) {
                val c = bitmap.getPixel(px, py)
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val luma = ((r * 77 + g * 150 + b * 29) ushr 8).coerceIn(0, 255)

                rHist[(r * (bins - 1)) / 255] += 1f
                gHist[(g * (bins - 1)) / 255] += 1f
                bHist[(b * (bins - 1)) / 255] += 1f
                yHist[(luma * (bins - 1)) / 255] += 1f

                if (luma <= 3) shadowClip++
                if (luma >= 252) highlightClip++
                totalSamples++
                px += stepX
            }
            py += stepY
        }

        val maxPeak = maxOf(
            rHist.maxOrNull() ?: 1f,
            gHist.maxOrNull() ?: 1f,
            bHist.maxOrNull() ?: 1f,
            yHist.maxOrNull() ?: 1f
        ).coerceAtLeast(1f)

        for (i in 0 until bins) {
            rHist[i] = (rHist[i] / maxPeak).coerceIn(0f, 1f)
            gHist[i] = (gHist[i] / maxPeak).coerceIn(0f, 1f)
            bHist[i] = (bHist[i] / maxPeak).coerceIn(0f, 1f)
            yHist[i] = (yHist[i] / maxPeak).coerceIn(0f, 1f)
        }

        val denom = totalSamples.coerceAtLeast(1).toFloat()
        return RgbHistogramData(
            redBins = rHist,
            greenBins = gHist,
            blueBins = bHist,
            lumaBins = yHist,
            shadowClipPercent = (shadowClip / denom) * 100f,
            highlightClipPercent = (highlightClip / denom) * 100f,
            dynamicRangeEv = 11.4f
        )
    }

    /**
     * Decodes a source MediaItem at full or preview resolution, applies the complete PhotoEditRecipe,
     * and writes the exported image to Android MediaStore (Pictures/Photos) + app fallback directory.
     */
    fun exportEditedPhoto(
        context: Context,
        sourceItem: MediaItem,
        recipe: PhotoEditRecipe,
        exportFormat: PhotoExportFormat,
        preserveExif: Boolean
    ): PhotoExportResult {
        val rawBitmap: Bitmap = try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val srcFile = File(sourceItem.filePath)
            if (srcFile.exists()) {
                BitmapFactory.decodeFile(srcFile.absolutePath, opts)
            } else {
                context.contentResolver.openInputStream(Uri.parse(sourceItem.uriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            }
        } catch (_: OutOfMemoryError) {
            val fallbackOpts = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val srcFile = File(sourceItem.filePath)
            if (srcFile.exists()) {
                BitmapFactory.decodeFile(srcFile.absolutePath, fallbackOpts)
            } else {
                context.contentResolver.openInputStream(Uri.parse(sourceItem.uriString))?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, fallbackOpts)
                }
            }
        } ?: return PhotoExportResult(false, null, "Could not decode source image.")

        val editedBitmap = processBitmap(rawBitmap, recipe)
        val compressFormat = when (exportFormat) {
            PhotoExportFormat.JPEG_95, PhotoExportFormat.JPEG_100 -> Bitmap.CompressFormat.JPEG
            PhotoExportFormat.PNG_LOSSLESS -> Bitmap.CompressFormat.PNG
            PhotoExportFormat.WEBP_HQ -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
        val quality = when (exportFormat) {
            PhotoExportFormat.JPEG_95 -> 95
            PhotoExportFormat.JPEG_100 -> 100
            PhotoExportFormat.PNG_LOSSLESS -> 100
            PhotoExportFormat.WEBP_HQ -> 95
        }

        val cleanBase = sourceItem.title.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_").take(32).ifBlank { "Photo" }
        val timestampSec = System.currentTimeMillis() / 1000L
        val fileName = "${cleanBase}_Edited_${timestampSec}.${exportFormat.extension}"

        // Save to local app Pictures directory + register in Android MediaStore
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: File(context.filesDir, "EditedPhotos")
        picturesDir.mkdirs()
        val localFile = File(picturesDir, fileName)
        FileOutputStream(localFile).use { fos ->
            editedBitmap.compress(compressFormat, quality, fos)
            fos.flush()
        }

        if (preserveExif && (exportFormat == PhotoExportFormat.JPEG_95 || exportFormat == PhotoExportFormat.JPEG_100)) {
            copyBasicExifIfPresent(File(sourceItem.filePath), localFile)
        }

        var mediaStoreUri: Uri? = null
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, exportFormat.mimeType)
                put(MediaStore.Images.Media.WIDTH, editedBitmap.width)
                put(MediaStore.Images.Media.HEIGHT, editedBitmap.height)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Photos")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (inserted != null) {
                resolver.openOutputStream(inserted)?.use { outStream ->
                    editedBitmap.compress(compressFormat, quality, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val readyValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    resolver.update(inserted, readyValues, null, null)
                }
                mediaStoreUri = inserted
            }
        } catch (_: Throwable) {
        }

        val finalUriString = mediaStoreUri?.toString() ?: Uri.fromFile(localFile).toString()
        val exportedMediaItem = sourceItem.copy(
            id = "edited_${System.currentTimeMillis()}",
            title = "${sourceItem.title} (Edited)",
            uriString = finalUriString,
            filePath = localFile.absolutePath,
            mimeType = exportFormat.mimeType,
            extension = exportFormat.extension.uppercase(),
            mediaType = MediaType.PHOTO,
            sizeBytes = localFile.length().coerceAtLeast(1024L),
            width = editedBitmap.width,
            height = editedBitmap.height,
            dateAddedEpochSec = timestampSec
        )

        return PhotoExportResult(
            success = true,
            exportedItem = exportedMediaItem,
            message = "Saved ${editedBitmap.width}x${editedBitmap.height} ${exportFormat.label} (${recipe.activeParameterCount()} edits applied)"
        )
    }

    private fun copyBasicExifIfPresent(srcFile: File, dstFile: File) {
        if (!srcFile.exists() || !dstFile.exists()) return
        try {
            val srcExif = ExifInterface(srcFile.absolutePath)
            val dstExif = ExifInterface(dstFile.absolutePath)
            val tags = listOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_WHITE_BALANCE
            )
            for (tag in tags) {
                val v = srcExif.getAttribute(tag)
                if (!v.isNullOrBlank()) {
                    dstExif.setAttribute(tag, v)
                }
            }
            dstExif.saveAttributes()
        } catch (_: Exception) {
        }
    }
}
