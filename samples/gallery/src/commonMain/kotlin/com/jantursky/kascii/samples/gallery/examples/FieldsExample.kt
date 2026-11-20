package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jantursky.kascii.compose.AsciiCanvas
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Field
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion

enum class FieldKind(
    val label: String,
) {
    Plasma("Plasma"),
    Noise("Noise"),
    Waves("Waves"),
    Ripples("Ripples"),
}

fun FieldKind.field(seed: Int): Field =
    when (this) {
        FieldKind.Plasma -> Fields.plasma()
        FieldKind.Noise -> Fields.noise(seed = seed)
        FieldKind.Waves -> Fields.waves(count = 4)
        FieldKind.Ripples -> Fields.ripples()
    }

// A field says how strong each cell is, from 0 to 1. The charset turns that into glyphs and the color mode into
// colors. stillTime is the frame shown when nothing moves: a moment that looks finished.
@Composable
fun FieldsExample(
    kind: FieldKind,
    seed: Int,
    charset: Charset,
    colorMode: ColorMode,
    cell: Dp,
    speed: Float,
    motion: Motion,
    theme: KasciiTheme,
    modifier: Modifier = Modifier,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
) {
    ProvideKasciiTheme(theme) {
        AsciiCanvas(
            field = kind.field(seed),
            modifier = modifier,
            charset = charset,
            cell = cell,
            colorMode = colorMode,
            speed = speed,
            stillTime = 1.5f,
            motion = motion,
            renderer = renderer,
            contentDescription = "${kind.label} field drawn with ${charset.name} glyphs",
        )
    }
}
