package com.jantursky.kascii.compose.internal

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The line height in ems. It is the bundled font's ascent plus descent, so block glyphs tile without gaps. */
internal const val LINE_HEIGHT_EM = 1.32f

/** The advance of the bundled font in ems, used before a measurement is available. */
internal const val DEFAULT_ADVANCE_EM = 0.6f

private const val REFERENCE_PX = 100f
private const val SAMPLE = "0000000000"
private val UnitDensity = Density(1f)

/** One glyph cell in whole pixels, and the font size in pixels that fills it. */
internal data class CellMetrics(
    val width: Int,
    val height: Int,
    val fontSize: Float,
) {
    /** Cell height over width, the `cellAspect` the core rasterizer takes. */
    val aspect: Float get() = height.toFloat() / width
}

/** A cell [widthPx] wide for a font whose glyphs advance [advanceEm]. Both sides are whole pixels. */
internal fun cellMetrics(
    widthPx: Float,
    advanceEm: Float,
): CellMetrics {
    val width = max(1, widthPx.roundToInt())
    val fontSize = width / advanceEm
    return CellMetrics(width, max(1, (fontSize * LINE_HEIGHT_EM).roundToInt()), fontSize)
}

/** The largest whole-pixel cell that fits a [cols] by [rows] grid into [widthPx] by [heightPx]. */
internal fun fitCell(
    widthPx: Float,
    heightPx: Float,
    cols: Int,
    rows: Int,
    advanceEm: Float,
): CellMetrics {
    if (cols <= 0 || rows <= 0) return cellMetrics(1f, advanceEm)
    var width = max(1, floor(min(widthPx / cols, heightPx * advanceEm / (rows * LINE_HEIGHT_EM))).toInt())
    var metrics = cellMetrics(width.toFloat(), advanceEm)
    while (width > 1 && metrics.height * rows > heightPx) {
        width--
        metrics = cellMetrics(width.toFloat(), advanceEm)
    }
    return metrics
}

/** The glyph advance of [font] in ems, measured over a run of digits. */
internal fun TextMeasurer.advanceEm(font: FontFamily): Float {
    val layout = measure(SAMPLE, TextStyle(fontFamily = font, fontSize = REFERENCE_PX.sp), density = UnitDensity)
    val advance = layout.getLineRight(0) / SAMPLE.length / REFERENCE_PX
    return if (advance > 0f) advance else DEFAULT_ADVANCE_EM
}

/**
 * The text style that puts one glyph in one cell, for a measurer at density 1 where a scaled pixel is a pixel. The
 * line box is exactly the cell height and centred, so nothing depends on platform font padding.
 */
internal fun glyphStyle(
    font: FontFamily,
    metrics: CellMetrics,
): TextStyle =
    TextStyle(
        fontFamily = font,
        fontSize = metrics.fontSize.sp,
        lineHeight = metrics.height.sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        letterSpacing = 0.sp,
    )

/** A measurer that lays out in pixels, for atlases and bitmaps drawn outside any layout. */
internal fun pixelMeasurer(resolver: FontFamily.Resolver): TextMeasurer =
    TextMeasurer(resolver, UnitDensity, LayoutDirection.Ltr)
