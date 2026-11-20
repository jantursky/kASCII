package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Rng
import kotlin.math.PI
import kotlin.math.sin

/**
 * An audio-style equalizer of [bands] bars that dance for a given [seed]. It is decoration: it holds a still frame
 * under reduced motion and is hidden from screen readers.
 */
@Composable
public fun AsciiEqualizer(
    modifier: Modifier = Modifier,
    bands: Int = 16,
    rows: Int = 6,
    seed: Long = 0L,
) {
    val time = rememberClock(EQUALIZER_FPS, rememberReducedMotion()).floatValue
    GridSurface(renderEqualizer(bands, rows, time, seed, LocalKasciiTheme.current), modifier)
}

/** Band levels (0..1) at time [t]: two sines per band whose speeds and phases come from [seed]. */
internal fun equalizerLevels(bands: Int, t: Float, seed: Long): List<Float> {
    val rng = Rng(seed)
    return List(bands) {
        val slow = rng.float(2f, 6f)
        val fast = rng.float(5f, 11f)
        val phaseA = rng.float(0f, 2 * PI.toFloat())
        val phaseB = rng.float(0f, 2 * PI.toFloat())
        0.5f + 0.28f * sin(t * slow + phaseA) + 0.2f * sin(t * fast + phaseB)
    }
}

internal fun renderEqualizer(bands: Int, rows: Int, t: Float, seed: Long, theme: KasciiTheme): Grid =
    renderColumns(equalizerLevels(bands, t, seed), rows, barWidth = 2, gap = 1, color = theme.accent)

private const val EQUALIZER_FPS = 15
