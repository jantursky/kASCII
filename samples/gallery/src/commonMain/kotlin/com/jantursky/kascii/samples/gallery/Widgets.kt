package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The smallest touch target, in dp. */
private const val TARGET = 48

/** The accent a pill takes: a 20% tint of its fill with text in the accent color. */
enum class Tint { Green, Amber, Red }

/** A click target of at least 48dp that shows a focus ring when it has keyboard focus. */
@Composable
fun Modifier.pressable(
    role: Role,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val ring = if (focused) Modifier.border(2.dp, Gallery.colors.link, shape) else Modifier
    val wash = Gallery.colors.link.copy(alpha = HOVER_WASH)
    return this
        .defaultMinSize(TARGET.dp, TARGET.dp)
        .then(ring)
        .clip(shape)
        .hoverable(source)
        .pointerHoverIcon(PointerIcon.Hand)
        .drawWithContent {
            drawContent()
            // Drawn over whatever the target is made of, so every kind of target answers the pointer the same way.
            if (hovered) drawRect(wash)
        }.clickable(interactionSource = source, indication = null, role = role, onClick = onClick)
}

private const val HOVER_WASH = 0.14f

/** A pill button: a 20% tint of the accent, accent text and a 10% white border. */
@Composable
fun Pill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Tint = Tint.Green,
) {
    val colors = Gallery.colors
    val (fill, ink) =
        when (tint) {
            Tint.Green -> colors.greenFill to colors.green
            Tint.Amber -> colors.amberFill to colors.amber
            Tint.Red -> colors.redFill to colors.red
        }
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .pressable(Role.Button, shape, onClick)
            .background(colors.surface1, shape)
            .background(fill, shape)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Gallery.type.caption.copy(color = ink), maxLines = 1, softWrap = false)
    }
}

/** Flat link text in the link color. */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.pressable(Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Gallery.type.body.copy(color = Gallery.colors.link), maxLines = 1, softWrap = false)
    }
}

/** A label above a control, with the current value on the right when there is one. */
@Composable
fun Labeled(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(label, style = Gallery.type.caption.copy(color = Gallery.colors.muted))
            if (value != null) BasicText(value, style = Gallery.type.caption.copy(color = Gallery.colors.text))
        }
        content()
    }
}

/** One choice out of a few, as a wrapping group of chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    val shape = RoundedCornerShape(percent = 50)
    FlowRow(
        modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .pressable(Role.RadioButton, shape) { onSelect(option) }
                    .semantics { this.selected = on }
                    .background(if (on) colors.surface3 else colors.surface1, shape)
                    .border(1.dp, if (on) colors.green else colors.border, shape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    label(option),
                    style = Gallery.type.caption.copy(color = if (on) colors.strong else colors.text),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/** A slider with no Material: a track, a fill and a thumb, dragged, tapped or moved with the arrow keys. */
@Composable
fun GallerySlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    description: String,
    modifier: Modifier = Modifier,
    step: Float = 0f,
) {
    val colors = Gallery.colors
    var width by remember { mutableIntStateOf(1) }
    val span = range.endInclusive - range.start
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
    val keyStep = if (step > 0f) step else span / 20f
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()

    fun snap(raw: Float): Float {
        val stepped = if (step > 0f) range.start + ((raw - range.start) / step).roundToInt() * step else raw
        return stepped.coerceIn(range.start, range.endInclusive)
    }

    fun at(x: Float): Float = snap(range.start + (x / width).coerceIn(0f, 1f) * span)
    Box(
        modifier
            .height(TARGET.dp)
            .fillMaxWidth()
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, range)
                setProgress {
                    onChange(snap(it))
                    true
                }
            }.focusable(interactionSource = source)
            .onKeyEvent { event ->
                val direction =
                    when (event.key) {
                        Key.DirectionRight, Key.DirectionUp -> 1
                        Key.DirectionLeft, Key.DirectionDown -> -1
                        else -> 0
                    }
                if (direction == 0 || event.type != KeyEventType.KeyDown) {
                    false
                } else {
                    onChange(snap(value + direction * keyStep))
                    true
                }
            }.pointerInput(range, step) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange(at(down.position.x))
                    drag(down.id) {
                        onChange(at(it.position.x))
                        it.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumb = 20.dp
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.surface3),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.green),
        )
        Box(
            Modifier
                .offset { IntOffset((fraction * (width - thumb.roundToPx())).roundToInt(), 0) }
                .size(thumb)
                .clip(CircleShape)
                .background(colors.void)
                .border(if (focused) 3.dp else 2.dp, if (focused) colors.link else colors.green, CircleShape),
        )
    }
}

/** A labeled slider that shows its value. */
@Composable
fun SliderField(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    step: Float = 0f,
    format: (Float) -> String = { (it * 100).roundToInt().div(100f).toString() },
) {
    Labeled(label, modifier.width(220.dp), value = format(value)) {
        GallerySlider(value, onChange, range, description = label, step = step)
    }
}

/** An on or off switch with a label. */
@Composable
fun SwitchField(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Gallery.colors
    Row(
        modifier
            .pressable(Role.Switch) { onChange(!checked) }
            .semantics(mergeDescendants = true) { stateDescription = if (checked) "On" else "Off" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 40.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (checked) colors.green else colors.surface3)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(colors.void))
        }
        BasicText(label, style = Gallery.type.caption.copy(color = colors.text))
    }
}

/** A section heading: 30px text with a 16px amber icon. */
@Composable
fun SectionHeading(text: String) {
    val amber = Gallery.colors.amber
    Row(
        Modifier.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A prompt: a chevron and a cursor.
        Canvas(Modifier.size(16.dp)) {
            val width = 2.dp.toPx()
            val u = size.width / 16f
            drawLine(amber, Offset(2 * u, 3 * u), Offset(8 * u, 8 * u), width, StrokeCap.Round)
            drawLine(amber, Offset(8 * u, 8 * u), Offset(2 * u, 13 * u), width, StrokeCap.Round)
            drawLine(amber, Offset(10 * u, 13 * u), Offset(14 * u, 13 * u), width, StrokeCap.Round)
        }
        BasicText(text, style = Gallery.type.heading.copy(color = Gallery.colors.strong))
    }
}

@Composable
fun Gap(size: Int) = Spacer(Modifier.height(size.dp))
