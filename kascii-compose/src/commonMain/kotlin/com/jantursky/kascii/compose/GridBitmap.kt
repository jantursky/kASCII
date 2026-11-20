package com.jantursky.kascii.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.jantursky.kascii.compose.internal.CellMetrics
import com.jantursky.kascii.compose.internal.FontRegistry
import com.jantursky.kascii.compose.internal.GlyphRenderer
import com.jantursky.kascii.compose.internal.GridLayout
import com.jantursky.kascii.compose.internal.LINE_HEIGHT_EM
import com.jantursky.kascii.compose.platform.standaloneFontResolver
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Rasterises this grid to a bitmap with the bundled font, for PNG export or sharing.
 *
 * The bundled font is available once any Kascii composable or [kasciiFontFamily] has loaded it; before that the
 * platform monospace font draws. Android can only draw text with a context, so there this throws
 * [IllegalStateException] until a Kascii composable has been shown.
 */
public fun Grid.toImageBitmap(
    theme: KasciiTheme = KasciiTheme.Dark,
    cellHeightPx: Int = 32,
): ImageBitmap {
    require(cols > 0 && rows > 0) { "Cannot draw an empty grid" }
    require(cellHeightPx > 0) { "Cell height must be positive" }
    val resolver =
        FontRegistry.resolver ?: standaloneFontResolver()
            ?: error("Show a Kascii composable before drawing grids to bitmaps on this platform")
    val font = FontRegistry.family ?: FontFamily.Monospace
    val renderer = GlyphRenderer()
    val fontSize = cellHeightPx / LINE_HEIGHT_EM
    val width = max(1, (fontSize * renderer.advanceEm(font, resolver)).roundToInt())
    val layout = GridLayout(CellMetrics(width, cellHeightPx, fontSize), cols, rows)
    val image = ImageBitmap(cols * width, rows * cellHeightPx)
    val size = Size(image.width.toFloat(), image.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), size) {
        renderer.draw(this, this@toImageBitmap, layout, font, resolver, theme, AsciiRenderer.Cpu)
    }
    return image
}
