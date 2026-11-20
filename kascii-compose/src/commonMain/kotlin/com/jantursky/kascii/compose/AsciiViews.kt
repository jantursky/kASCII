package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.internal.AsciiView
import com.jantursky.kascii.compose.internal.ClockSpec
import com.jantursky.kascii.compose.internal.DefaultCell
import com.jantursky.kascii.compose.internal.FieldSource
import com.jantursky.kascii.compose.internal.PieceSource
import com.jantursky.kascii.compose.internal.StaticSource
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Field
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion

/**
 * Draws a procedural [field] as glyphs. Animates while visible, pauses offscreen and in the background, and holds
 * a still frame under reduced motion. [cell] is the width of one glyph cell.
 */
@Composable
public fun AsciiCanvas(
    field: Field,
    modifier: Modifier = Modifier,
    charset: Charset = Charsets.Standard,
    cell: Dp = 8.dp,
    colorMode: ColorMode = ColorMode.Theme,
    speed: Float = 1f,
    stillTime: Float = 0f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    val clock = ClockSpec(running = !rememberReducedMotion(motion), speed = speed, still = stillTime)
    AsciiView(FieldSource(field, charset, colorMode), cell, clock, renderer, theme, contentDescription, modifier)
}

/**
 * Plays an [AsciiPiece]. The grid is [AsciiPiece.meta]'s size, scaled to fit [modifier]. Pauses offscreen, honours
 * reduced motion by showing the piece's still frame.
 */
@Composable
public fun AsciiPieceView(
    piece: AsciiPiece,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    val meta = piece.meta
    val clock =
        ClockSpec(
            running = meta.fps > 0 && !rememberReducedMotion(motion),
            speed = speed,
            still = meta.still,
            loop = meta.loop,
        )
    AsciiView(PieceSource(piece), DefaultCell, clock, renderer, theme, contentDescription, modifier)
}

/** Draws a static [grid] once. Use [renderer] = [AsciiRenderer.Text] for selectable text. */
@Composable
public fun AsciiGridView(
    grid: Grid,
    modifier: Modifier = Modifier,
    renderer: AsciiRenderer = AsciiRenderer.Cpu,
    theme: KasciiTheme = LocalKasciiTheme.current,
    contentDescription: String? = null,
) {
    val still = ClockSpec(running = false)
    AsciiView(StaticSource(grid), DefaultCell, still, renderer, theme, contentDescription, modifier)
}
