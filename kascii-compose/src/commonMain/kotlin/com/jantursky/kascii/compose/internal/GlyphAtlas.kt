package com.jantursky.kascii.compose.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Grid
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** Where each of [count] glyphs sits in a near-square atlas of [cellWidth] by [cellHeight] slots. */
internal class AtlasLayout(
    val count: Int,
    val cellWidth: Int,
    val cellHeight: Int,
) {
    val columns: Int = max(1, ceil(sqrt(count.toDouble())).toInt())
    val rows: Int = max(1, (count + columns - 1) / columns)
    val width: Int get() = columns * cellWidth
    val height: Int get() = rows * cellHeight

    fun offset(index: Int): IntOffset = IntOffset((index % columns) * cellWidth, (index / columns) * cellHeight)
}

/** White glyphs on transparent, one per slot. Spaces are never drawn, so they have no slot. */
internal class GlyphAtlas(
    val glyphs: String,
    val metrics: CellMetrics,
    val font: FontFamily,
    val image: ImageBitmap,
) {
    val layout: AtlasLayout = AtlasLayout(glyphs.length, metrics.width, metrics.height)
    private val slots = HashMap<Char, Int>(glyphs.length * 2).apply { glyphs.forEachIndexed { i, c -> put(c, i) } }

    /** The slot of [glyph], or -1 when the atlas lacks it. */
    fun slotOf(glyph: Char): Int = slots[glyph] ?: -1

    fun covers(grid: Grid): Boolean = grid.glyphs.all { it == ' ' || it in slots }
}

/** Every glyph [grid] shows, without spaces, sorted and distinct. */
internal fun glyphsOf(
    grid: Grid,
    extra: String = "",
): String =
    (grid.glyphs.asSequence() + extra.asSequence())
        .filter { it != ' ' }
        .distinct()
        .sorted()
        .joinToString("")

/** Every glyph a charset can produce, so a field's atlas is built once instead of growing frame by frame. */
internal fun Charset.glyphSet(): String =
    when (this) {
        is Charset.Ramp -> {
            glyphs
        }

        is Charset.Mask -> {
            val n = subX * subY
            (0 until (1 shl n))
                .map { mask -> glyphFor(FloatArray(n) { if (mask shr it and 1 == 1) 1f else 0f }) }
                .joinToString("")
        }

        is Charset.Edge -> {
            base.glyphs + "-/\\|"
        }
    }

/** Draws [glyphs] into a new atlas. Each glyph is clipped to its slot so wide fallback glyphs cannot bleed. */
internal fun buildAtlas(
    glyphs: String,
    metrics: CellMetrics,
    font: FontFamily,
    measurer: TextMeasurer,
): GlyphAtlas {
    val layout = AtlasLayout(glyphs.length, metrics.width, metrics.height)
    val image = ImageBitmap(layout.width, layout.height)
    val style = glyphStyle(font, metrics).copy(color = Color.White)
    val w = metrics.width.toFloat()
    val h = metrics.height.toFloat()
    val canvasSize = Size(layout.width.toFloat(), layout.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), canvasSize) {
        glyphs.forEachIndexed { i, glyph ->
            val o = layout.offset(i)
            clipRect(o.x.toFloat(), o.y.toFloat(), o.x + w, o.y + h) {
                drawText(measurer, glyph.toString(), Offset(o.x.toFloat(), o.y.toFloat()), style)
            }
        }
    }
    return GlyphAtlas(glyphs, metrics, font, image)
}

/**
 * Keeps one atlas and grows it when a frame shows a glyph it lacks. A new cell size or font starts over, so the
 * atlas is built once per font, cell size and glyph set.
 */
internal class AtlasCache {
    private var atlas: GlyphAtlas? = null

    fun atlasFor(
        grid: Grid,
        extra: String,
        metrics: CellMetrics,
        font: FontFamily,
        measurer: TextMeasurer,
    ): GlyphAtlas {
        val current = atlas?.takeIf { it.metrics == metrics && it.font == font }
        if (current != null && current.covers(grid)) return current
        val built = buildAtlas(glyphsOf(grid, extra + current?.glyphs.orEmpty()), metrics, font, measurer)
        atlas = built
        return built
    }
}
