package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * Seconds since the effect started, advanced at [fps] steps per second so a component only recomposes when its
 * frame changes. The loop ends when the caller leaves the composition. When [reduced] the value stays at
 * [stillTime].
 */
@Composable
internal fun rememberClock(fps: Int, reduced: Boolean, stillTime: Float = 0f): FloatState {
    val time = remember(reduced, stillTime) { mutableFloatStateOf(if (reduced) stillTime else 0f) }
    LaunchedEffect(fps, reduced) {
        if (reduced) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time.floatValue = quantizedSeconds(now - start, fps)
        }
    }
    return time
}

internal fun quantizedSeconds(elapsedNanos: Long, fps: Int): Float =
    (elapsedNanos * fps / NANOS_PER_SECOND) / fps.toFloat()

private const val NANOS_PER_SECOND = 1_000_000_000L
