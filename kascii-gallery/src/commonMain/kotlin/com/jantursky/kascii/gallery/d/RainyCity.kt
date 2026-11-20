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
import com.jantursky.kascii.core.Rng
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs
import kotlin.math.floor

private const val COLS = 64
private const val ROWS = 24
private const val LOOP = 8f
private const val STREET = 19
private const val DROPS = 80
private const val RIPPLE_ROWS = 4f
private const val FLASH_AT = 5f
private const val LAMP_X = 47
private const val NEAR_FILL = '▓'
private const val FAR_FILL = '░'

private class CityTower(
    val x: Int,
    val width: Int,
    val top: Int,
    val near: Boolean,
)

private class CityPuddle(
    val cx: Float,
    val cy: Float,
    val rx: Float,
) {
    fun contains(
        x: Int,
        y: Int,
    ): Boolean {
        val dx = (x + 0.5f - cx) / rx
        val dy = (y + 0.5f - cy) / 0.9f
        return dx * dx + dy * dy <= 1f
    }
}

/**
 * A rainy night in the city: a skyline in silhouette whose windows switch on and off, slanted rain that ripples
 * the puddles on the street, a street lamp, and once per loop a lightning strike that lights up the sky. The seed
 * builds the skyline and places the drops and puddles; the lightning comes at the same moment of every loop.
 */
public fun rainyCityPiece(
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    rain: Rgb = AKit.Blue,
    wind: Float = 0.35f,
): AsciiPiece {
    val towers = skyline(AKit.rng(seed, 51))
    val rng = AKit.rng(seed, 52)
    val drops =
        List(DROPS) {
            floatArrayOf(
                rng.float(0f, COLS.toFloat() + 8f),
                (2 + rng.int(2)).toFloat(),
                rng.float(),
                (STREET + 1 + rng.int(ROWS - STREET - 1)).toFloat(),
            )
        }
    val puddles =
        listOf(9f, 30f, 54f).map {
            CityPuddle(it + rng.float(-3f, 3f), STREET + 2.5f + rng.int(2), 3f + rng.float(0f, 2f))
        }
    val boltX = 12 + rng.int(36)
    val slant =
        when {
            wind > 0.1f -> '/'
            wind < -0.1f -> '\\'
            else -> '|'
        }
    return asciiPiece(
        name = "rainy city",
        category = "scenes",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 2f,
        loop = LOOP,
        note = "rain over a night skyline with flickering windows and a lightning flash",
    ) { time, grid ->
        val t = loopTime(time, LOOP)
        val flash = flash(t)
        paintFlashSky(grid, seed, flash)
        towers.forEach { paintTower(grid, it, seed, t, window) }
        if (flash > 0.5f) paintBolt(grid, seed, boltX)
        paintAntenna(grid, towers, t)
        paintStreet(grid, puddles, seed, t, flash)
        paintLamp(grid, window)
        paintRain(grid, drops, puddles, t, wind, slant, rain, window)
    }
}

private fun skyline(rng: Rng): List<CityTower> {
    val towers = mutableListOf<CityTower>()
    var x = -2 + rng.int(4)
    while (x < COLS) {
        val w = 4 + rng.int(5)
        towers += CityTower(x, w, STREET - 8 - rng.int(9), near = false)
        x += w + 2 + rng.int(4)
    }
    x = -1 + rng.int(3)
    while (x < COLS) {
        val w = 5 + rng.int(5)
        towers += CityTower(x, w, STREET - 4 - rng.int(8), near = true)
        x += w + 2 + rng.int(4)
    }
    return towers
}

/** Brightness of the lightning, 0 most of the loop and a double flash just after [FLASH_AT] seconds. */
private fun flash(t: Float): Float {
    val local = t - FLASH_AT
    return when {
        local < 0f -> 0f
        local < 0.15f -> 1f
        local < 0.3f -> 0.3f
        local < 0.45f -> 0.85f
        local < 1.2f -> 0.85f * (1f - (local - 0.45f) / 0.75f)
        else -> 0f
    }
}

private fun paintFlashSky(
    grid: Grid,
    seed: Int,
    flash: Float,
) {
    for (y in 0 until STREET) {
        for (x in 0 until COLS) {
            val cloud = Noise.fbm(x * 0.08f, y * 0.35f, 0f, 3, seed) - y * 0.07f
            val light = flash * (0.6f + 0.6f * cloud)
            val glyph =
                when {
                    light > 0.65f -> ':'
                    light > 0.3f -> '.'
                    cloud > 0.5f && y < 3 -> '-'
                    else -> continue
                }
            grid.put(x, y, glyph, if (flash > 0f) AKit.Blue.lerp(AKit.Violet, 1f - flash) else null)
        }
    }
}

private fun paintBolt(
    grid: Grid,
    seed: Int,
    startX: Int,
) {
    var x = startX
    for (y in 0 until STREET) {
        val under = grid.glyphAt(x, y)
        if (under == NEAR_FILL || under == FAR_FILL) break
        val step = (Noise.hash(y, 0, 21, seed) * 3f).toInt() - 1
        val glyph =
            when (step) {
                -1 -> '/'
                1 -> '\\'
                else -> '|'
            }
        grid.put(x, y, glyph, AKit.Amber)
        if (y % 4 == 3) grid.put(x + 2 * step, y + 1, if (step < 0) '/' else '\\', AKit.Amber)
        x += step
    }
}

private fun paintTower(
    grid: Grid,
    tower: CityTower,
    seed: Int,
    t: Float,
    window: Rgb,
) {
    val fill = if (tower.near) NEAR_FILL else FAR_FILL
    grid.fill(tower.x, tower.top, tower.width, STREET - tower.top, fill, AKit.Violet)
    for (wy in tower.top + 1 until STREET - 1 step 2) {
        for (wx in tower.x + 1 until tower.x + tower.width - 1 step 2) {
            if (!windowLit(wx, wy, seed, t, tower.near)) continue
            grid.put(wx, wy, if (tower.near) '█' else '▒', window)
        }
    }
}

/** A window is either always lit, always dark, or one of a few that switch every second of the loop. */
private fun windowLit(
    x: Int,
    y: Int,
    seed: Int,
    t: Float,
    near: Boolean,
): Boolean {
    val layer = if (near) 1 else 2
    val steady = Noise.hash(x, y, layer, seed) > (if (near) 0.55f else 0.7f)
    if (Noise.hash(x, y, layer + 10, seed) < 0.82f) return steady
    val epoch = floor(t + Noise.hash(x, y, layer + 20, seed)).toInt().mod(LOOP.toInt())
    return Noise.hash(x, y, epoch + 30, seed) > 0.5f
}

private fun paintAntenna(
    grid: Grid,
    towers: List<CityTower>,
    t: Float,
) {
    val tallest = towers.filter { it.near }.minBy { it.top }
    val x = tallest.x + tallest.width / 2
    for (k in 1..3) grid.put(x, tallest.top - k, '|')
    val blink = AKit.frac(2f * t / LOOP * 4f) < 0.5f
    grid.put(x, tallest.top - 4, if (blink) '*' else '.', AKit.Red)
}

private fun paintStreet(
    grid: Grid,
    puddles: List<CityPuddle>,
    seed: Int,
    t: Float,
    flash: Float,
) {
    for (x in 0 until COLS) grid.put(x, STREET, '▀')
    val shimmer = floor(t * 4f).toInt().mod((LOOP * 4f).toInt())
    for (y in STREET + 1 until ROWS) {
        for (x in 0 until COLS) {
            val puddle = puddles.firstOrNull { it.contains(x, y) }
            if (puddle == null) {
                if (y == ROWS - 2 && x % 8 < 4) grid.put(x, y, '-')
                continue
            }
            // A puddle mirrors the lit windows above it, and the sky when lightning strikes.
            val mirror = grid.glyphAt(x, 2 * STREET - y)
            when {
                mirror == '█' -> grid.put(x, y, '=', grid.colorAt(x, 2 * STREET - y))
                flash > 0.3f -> grid.put(x, y, '=', AKit.Blue)
                else -> grid.put(x, y, if (Noise.hash(x, y, shimmer, seed) > 0.6f) '~' else '-', AKit.Blue)
            }
        }
    }
}

private fun paintLamp(
    grid: Grid,
    light: Rgb,
) {
    val top = STREET - 8
    grid.write(LAMP_X, top, "┌──┐")
    grid.put(LAMP_X + 3, top + 1, '▀', light)
    for (y in top + 1 until STREET) grid.put(LAMP_X, y, '│')
    for (x in LAMP_X + 1..LAMP_X + 6) grid.put(x, STREET, '▀', light)
}

private fun paintRain(
    grid: Grid,
    drops: List<FloatArray>,
    puddles: List<CityPuddle>,
    t: Float,
    wind: Float,
    slant: Char,
    rain: Rgb,
    light: Rgb,
) {
    for (d in drops) {
        val land = d[3]
        val y = AKit.frac(d[2] + d[1] * t / LOOP) * (land + RIPPLE_ROWS) - 1f
        val x = (d[0] - (y + 1f) * wind * 1.6f).toInt()
        val row = y.toInt()
        if (y < land) {
            if (y < 0f) continue
            val under = grid.glyphAt(x, row)
            if (under == NEAR_FILL || under == '█' || under == '▒') continue
            // Drops falling through the lamp's light catch its color.
            val lit = row > STREET - 7 && abs(x - (LAMP_X + 3.5f)) < (row - (STREET - 7)) * 0.6f + 0.5f
            grid.put(x, row, slant, if (lit) light else rain)
            continue
        }
        val age = (y - land) / RIPPLE_ROWS
        val ly = land.toInt()
        val lx = (d[0] - (land + 1f) * wind * 1.6f).toInt()
        if (puddles.any { it.contains(lx, ly) }) {
            val r = 1 + (age * 3f).toInt()
            if (age < 0.3f) grid.put(lx, ly, 'o', rain)
            grid.put(lx - r, ly, '(', rain)
            grid.put(lx + r, ly, ')', rain)
        } else if (age < 0.4f) {
            grid.put(lx - 1, ly, '.', rain)
            grid.put(lx + 1, ly, '.', rain)
            grid.put(lx, ly, ',', rain)
        }
    }
}

/**
 * A rainy night in the city: a skyline with flickering windows, slanted rain rippling the puddles, a street lamp
 * and a lightning flash once per loop.
 *
 * @param seed builds the skyline and places the rain and the puddles.
 * @param window the color of the lit windows and the lamp.
 * @param rain the color of the rain and the puddles.
 * @param wind how much the rain slants, 0 for straight down.
 */
@Composable
public fun AsciiRainyCity(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    window: Rgb = AKit.Amber,
    rain: Rgb = AKit.Blue,
    wind: Float = 0.35f,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A city skyline at night in the rain, windows flickering and lightning in the sky",
) {
    val piece = remember(seed, window, rain, wind) { rainyCityPiece(seed, window, rain, wind) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
