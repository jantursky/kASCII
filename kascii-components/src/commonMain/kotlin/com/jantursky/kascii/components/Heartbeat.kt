package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.compose.rememberReducedMotion
import com.jantursky.kascii.core.BrailleCanvas
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt

/** A scrolling ECG trace at [bpm] beats per minute. Decoration: still under reduced motion, hidden from readers. */
@Composable
public fun AsciiHeartbeat(
    modifier: Modifier = Modifier,
    bpm: Int = 72,
    cols: Int = 40,
    rows: Int = 4,
) {
    val time = rememberClock(HEARTBEAT_FPS, rememberReducedMotion()).floatValue
    GridSurface(renderHeartbeat(cols, rows, time, bpm, LocalKasciiTheme.current), modifier)
}

/** One beat of an ECG trace for [phase] in 0..1: P wave, QRS spike, T wave. Baseline is 0, the R peak 1. */
internal fun ecgSample(phase: Float): Float {
    fun bump(center: Float, width: Float, height: Float): Float {
        val d = (phase - center) / width
        return height * exp(-d * d)
    }
    return bump(P_AT, 0.04f, 0.15f) + bump(Q_AT, 0.012f, -0.2f) + bump(R_AT, 0.014f, 1f) +
        bump(S_AT, 0.014f, -0.3f) + bump(T_AT, 0.06f, 0.25f)
}

internal fun renderHeartbeat(cols: Int, rows: Int, t: Float, bpm: Int, theme: KasciiTheme): Grid {
    val canvas = BrailleCanvas(cols, rows)
    val beatDots = 60f / bpm * DOTS_PER_SECOND
    val baseline = canvas.dotHeight * BASELINE_SHARE
    fun dotY(x: Int): Int {
        val beats = (x + t * DOTS_PER_SECOND) / beatDots
        return (baseline - ecgSample(beats - floor(beats)) * canvas.dotHeight * AMPLITUDE_SHARE).roundToInt()
    }
    for (x in 1 until canvas.dotWidth) canvas.line(x - 1, dotY(x - 1), x, dotY(x))
    return Grid(cols, rows).also { canvas.drawTo(it, 0, 0, theme.good()) }
}

private const val HEARTBEAT_FPS = 24
private const val DOTS_PER_SECOND = 30f
private const val BASELINE_SHARE = 0.7f
private const val AMPLITUDE_SHARE = 0.6f
private const val P_AT = 0.18f
private const val Q_AT = 0.40f
private const val R_AT = 0.44f
private const val S_AT = 0.48f
private const val T_AT = 0.70f
