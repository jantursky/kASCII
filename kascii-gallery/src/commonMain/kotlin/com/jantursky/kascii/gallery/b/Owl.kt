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
import com.jantursky.kascii.core.disc
import com.jantursky.kascii.core.label
import com.jantursky.kascii.gallery.b.Palette
import com.jantursky.kascii.gallery.b.Star
import com.jantursky.kascii.gallery.b.TAU
import com.jantursky.kascii.gallery.b.rngFor
import kotlin.math.sin

private const val COLS = 44
private const val ROWS = 22
private const val LOOP = 12f
private const val BLINK = 0.18f
private const val GLANCE = 1.4f
private const val OWL_X = 21
private const val BODY_WIDTH = 19

private fun row(left: String, inner: String, right: String): String {
    val room = BODY_WIDTH - left.length - right.length
    return left + inner.padStart((room + inner.length) / 2).padEnd(room) + right
}

private val BODY = listOf(
    row("/\\", "_".repeat(BODY_WIDTH - 4), "/\\"),
    row("/", "", "\\"),
    "",
    row("|", "v", "|"),
    row("|", "/|||\\", "|"),
    row("|", "/|||||\\", "|"),
    row(" \\", "\\|||/", "/ "),
    row("  \\", "_".repeat(BODY_WIDTH - 6), "/  "),
    "\"\" \"\"",
)

/** Where the owl looks, in -1..1, and whether its eyes are shut, for a time inside the loop. */
private class Gaze(val blinks: List<Float>, val glances: List<Float>) {
    fun closed(t: Float): Boolean = blinks.any { t >= it && t < it + BLINK }

    fun side(t: Float): Int {
        val i = glances.indexOfLast { t >= it }
        return if (i >= 0 && t < glances[i] + GLANCE) (if (i % 2 == 0) -1 else 1) else 0
    }
}

private fun eye(closed: Boolean, side: Int): String = when {
    closed -> "---"
    side < 0 -> "O  "
    side > 0 -> "  O"
    else -> " O "
}

private fun Grid.owl(gaze: Gaze, t: Float, eyes: Rgb) {
    val top = 8
    BODY.forEachIndexed { i, line -> label(OWL_X, top + i, line) }
    val pupils = eye(gaze.closed(t), gaze.side(t))
    val row = top + 2
    val left = OWL_X - BODY_WIDTH / 2
    write(left, row, "|  (")
    write(left + 4, row, pupils, eyes)
    write(left + 7, row, ")   (")
    write(left + 12, row, pupils, eyes)
    write(left + 15, row, ")  |")
}

private fun Grid.branch(t: Float, leaf: Rgb) {
    for (x in 0 until COLS) put(x, 17, if (x % 7 == 3) '=' else '_')
    for (x in 4 until COLS - 4) put(x, 18, if (x % 5 == 0) '~' else ' ')
    val sway = if (sin(TAU * t / LOOP * 3f) > 0f) 1 else 0
    for (k in 0 until 3) {
        put(6 + k * 11 + sway, 16, '>', leaf)
        put(36 - k * 9 - sway, 16, '<', leaf)
    }
}

/**
 * A small owl on a branch under a moon. It blinks and glances left and right at times the seed picks, and the
 * stars twinkle on their own beat; everything repeats every 12 seconds. The seed moves the stars and shifts the
 * blinks. Original piece.
 */
public fun owlPiece(seed: Int = 0, eyes: Rgb = Palette.Amber): AsciiPiece {
    val rng = rngFor(seed, 2)
    val stars = List(16) { Star(rng.int(COLS), rng.int(9), 1 + rng.int(3), rng.float() * TAU) }
    val gaze = Gaze(
        blinks = List(3) { it * 4f + 0.5f + rng.float() * 3f },
        glances = List(4) { it * 3f + 0.4f + rng.float() * 1.2f },
    )
    return asciiPiece(
        name = "owl",
        category = "creatures",
        cols = COLS,
        rows = ROWS,
        fps = 12,
        still = 0f,
        loop = LOOP,
        note = "an owl on a branch, blinking and glancing under twinkling stars",
    ) { time, grid ->
        val t = time % LOOP
        for (s in stars) {
            val v = sin(TAU * s.rate * t / LOOP + s.phase)
            grid.put(s.x, s.y, if (v > 0.5f) '*' else if (v > -0.3f) '+' else '.')
        }
        grid.disc(6f, 4f, 5f, 'o', eyes)
        grid.owl(gaze, t, eyes)
        grid.branch(t, Palette.Green)
    }
}

/**
 * A small owl on a branch that blinks and glances around under twinkling stars. [seed] times the blinks and
 * places the stars, [eyes] colors the eyes and the moon.
 */
@Composable
public fun AsciiOwl(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    eyes: Rgb = Palette.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "An owl on a branch under twinkling stars",
) {
    val piece = remember(seed, eyes) { owlPiece(seed, eyes) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
