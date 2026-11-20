package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Noise
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.b.ease
import com.jantursky.kascii.gallery.b.frameIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val FPS = 20
private const val LOOP = 12f
private const val PAD = 20
private const val ROCKET_X = 34

// The timeline of one loop, in seconds.
private const val IGNITION = 2f
private const val LIFTOFF = 3.5f
private const val GONE = 8f
private const val CLEARED = 10.5f
private const val ARRIVE = 10f
private const val READY = 11.5f

/** Rows climbed per second squared after lift-off; the rocket leaves the frame before [GONE]. */
private const val CLIMB = 1.9f

// The rocket, drawn from its top-left corner and centred on ROCKET_X. '=' marks the painted bands, 'o' the
// porthole.
private val ROCKET =
    listOf(
        "   /\\   ",
        "  /  \\  ",
        "  |  |  ",
        "  |()|  ",
        "  |  |  ",
        "  |==|  ",
        "  |  |  ",
        "  |  |  ",
        " /|==|\\ ",
        "/_|__|_\\",
        "  /__\\  ",
    )
private const val ROCKET_LEFT = ROCKET_X - 4
private const val FLAME_GLYPHS = "▓#*'."
private const val TOWER_X = ROCKET_X - 10
private const val TOWER_TOP = PAD - 13

/**
 * A rocket launch without a countdown: the rocket waits on the pad venting vapor, the engines light, flame and
 * smoke billow over the pad, and it lifts off and climbs out of the frame while the camera shakes. The smoke then
 * thins and a fresh rocket fades in on the pad, so the loop starts over where it began. The seed shapes the smoke
 * and the stars.
 */
public fun rocketLaunchPiece(
    seed: Int = 0,
    flame: Rgb = AKit.Amber,
    trim: Rgb = AKit.Red,
): AsciiPiece {
    val frames = (LOOP * FPS).toInt()
    return asciiPiece(
        name = "rocket launch",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 5f,
        loop = LOOP,
        note = "a rocket lights its engines and climbs out of frame in a cloud of smoke",
    ) { t, grid ->
        // Whole frames modulo the loop, so the timeline repeats exactly.
        val frame = frameIndex(t, FPS) % frames
        val time = frame / FPS.toFloat()
        val scene = Grid(COLS, ROWS)
        paintSky(scene, seed)
        paintGround(scene, seed)
        paintTower(scene, time)
        paintTrail(scene, seed, time)
        // The smoke swallows the foot of the rocket on the pad, but not once it has climbed clear.
        if (altitude(time) < 3f) {
            paintRocket(scene, seed, time, flame, trim)
            paintPadSmoke(scene, seed, time)
        } else {
            paintPadSmoke(scene, seed, time)
            paintRocket(scene, seed, time, flame, trim)
        }
        val shake = shake(time)
        val dx = ((Noise.hash(frame, 1, 31, seed) * 2f - 1f) * shake * 1.5f).roundToInt()
        val dy = ((Noise.hash(frame, 2, 31, seed) * 2f - 1f) * shake * 0.8f).roundToInt()
        grid.blit(scene, dx, dy)
    }
}

/** How hard the camera shakes, 0 to 1: it builds with the engines and fades as the rocket climbs away. */
private fun shake(time: Float): Float =
    when {
        time < IGNITION + 0.3f -> 0f
        time < LIFTOFF -> ease((time - IGNITION - 0.3f) / (LIFTOFF - IGNITION - 0.3f))
        time < 5f -> 1f
        else -> 1f - ease((time - 5f) / 2f)
    }

/** Rows the rocket has climbed above the pad. */
private fun altitude(time: Float): Float =
    if (time < LIFTOFF || time >= ARRIVE) 0f else CLIMB * (time - LIFTOFF) * (time - LIFTOFF)

/** Overall strength of the smoke, 0 to 1: it rises with ignition and thins away before the next rocket. */
private fun smokeLevel(time: Float): Float =
    when {
        time < IGNITION -> 0f
        time < LIFTOFF + 0.5f -> ease((time - IGNITION) / (LIFTOFF + 0.5f - IGNITION))
        time < 7f -> 1f
        time < CLEARED -> 1f - ease((time - 7f) / (CLEARED - 7f))
        else -> 0f
    }

private fun paintSky(
    grid: Grid,
    seed: Int,
) {
    for (y in 0 until PAD - 2) {
        for (x in 0 until COLS) {
            val h = Noise.hash(x, y, 33, seed)
            if (h > 0.985f) grid.put(x, y, if (h > 0.996f) '+' else '.')
        }
    }
}

private fun paintGround(
    grid: Grid,
    seed: Int,
) {
    for (x in 0 until COLS) {
        grid.put(x, PAD + 1, '▀')
        for (y in PAD + 2 until ROWS) {
            if (Noise.hash(x, y, 35, seed) > 0.85f) grid.put(x, y, '.')
        }
    }
    for (x in ROCKET_X - 8..ROCKET_X + 7) grid.put(x, PAD, '▄')
}

private fun paintTower(
    grid: Grid,
    time: Float,
) {
    for (y in TOWER_TOP..PAD) {
        grid.put(TOWER_X, y, '|')
        grid.put(TOWER_X + 1, y, if (y % 2 == 0) '\\' else '/')
        grid.put(TOWER_X + 2, y, '|')
    }
    grid.write(TOWER_X - 1, TOWER_TOP - 1, "_|_")
    grid.put(TOWER_X + 1, TOWER_TOP - 2, '*', AKit.Red)
    // The access arms swing away before lift-off and come back for the next rocket.
    val reach =
        when {
            time < IGNITION -> 1f
            time < LIFTOFF -> 1f - ease((time - IGNITION) / (LIFTOFF - IGNITION))
            time < ARRIVE -> 0f
            else -> ease((time - ARRIVE) / (READY - ARRIVE))
        }
    val length = (reach * (ROCKET_LEFT + 2 - TOWER_X - 3)).roundToInt()
    for (row in listOf(PAD - 9, PAD - 5)) {
        for (k in 0 until length) grid.put(TOWER_X + 3 + k, row, '=')
    }
}

private fun paintRocket(
    grid: Grid,
    seed: Int,
    time: Float,
    flame: Rgb,
    trim: Rgb,
) {
    val arriving = time >= ARRIVE
    if (time >= GONE && !arriving) return
    val top = PAD - ROCKET.size - altitude(time).toInt()
    val fade = if (arriving) ((time - ARRIVE) / (READY - ARRIVE)).coerceIn(0f, 1f) else 1f
    ROCKET.forEachIndexed { row, line ->
        line.forEachIndexed { col, ch ->
            if (ch == ' ') return@forEachIndexed
            val x = ROCKET_LEFT + col
            val y = top + row
            val h = Noise.hash(col, row, 37, seed)
            when {
                h < fade -> {
                    when (ch) {
                        '=' -> grid.put(x, y, '=', trim)
                        '(', ')' -> grid.put(x, y, ch, AKit.Blue)
                        else -> grid.put(x, y, ch)
                    }
                }

                h < fade + 0.3f -> {
                    grid.put(x, y, '.')
                }
            }
        }
    }
    if (!arriving) {
        paintVapor(grid, top, time)
        paintFlame(grid, seed, time, top + ROCKET.size, flame)
    }
}

/** Wisps of vapor drifting off the waiting rocket. */
private fun paintVapor(
    grid: Grid,
    top: Int,
    time: Float,
) {
    if (time >= IGNITION) return
    val strength = sin(PI.toFloat() * time / IGNITION)
    for (k in 0..3) {
        val drift = AKit.frac(time * 0.8f + k * 0.25f)
        if (drift > strength) continue
        val side = if (k % 2 == 0) 1 else -1
        val x = ROCKET_X + side * (3 + (drift * 5f).toInt()) - (if (side < 0) 1 else 0)
        grid.put(x, top + 4 + k, if (drift < 0.5f) '~' else '-')
    }
}

private fun paintFlame(
    grid: Grid,
    seed: Int,
    time: Float,
    nozzle: Int,
    color: Rgb,
) {
    if (time < IGNITION) return
    val frame = (time * FPS).toInt()
    val length =
        if (time < LIFTOFF) {
            1f + 5f * ease((time - IGNITION) / (LIFTOFF - IGNITION))
        } else {
            6f + 1.5f * Noise.hash(frame, 0, 39, seed)
        }
    for (k in 0 until length.toInt()) {
        val y = nozzle + k
        if (y > PAD + 1) break
        val half = (2.2f - k * 0.35f).coerceAtLeast(0.6f)
        for (x in (ROCKET_X - half - 0.5f).roundToInt()..(ROCKET_X + half - 0.5f).roundToInt()) {
            val flicker = Noise.hash(x, y, frame, seed)
            val heat = 1f - k / length
            val glyph =
                when {
                    heat > 0.75f -> '▓'
                    heat > 0.5f -> if (flicker > 0.4f) '#' else '*'
                    heat > 0.25f -> if (flicker > 0.5f) '*' else '\''
                    else -> if (flicker > 0.5f) '\'' else '.'
                }
            grid.put(x, y, glyph, color.lerp(AKit.Red, 1f - heat))
        }
    }
}

/** The billowing cloud around the pad. */
private fun paintPadSmoke(
    grid: Grid,
    seed: Int,
    time: Float,
) {
    val level = smokeLevel(time)
    if (level <= 0f) return
    val radius = 3f + 11f * ease((time - IGNITION) / 4f) + (time - IGNITION) * 0.6f
    for (y in PAD - 8..PAD + 1) {
        for (x in 0 until COLS) {
            val dx = (x - ROCKET_X + 0.5f) / (radius * 1.7f)
            val dy = (y - PAD + 0.5f) / (radius * 0.5f).coerceAtLeast(1.5f)
            val d = sqrt(dx * dx + dy * dy)
            if (d > 1.3f) continue
            val n = Noise.fbm(x * 0.2f, y * 0.4f + time * 0.5f, time * 0.25f, 3, seed + 3)
            val v = level * (1.1f - d) + (n - 0.5f) * 0.9f
            val glyph =
                when {
                    v > 0.75f -> '@'
                    v > 0.55f -> 'O'
                    v > 0.38f -> 'o'
                    v > 0.22f -> ':'
                    v > 0.1f -> '.'
                    else -> continue
                }
            // The flame burns through the smoke.
            if (grid.colorAt(x, y) != null && grid.glyphAt(x, y) in FLAME_GLYPHS) continue
            grid.put(x, y, glyph)
        }
    }
}

/** The column of exhaust the rocket leaves behind, widening and thinning as it ages. */
private fun paintTrail(
    grid: Grid,
    seed: Int,
    time: Float,
) {
    if (time < LIFTOFF + 0.3f) return
    val fade = smokeLevel(time)
    if (fade <= 0f) return
    val nozzle = PAD - altitude(time) + 6f
    for (y in 0 until PAD - 2) {
        if (y < nozzle) continue
        // When the nozzle passed this row, and how long ago that was.
        val climbed = (PAD - y).toFloat()
        val passed = LIFTOFF + sqrt(climbed / CLIMB)
        val age = (time - passed).coerceAtLeast(0f)
        val width = 0.8f + age * 1.1f
        for (x in (ROCKET_X - width - 1).toInt()..(ROCKET_X + width + 1).toInt()) {
            val off = abs(x + 0.5f - ROCKET_X) / width
            if (off > 1.2f) continue
            val n = Noise.fbm(x * 0.3f, y * 0.3f - time * 0.4f, 1f, 2, seed + 5)
            val v = fade * (1f - age / 6f) * (1.1f - off) + (n - 0.5f) * 0.7f
            val glyph =
                when {
                    v > 0.6f -> 'O'
                    v > 0.4f -> 'o'
                    v > 0.22f -> ':'
                    v > 0.1f -> '.'
                    else -> continue
                }
            grid.put(x, y, glyph)
        }
    }
}

/**
 * A rocket launch: engines light, smoke billows, the rocket climbs out of frame with the camera shaking, and a
 * new rocket fades in on the pad.
 *
 * @param seed shapes the smoke and the stars.
 * @param flame the color of the engine flame.
 * @param trim the color of the painted bands on the rocket.
 */
@Composable
public fun AsciiRocketLaunch(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    flame: Rgb = AKit.Amber,
    trim: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A rocket lifting off its launch pad in a cloud of smoke and flame",
) {
    val piece = remember(seed, flame, trim) { rocketLaunchPiece(seed, flame, trim) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
