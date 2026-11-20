package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * [text] that glitches now and then: bursts of swapped glyphs and a sideways jump. [intensity] is the share of
 * glyphs hit during a burst. Which frames glitch depends on [seed]. Under reduced motion the text stays clean.
 */
@Composable
public fun AsciiGlitchText(
    text: String,
    modifier: Modifier = Modifier,
    intensity: Float = 0.2f,
    seed: Long = 0L,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, theme ->
        renderGlitch(text, t, intensity, seed, reduced, theme)
    }
}

/** Scrolls [text] through a window [cols] cells wide, endlessly. Under reduced motion it shows the start. */
@Composable
public fun AsciiMarquee(
    text: String,
    modifier: Modifier = Modifier,
    cols: Int = 24,
    charsPerSecond: Float = 8f,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, _ ->
        renderMarquee(text, cols, if (reduced) 0f else t, charsPerSecond)
    }
}

/** [text] riding a sine wave, [amplitude] rows up and down. Under reduced motion it is flat. */
@Composable
public fun AsciiWaveText(
    text: String,
    modifier: Modifier = Modifier,
    amplitude: Int = 1,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, _ -> renderWave(text, if (reduced) null else t, amplitude) }
}

/** [text] colored by cycling through the theme palette, drifting along the line. Still under reduced motion. */
@Composable
public fun AsciiRainbowText(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, theme -> renderRainbow(text, if (reduced) 0f else t, theme) }
}

/** [text] with a highlight sweeping across it. Under reduced motion it is plain ink. */
@Composable
public fun AsciiShimmerText(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedText(text, TEXT_FPS, modifier) { t, reduced, theme -> renderShimmer(text, t, reduced, theme) }
}

internal fun renderGlitch(
    text: String,
    t: Float,
    intensity: Float,
    seed: Long,
    reduced: Boolean,
    theme: KasciiTheme,
): Grid {
    val grid = Grid(text.length + 2, 1)
    val frame = (t * TEXT_FPS).toInt()
    val burst = !reduced && frameRng(seed, -1, frame / 3).chance(GLITCH_BURST_CHANCE)
    if (!burst) return grid.also { it.write(1, 0, text) }
    val jump = frameRng(seed, -2, frame).int(3)
    text.forEachIndexed { i, c ->
        val hit = c != ' ' && frameRng(seed, i, frame).chance(intensity)
        val glyph = if (hit) GLITCH[frameRng(seed, i, frame + 1).int(GLITCH.length)] else c
        grid.put(jump + i, 0, glyph, if (hit) theme.bad() else null)
    }
    return grid
}

internal fun renderMarquee(text: String, cols: Int, t: Float, charsPerSecond: Float): Grid {
    val loop = text + " ".repeat(MARQUEE_GAP)
    val offset = (t * charsPerSecond).toInt() % loop.length
    val window = (loop.repeat(cols / loop.length + 2)).drop(offset).take(cols)
    return Grid(cols, 1).also { it.writeText(window) }
}

/** Rows hold the wave; [t] null draws the text flat in the middle row. */
internal fun renderWave(text: String, t: Float?, amplitude: Int): Grid {
    val grid = Grid(text.length, 2 * amplitude + 1)
    text.forEachIndexed { i, c ->
        val lift = if (t == null) 0f else sin(t * WAVE_SPEED + i * WAVE_STEP)
        grid.put(i, amplitude + (lift * amplitude).roundToInt(), c)
    }
    return grid
}

internal fun renderRainbow(text: String, t: Float, theme: KasciiTheme): Grid {
    val grid = Grid(text.length, 1)
    text.forEachIndexed { i, c -> grid.put(i, 0, c, paletteColor(theme.palette, i * RAINBOW_STEP + t * RAINBOW_SPEED)) }
    return grid
}

/** A color along the looping [palette], [position] in turns: 0 and 1 are both the first color. */
internal fun paletteColor(palette: List<Rgb>, position: Float): Rgb {
    val scaled = (position - floor(position)) * palette.size
    val from = scaled.toInt() % palette.size
    return palette[from].lerp(palette[(from + 1) % palette.size], scaled - floor(scaled))
}

internal fun renderShimmer(text: String, t: Float, reduced: Boolean, theme: KasciiTheme): Grid {
    val grid = Grid(text.length, 1)
    val center = (t * SHIMMER_SPEED) % (text.length + 2 * SHIMMER_WIDTH) - SHIMMER_WIDTH
    text.forEachIndexed { i, c ->
        val tone = (1f - abs(i - center) / SHIMMER_WIDTH).coerceAtLeast(0f)
        grid.put(i, 0, c, if (reduced) null else theme.dim().lerp(theme.ink, tone))
    }
    return grid
}

private const val GLITCH = "▓▒░#@%&\$/\\|<>"
private const val GLITCH_BURST_CHANCE = 0.4f
private const val MARQUEE_GAP = 4
private const val WAVE_SPEED = 4f
private const val WAVE_STEP = (PI / 3).toFloat()
private const val RAINBOW_STEP = 0.08f
private const val RAINBOW_SPEED = 0.25f
private const val SHIMMER_SPEED = 12f
private const val SHIMMER_WIDTH = 5f
