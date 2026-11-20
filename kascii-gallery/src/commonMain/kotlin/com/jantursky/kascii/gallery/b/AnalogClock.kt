package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.TAU
import com.jantursky.kascii.gallery.b.utcSecondsOfDay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COLS = 47
private const val ROWS = 23
private const val RADIUS = 20f
private const val CX = COLS / 2
private const val CY = ROWS / 2

/** The glyph that follows a stroke pointing at clock [angle] (radians clockwise from twelve). */
private fun strokeGlyph(angle: Float): Char {
    val dx = sin(angle)
    val dy = -cos(angle)
    return when {
        abs(dx) < 0.38f -> '|'
        abs(dy) < 0.38f -> '-'
        dx * dy < 0f -> '/'
        else -> '\\'
    }
}

private fun Grid.at(angle: Float, radius: Float, glyph: Char, color: Rgb? = null) {
    put((CX + radius * sin(angle)).roundToInt(), (CY - radius * cos(angle) / 2f).roundToInt(), glyph, color)
}

private fun Grid.hand(angle: Float, from: Float, to: Float, color: Rgb?) {
    val glyph = strokeGlyph(angle)
    var r = from
    while (r <= to) {
        at(angle, r, glyph, color)
        r += 0.5f
    }
}

private fun Grid.face() {
    for (i in 0 until 720) {
        val a = TAU * i / 720f
        at(a, RADIUS, strokeGlyph(a + TAU / 4f))
    }
    for (m in 0 until 60) {
        val a = TAU * m / 60f
        at(a, RADIUS - 2f, if (m % 5 == 0) '#' else '.')
    }
    numeral(12, "12", 3)
    numeral(3, "3", -1)
    numeral(6, "6", 1)
    numeral(9, "9", -3)
}

private fun Grid.numeral(hour: Int, text: String, shift: Int) {
    val a = TAU * hour / 12f
    write((CX + (RADIUS - 5f) * sin(a)).roundToInt() + shift, (CY - (RADIUS - 5f) * cos(a) / 2f).roundToInt(), text)
}

/**
 * A round wall clock. It shows the time [now] returns when the piece is built, which is seconds since midnight,
 * and then keeps counting with the piece's own time, so a fixed [now] gives the same face for the same `t`
 * everywhere. The default reads the system clock in UTC: common code has no time zone, so pass a local reading
 * to show local time. Original piece (drawn after ascii.rest `analog clock`, hands and bezel are new).
 */
public fun analogClockPiece(now: () -> Double = ::utcSecondsOfDay, second: Rgb = Palette.Red): AsciiPiece {
    val start = now()
    return asciiPiece(
        name = "analog clock",
        category = "objects",
        cols = COLS,
        rows = ROWS,
        fps = 10,
        still = 0f,
        note = "a round clock face, the seconds hand ticking",
    ) { t, grid ->
        val seconds = floor(start + t)
        val minutes = seconds / 60.0
        val hours = minutes / 60.0
        grid.face()
        grid.hand((TAU * (hours % 12.0) / 12.0).toFloat(), 0f, RADIUS * 0.5f, null)
        grid.hand((TAU * (minutes % 60.0) / 60.0).toFloat(), 0f, RADIUS * 0.78f, null)
        grid.hand((TAU * (seconds % 60.0) / 60.0).toFloat(), 0f, RADIUS * 0.88f, second)
        grid.put(CX, CY, '@')
    }
}

/**
 * An analog clock whose hands follow [now], seconds since midnight, sampled once. The default is the system
 * clock in UTC. [second] colors the seconds hand.
 */
@Composable
public fun AsciiAnalogClock(
    modifier: Modifier = Modifier,
    now: () -> Double = ::utcSecondsOfDay,
    second: Rgb = Palette.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An analog clock showing the current time",
) {
    val piece = remember(now, second) { analogClockPiece(now, second) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
