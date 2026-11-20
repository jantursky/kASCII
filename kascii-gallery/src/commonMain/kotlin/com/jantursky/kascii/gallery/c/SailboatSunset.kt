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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val COLS = 64
private const val ROWS = 24
private const val HORIZON = 14 // the first row of water
private const val LOOP = 24f
private const val FPS = 12
private const val SUN_X = 42f
private const val SUN_R = 7f // radius in columns; a row is two columns tall
private const val BOAT_X = 6
private const val GULLS = 3
private const val GLOW_RAMP = " .-="

private val BOAT =
    listOf(
        "         |\\        ",
        "        /|&\\       ",
        "       /&|&&\\      ",
        "      /&&|&&&\\     ",
        "     /&&&|&&&&\\    ",
        "    /&&&&|&&&&&\\   ",
        "   /____ |______\\  ",
        " ________|_________",
        " \\&&&&&&&&&&&&&&&/ ",
        "  \\_____________/  ",
    )

private val GULL = listOf("\\v/", "-v-", "/v\\", "-v-")

/**
 * A sailboat at sunset. The sun sinks until it touches the horizon and climbs back once per loop, its glow and a
 * shimmering band of light on the water following it. The boat bobs on the swell and a few gulls glide past,
 * flapping. Every motion repeats a whole number of times per loop.
 */
public fun sailboatSunsetPiece(
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    sky: Rgb = AKit.Red,
): AsciiPiece {
    val rng = AKit.rng(seed, 31)
    val gulls = List(GULLS) { floatArrayOf(rng.float(), 1f + rng.int(5), (1 + rng.int(2)).toFloat()) }
    return asciiPiece(
        name = "sailboat sunset",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = FPS,
        still = 9f,
        loop = LOOP,
        note = "a sailboat bobbing under a setting sun, its light shimmering on the sea",
    ) { time, grid ->
        val t = SceneKit.loopTime(time, FPS, LOOP)
        val angle = AKit.TAU * t / LOOP
        val sunY = HORIZON - 6.5f + 6.5f * (0.5f - 0.5f * cos(angle))
        paintSunsetSky(grid, seed, angle, sunY, sun, sky)
        paintWater(grid, seed, t, sunY, sun, sky)
        paintBoat(grid, t)
        for ((i, g) in gulls.withIndex()) paintGull(grid, i, g, t)
    }
}

private fun paintSunsetSky(
    grid: Grid,
    seed: Int,
    angle: Float,
    sunY: Float,
    sun: Rgb,
    sky: Rgb,
) {
    for (y in 0 until HORIZON) {
        val band = AKit.Violet.lerp(sky, y / (HORIZON - 1f))
        for (x in 0 until COLS) {
            val dx = (x + 0.5f - SUN_X) / SUN_R
            val dy = (y + 0.5f - sunY) / (SUN_R / 2f)
            val d = sqrt(dx * dx + dy * dy)
            if (d <= 1f) {
                grid.put(x, y, if (d > 0.8f) '#' else '@', sun)
                continue
            }
            val glow = 1.6f / (1f + (d - 1f) * (d - 1f) * 3f) - 0.2f
            if (glow > 0.15f) {
                grid.put(x, y, AKit.glyph(GLOW_RAMP, glow), sun.lerp(band, 1f - glow))
                continue
            }
            // Thin streaks of cloud drift across the sky and come back over a loop.
            val n = Noise.fbm(x * 0.05f + 2f * cos(angle), y * 0.7f + sin(angle), 0f, 3, seed)
            if (n > 0.68f) grid.put(x, y, if (n > 0.74f) '=' else '-', band)
        }
    }
}

private fun paintWater(
    grid: Grid,
    seed: Int,
    t: Float,
    sunY: Float,
    sun: Rgb,
    sky: Rgb,
) {
    // The sun half under the horizon still lights a narrow path.
    val strength = (1.4f - (sunY - (HORIZON - 6.5f)) / 9f).coerceIn(0.5f, 1f)
    for (y in HORIZON until ROWS) {
        val depth = (y - HORIZON + 1f) / (ROWS - HORIZON)
        val half = SUN_R * (0.45f + 0.7f * depth) * strength
        val phase = AKit.TAU * (3 + y % 3) * t / LOOP
        val shift = Noise.hash(y, 0, 32, seed) * AKit.TAU
        for (x in 0 until COLS) {
            val off = abs(x + 0.5f - SUN_X) / half
            val ripple = sin(x * (0.9f - depth * 0.4f) + shift - phase) + 0.5f * sin(x * 2.1f + phase * 2f)
            if (off < 1f) {
                val shimmer = ripple + (1f - off) * 1.2f
                if (shimmer > 0.4f) {
                    grid.put(x, y, if (shimmer > 1.3f) '=' else '-', sun.lerp(sky, off))
                }
                continue
            }
            if (ripple > 1.05f) grid.put(x, y, if (depth > 0.5f) '~' else '-', AKit.Blue)
        }
    }
}

private fun paintBoat(
    grid: Grid,
    t: Float,
) {
    val bob = sin(AKit.TAU * 3f * t / LOOP).roundToInt()
    val drift = (sin(AKit.TAU * t / LOOP) * 3f).roundToInt()
    SceneKit.stamp(grid, BOAT_X + drift, HORIZON - 7 + bob, BOAT)
    // A thin wake at the waterline.
    val wake = HORIZON + 1 + bob
    grid.put(BOAT_X + drift, wake, '~', AKit.Blue)
    grid.put(BOAT_X + drift + BOAT[0].length, wake, '~', AKit.Blue)
}

private fun paintGull(
    grid: Grid,
    index: Int,
    gull: FloatArray,
    t: Float,
) {
    val span = COLS + 4f
    val laps = gull[2]
    val x = SceneKit.wrap(gull[0] * span + laps * span * t / LOOP, span) - 3f
    val y = gull[1] + sin(AKit.TAU * (2f * t / LOOP + gull[0])) * 0.8f
    val flap = floor(4f * 24f * t / LOOP + index).toInt().mod(GULL.size)
    grid.write(x.toInt(), y.roundToInt(), GULL[flap])
}

/**
 * A sailboat bobbing under a setting sun, the light shimmering on the water and gulls gliding past.
 *
 * @param seed shapes the cloud streaks and the ripples and places the gulls.
 * @param sun the color of the sun and of its path on the water.
 * @param sky the color the sky turns near the horizon.
 */
@Composable
public fun AsciiSailboatSunset(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    sky: Rgb = AKit.Red,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A sailboat on the sea at sunset, the sun's light shimmering on the water",
) {
    val piece = remember(seed, sun, sky) { sailboatSunsetPiece(seed, sun, sky) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
