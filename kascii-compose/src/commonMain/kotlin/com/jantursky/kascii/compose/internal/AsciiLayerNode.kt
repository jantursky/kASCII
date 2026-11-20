package com.jantursky.kascii.compose.internal

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.platform.loadBundledFont
import com.jantursky.kascii.compose.toColor
import com.jantursky.kascii.compose.toTones
import com.jantursky.kascii.compose.withSamples
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rasterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

internal data class AsciiLayerElement(
    val charset: Charset,
    val cell: Dp,
    val colorMode: ColorMode,
    val renderer: AsciiRenderer,
    val theme: KasciiTheme?,
    val live: Boolean = false,
) : ModifierNodeElement<AsciiLayerNode>() {
    val glyphs: String by lazy { charset.glyphSet() }

    override fun create(): AsciiLayerNode = AsciiLayerNode(this)

    override fun update(node: AsciiLayerNode) = node.update(this)

    override fun InspectorInfo.inspectableProperties() {
        name = "ascii"
        properties["charset"] = charset.name
        properties["cell"] = cell
        properties["renderer"] = renderer
        properties["live"] = live
    }
}

/**
 * Records the content into a layer instead of drawing it, samples the layer on a background thread and draws the
 * sampled grid. A draw this node asked for itself, to show a finished sample, does not sample again, so a still
 * content is sampled once and an animated one about as often as it changes.
 */
internal class AsciiLayerNode(
    private var spec: AsciiLayerElement,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var layer: GraphicsLayer? = null
    private val renderer = GlyphRenderer()
    private var sampled: Grid? = null
    private var sampledLayout: GridLayout? = null
    private var sampling: Job? = null
    private var changedWhileSampling = false
    private var showingSample = false

    override val shouldAutoInvalidate: Boolean get() = false

    private var ticker: Job? = null

    fun update(next: AsciiLayerElement) {
        spec = next
        syncTicker()
        invalidateDraw()
    }

    /** A live node asks for a redraw on every frame, because a child's own layer never redraws this node. */
    private fun syncTicker() {
        ticker?.cancel()
        ticker = null
        if (!spec.live || !isAttached) return
        ticker =
            coroutineScope.launch {
                while (true) {
                    withFrameNanos { }
                    invalidateDraw()
                }
            }
    }

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer()
        syncTicker()
        if (FontRegistry.family == null) {
            coroutineScope.launch { FontRegistry.register(loadBundledFont(), currentValueOf(LocalFontFamilyResolver)) }
        }
    }

    override fun onDetach() {
        ticker?.cancel()
        ticker = null
        sampling?.cancel()
        sampling = null
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        sampled = null
    }

    override fun ContentDrawScope.draw() {
        val layer = layer ?: return
        if (size.minDimension <= 0f) return
        val theme = spec.theme ?: currentValueOf(LocalKasciiTheme)
        val font = FontRegistry.family ?: FontFamily.Monospace
        val resolver = currentValueOf(LocalFontFamilyResolver)
        val metrics = cellMetrics(spec.cell.toPx(), renderer.advanceEm(font, resolver))
        val layout =
            GridLayout(
                metrics,
                ceil(size.width / metrics.width).toInt().coerceAtLeast(1),
                ceil(size.height / metrics.height).toInt().coerceAtLeast(1),
            )
        layer.record { this@draw.drawContent() }
        if (!showingSample) sample(layer, layout, theme)
        showingSample = false
        val grid = sampled
        if (grid != null && sampledLayout == layout) {
            val drawWith = if (spec.renderer == AsciiRenderer.Text) AsciiRenderer.Cpu else spec.renderer
            renderer.draw(this, grid, layout, font, resolver, theme, drawWith, spec.glyphs)
        } else {
            drawRect(theme.paper.toColor())
        }
    }

    private fun sample(
        layer: GraphicsLayer,
        layout: GridLayout,
        theme: KasciiTheme,
    ) {
        if (sampling?.isActive == true) {
            changedWhileSampling = true
            return
        }
        val charset = spec.charset
        val colorMode = spec.colorMode
        sampling =
            coroutineScope.launch {
                val bitmap = layer.toImageBitmap()
                sampled = withContext(Dispatchers.Default) { sampleGrid(bitmap, layout, charset, colorMode, theme) }
                sampledLayout = layout
                showingSample = !changedWhileSampling
                changedWhileSampling = false
                invalidateDraw()
            }
    }
}

/** Samples [bitmap] at the charset's sub-cell resolution and maps it to glyphs. */
internal fun sampleGrid(
    bitmap: ImageBitmap,
    layout: GridLayout,
    charset: Charset,
    colorMode: ColorMode,
    theme: KasciiTheme,
): Grid {
    val tones = bitmap.toTones(layout.cols * charset.subX, layout.rows * charset.subY, theme.paper.toColor())
    return Rasterizer.fromTones(
        tones.tones,
        charset,
        layout.cols,
        layout.rows,
        colorMode = colorMode.withSamples(tones),
        theme = theme,
    )
}
