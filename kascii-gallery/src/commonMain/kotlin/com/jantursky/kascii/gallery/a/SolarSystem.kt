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
import com.jantursky.kascii.core.disc
import kotlin.math.cos
import kotlin.math.sin

private const val COLS = 72
private const val ROWS = 26
private const val LOOP = 60f
private const val TILT = 0.35f
private const val DOTS = 72

private class SolarPlanet(val radius: Float, val laps: Int, val glyph: Char, val color: Rgb?, val ring: Boolean = false)

private const val MOON_LAPS = 60

/**
 * A toy solar system seen from above at a slant: a sun, six planets on dotted orbits (one with a ring) and a moon.
 * Each planet makes a whole number of laps per loop, so the loop is seamless. The seed sets where each planet
 * starts on its orbit.
 */
public fun solarSystemPiece(seed: Int = 0, sun: Rgb = AKit.Amber): AsciiPiece {
    val planets = listOf(
        SolarPlanet(8f, 12, 'o', AKit.Red),
        SolarPlanet(13f, 8, 'O', AKit.Blue),
        SolarPlanet(19f, 5, '@', AKit.Green),
        SolarPlanet(25f, 3, 'O', AKit.Amber, ring = true),
        SolarPlanet(30f, 2, '0', AKit.Violet),
        SolarPlanet(34f, 1, 'o', null),
    )
    val rng = AKit.rng(seed, 3)
    val starts = planets.map { rng.float() * AKit.TAU }
    return asciiPiece(
        name = "solar system",
        category = "space",
        cols = COLS,
        rows = ROWS,
        fps = 20,
        still = 7f,
        loop = LOOP,
        note = "six planets, a ringed one and a moon around a sun",
    ) { t, grid ->
        val cx = COLS / 2f
        val cy = ROWS / 2f
        planets.forEach { p ->
            repeat(DOTS) { k ->
                val a = AKit.TAU * k / DOTS
                grid.put((cx + cos(a) * p.radius).toInt(), (cy + sin(a) * p.radius * TILT).toInt(), '.')
            }
        }
        grid.disc(cx, cy, 6f, '@', sun)
        planets.forEachIndexed { i, p ->
            val a = starts[i] + AKit.TAU * p.laps * t / LOOP
            val px = (cx + cos(a) * p.radius).toInt()
            val py = (cy + sin(a) * p.radius * TILT).toInt()
            if (p.ring) {
                grid.write(px - 4, py, "=(${p.glyph})=", p.color)
            } else {
                grid.write(px - 1, py, "(${p.glyph})", p.color)
            }
            if (i == 2) {
                val m = AKit.TAU * MOON_LAPS * t / LOOP
                grid.put((px + cos(m) * 3f).toInt(), (py + sin(m) * 1.2f).toInt(), '.', AKit.Blue)
            }
        }
    }
}

/**
 * A toy solar system: a sun, six planets on dotted orbits and a moon.
 *
 * @param seed sets where each planet starts on its orbit.
 * @param sun the color of the sun.
 */
@Composable
public fun SolarSystem(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    sun: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A sun with six planets and a moon circling it on dotted orbits",
) {
    val piece = remember(seed, sun) { solarSystemPiece(seed, sun) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
