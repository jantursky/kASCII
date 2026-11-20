package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.cos
import kotlin.math.sin

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 6f
private const val CLOUD_ROWS = 3
private const val SPLASH_ROWS = 3

/**
 * Rain falling from a drifting cloud and splashing on the ground. Drops fall at three speeds, slanted by [wind],
 * and wrap around the sides. Each drop lands a whole number of times per loop.
 */
public fun rainPiece(
    seed: Int = 0,
    density: Float = 1f,
    wind: Float = 0.25f,
    color: Rgb = AKit.Blue,
): AsciiPiece {
    require(density > 0f) { "density must be positive" }
    val rng = AKit.rng(seed, 6)
    val drops =
        List((COLS * density * 2f).toInt().coerceAtLeast(1)) {
            floatArrayOf(rng.float(0f, COLS.toFloat()), (2 + rng.int(3)).toFloat(), rng.float())
        }
    val slant =
        when {
            wind > 0.1f -> '/'
            wind < -0.1f -> '\\'
            else -> '|'
        }
    return asciiPiece(
        name = "rain",
        category = "nature",
        cols = COLS,
        rows = ROWS,
        still = 1.5f,
        loop = LOOP,
        note = "slanted rain under a drifting cloud, splashing on the ground",
    ) { t, grid ->
        val angle = AKit.TAU * t / LOOP
        for (y in 0 until CLOUD_ROWS) {
            for (x in 0 until COLS) {
                val n = Noise.fbm(x * 0.09f + 3f * cos(angle), y * 0.4f + 3f * sin(angle), 0f, 3, seed) - y * 0.16f
                val glyph = AKit.glyph(" .:=%", n * 1.4f - 0.3f)
                if (glyph != ' ') grid.put(x, y, glyph)
            }
        }
        for (x in 0 until COLS) grid.put(x, ROWS - 1, '-')
        for (d in drops) {
            val y = AKit.frac(d[2] + d[1] * t / LOOP) * (ROWS + SPLASH_ROWS)
            val x = (((d[0] - y * wind).toInt() % COLS) + COLS) % COLS
            val fall = y.toInt()
            when {
                // Drops inside the cloud are not drawn.
                fall < CLOUD_ROWS -> {}

                fall < ROWS - 1 -> {
                    grid.put(x, fall, if (d[1] == 2f) '.' else slant, color)
                }

                else -> {
                    grid.put(x, ROWS - 1, 'o', color)
                    if (fall > ROWS - 1) {
                        grid.put(x - 1, ROWS - 1, '.', color)
                        grid.put(x + 1, ROWS - 1, '.', color)
                    }
                }
            }
        }
    }
}

/**
 * Rain falling from a drifting cloud.
 *
 * @param seed places the drops and shapes the cloud.
 * @param density the number of drops relative to the width; 1 is a steady shower.
 * @param wind how much the rain slants, 0 for straight down.
 * @param color the rain.
 */
@Composable
public fun RainShower(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    density: Float = 1f,
    wind: Float = 0.25f,
    color: Rgb = AKit.Blue,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "Rain falling from a cloud and splashing on the ground",
) {
    val piece = remember(seed, density, wind, color) { rainPiece(seed, density, wind, color) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
