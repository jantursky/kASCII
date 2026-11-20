package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.label
import kotlin.math.abs
import kotlin.math.sin

private const val COLS = 56
private const val ROWS = 26
private const val CENTER = 28
private const val BASE = 21
private const val SPARK_LOOP = 8f
private const val SPARKS = 12
private const val RAMP = " .:*sS#@"

/**
 * A campfire: flames from a value-noise field that rises and flickers inside a tapering cone, colored from red
 * at the edge to amber at the core, over crossed logs with sparks drifting up and a few stars overhead. [height]
 * scales the flames, 1 being about 17 rows. The flicker is not periodic, so the piece does not loop.
 */
public fun campfirePiece(
    seed: Int = 0,
    height: Float = 1f,
    ember: Rgb = AKit.Red,
    flame: Rgb = AKit.Amber,
): AsciiPiece {
    require(height > 0f) { "height must be positive" }
    val rows = 17f * height
    val rng = AKit.rng(seed, 7)
    val sparks = List(SPARKS) { floatArrayOf(rng.float(), rng.float(-1f, 1f), (1 + rng.int(2)).toFloat()) }
    return asciiPiece(
        name = "campfire",
        category = "nature",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 2f,
        note = "flickering flames over crossed logs with rising sparks",
    ) { t, grid ->
        for (x in 0 until COLS) for (y in 0 until 4) {
            if (Noise.hash(x, y, 7, seed) > 0.96f) grid.put(x, y, '.')
        }
        for (y in 0..BASE) for (x in 0 until COLS) {
            val hy = (BASE - y) / rows
            if (hy > 1f) continue
            val n = Noise.fbm(x * 0.22f, y * 0.28f + t * 2.4f, t * 0.6f, 3, seed)
            val shape = 1f - abs(x - CENTER) / (9f * (1f - hy * 0.85f))
            val tone = (shape * 1.1f - hy * 0.9f + (n - 0.5f) * 0.9f + 0.25f).coerceIn(0f, 1f)
            if (tone > 0.12f) grid.put(x, y, AKit.glyph(RAMP, tone), ember.lerp(flame, tone))
        }
        grid.label(CENTER, BASE + 1, "\\==#####==/")
        grid.label(CENTER, BASE + 2, "/===#####===\\")
        grid.label(CENTER - 11, BASE + 2, "oOo")
        grid.label(CENTER + 11, BASE + 2, "oOo")
        for (x in 0 until COLS) grid.put(x, BASE + 3, '=')
        for (s in sparks) {
            val p = AKit.frac(s[0] + s[2] * t / SPARK_LOOP)
            val x = CENTER + s[1] * 6f * p + sin(AKit.TAU * (s[2] * t / SPARK_LOOP + s[0])) * 2f * p
            grid.put(x.toInt(), BASE - 3 - (p * (rows + 3f)).toInt(), if (p < 0.5f) '*' else '.', flame)
        }
    }
}

/**
 * A campfire with flickering flames, logs and rising sparks.
 *
 * @param seed shapes the flicker and the sparks.
 * @param height scales the flames; 1 is the default size.
 * @param ember the color of the flame edges.
 * @param flame the color of the flame core and the sparks.
 */
@Composable
public fun Campfire(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    height: Float = 1f,
    ember: Rgb = AKit.Red,
    flame: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A campfire over crossed logs, flames flickering and sparks rising",
) {
    val piece = remember(seed, height, ember, flame) { campfirePiece(seed, height, ember, flame) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
