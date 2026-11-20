package com.jantursky.kascii.compose.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class AsciiElement(
    val source: GridSource,
    val cell: Dp,
    val font: FontFamily,
    val theme: KasciiTheme,
    val renderer: AsciiRenderer,
    val clock: ClockSpec,
) : ModifierNodeElement<AsciiNode>() {
    override fun create(): AsciiNode = AsciiNode(this)

    override fun update(node: AsciiNode) = node.update(this)

    override fun InspectorInfo.inspectableProperties() {
        name = "ascii"
        properties["renderer"] = renderer
        properties["running"] = clock.running
    }
}

/**
 * Draws a [GridSource]. The clock lives here as state that only the draw block reads, so frames redraw without
 * recomposing. It runs only while the node is attached, its spec is running and some of it is on screen.
 */
internal class AsciiNode(
    private var spec: AsciiElement,
) : Modifier.Node(),
    LayoutModifierNode,
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private val clock = AsciiClock(spec.clock.still)
    private val renderer = GlyphRenderer()
    private var job: Job? = null
    private var onScreen = false
    private var grid: Grid? = null

    override val shouldAutoInvalidate: Boolean get() = false

    fun update(next: AsciiElement) {
        val clockChanged = next.clock != spec.clock
        val sizeChanged = next.source != spec.source || next.cell != spec.cell || next.font != spec.font
        if (next.source != spec.source) grid = null
        spec = next
        if (clockChanged) {
            if (next.clock.running == clock.spec.running) clock.spec = next.clock else restartClock()
        }
        if (sizeChanged) invalidateMeasurement()
        invalidateDraw()
    }

    override fun onAttach() = restartClock()

    override fun onDetach() {
        job?.cancel()
        job = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val visible = coordinates.isOnScreen()
        if (visible == onScreen) return
        onScreen = visible
        restartClock()
    }

    private fun restartClock() {
        job?.cancel()
        job = null
        val clockSpec = spec.clock
        clock.spec = clockSpec
        clock.settle(clockSpec)
        if (clockSpec.running && onScreen && isAttached) job = coroutineScope.launch { clock.run() }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val advance = renderer.advanceEm(spec.font, currentValueOf(LocalFontFamilyResolver))
        val preferred = spec.source.preferredSize(advance, spec.cell.toPx())
        val width = constraints.constrainWidth(preferred.width)
        val height = constraints.constrainHeight(preferred.height)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        val resolver = currentValueOf(LocalFontFamilyResolver)
        val advance = renderer.advanceEm(spec.font, resolver)
        val layout = spec.source.layout(size, advance, spec.cell.toPx())
        val frame = spec.source.frame(clock.time, layout, spec.theme, grid)
        grid = frame
        renderer.draw(this, frame, layout, spec.font, resolver, spec.theme, spec.renderer, spec.source.glyphs)
    }
}
