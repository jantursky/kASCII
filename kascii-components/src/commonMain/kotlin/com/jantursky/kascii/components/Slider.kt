package com.jantursky.kascii.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.roundToInt

/**
 * A slider drawn as `[====O-----]`. Drag or tap the track, or focus it and use the arrow keys, Home and End.
 * [steps] is the number of stops between the ends, like Material's slider (0 for a continuous slider).
 */
@Composable
public fun AsciiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    cols: Int = 24,
    enabled: Boolean = true,
    label: String = "Slider",
) {
    val source = remember { MutableInteractionSource() }
    val state = rememberControlState(source, enabled)
    val focus = remember { FocusRequester() }
    val currentOnChange by rememberUpdatedState(onValueChange)
    val fraction = fractionOf(value, valueRange)
    val cellPx = with(LocalDensity.current) { CellWidth.toPx() }
    var boxWidth by remember { mutableIntStateOf(0) }
    val inner = sliderTrack(cols)

    fun commit(next: Float) = currentOnChange(valueAt(snapFraction(next, steps), valueRange))

    fun commitAt(xPx: Float) = commit(fractionAtCell((xPx - (boxWidth - cols * cellPx) / 2) / cellPx, inner))

    ControlGrid(
        grid = renderSlider(cols, fraction, state, LocalKasciiTheme.current),
        state = state,
        modifier =
            modifier
                .onSizeChanged { boxWidth = it.width }
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                    stateDescription = "${percentOf(fraction)}%"
                    if (!enabled) disabled()
                    setProgress { target -> enabled && commit(fractionOf(target, valueRange)).let { true } }
                }.onKeyEvent { event ->
                    val down = enabled && event.type == KeyEventType.KeyDown
                    val target = if (down) keyTarget(event.key, fraction, steps) else null
                    target?.let(::commit) != null
                }.focusRequester(focus)
                .focusable(enabled, source)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                focus.requestFocus()
                                commitAt(it.x)
                            },
                        )
                    }
                }.pointerInput(enabled) {
                    if (enabled) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            commitAt(change.position.x)
                        }
                    }
                },
    )
}

internal fun renderSlider(
    cols: Int,
    fraction: Float,
    state: ControlState,
    theme: KasciiTheme,
): Grid {
    val inner = sliderTrack(cols)
    val grid = Grid(cols, 1)
    val color = if (state.enabled) null else theme.dim()
    val thumb = (fraction.coerceIn(0f, 1f) * (inner - 1)).roundToInt()
    grid.put(1, 0, '[', color)
    grid.put(inner + 2, 0, ']', color)
    for (i in 0 until inner) {
        val glyph =
            when {
                i == thumb -> 'O'
                i < thumb -> '='
                else -> '-'
            }
        grid.put(2 + i, 0, glyph, if (i <= thumb && state.enabled) theme.good() else color)
    }
    if (state.focused) grid.put(0, 0, '>', theme.accent)
    return grid
}

/** Track cells between the brackets: the grid has a focus column and two brackets besides. */
internal fun sliderTrack(cols: Int): Int = (cols - 3).coerceAtLeast(2)

/** The fraction for a pointer at [x] cells from the grid's left edge. The thumb centers on its cell. */
internal fun fractionAtCell(
    x: Float,
    inner: Int,
): Float = ((x - 2f - 0.5f) / (inner - 1)).coerceIn(0f, 1f)

internal fun fractionOf(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
): Float {
    val span = range.endInclusive - range.start
    return if (span <= 0f) 0f else ((value - range.start) / span).coerceIn(0f, 1f)
}

internal fun valueAt(
    fraction: Float,
    range: ClosedFloatingPointRange<Float>,
): Float = range.start + (range.endInclusive - range.start) * fraction.coerceIn(0f, 1f)

internal fun snapFraction(
    fraction: Float,
    steps: Int,
): Float {
    val clamped = fraction.coerceIn(0f, 1f)
    return if (steps <= 0) clamped else (clamped * (steps + 1)).roundToInt() / (steps + 1f)
}

/** Where a key sends the thumb from [fraction], or null when the key is not a slider key. */
internal fun keyTarget(
    key: Key,
    fraction: Float,
    steps: Int,
): Float? {
    val step = if (steps > 0) 1f / (steps + 1) else KEY_STEP
    return when (key) {
        Key.DirectionRight, Key.DirectionUp -> fraction + step
        Key.DirectionLeft, Key.DirectionDown -> fraction - step
        Key.MoveHome -> 0f
        Key.MoveEnd -> 1f
        else -> null
    }
}

private const val KEY_STEP = 0.05f
