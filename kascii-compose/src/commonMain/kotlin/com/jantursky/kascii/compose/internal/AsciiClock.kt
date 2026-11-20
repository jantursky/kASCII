package com.jantursky.kascii.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.IntSize
import com.jantursky.kascii.core.PieceClock

/** How a view's time moves. [running] is false for still views and under reduced motion. */
internal data class ClockSpec(
    val running: Boolean,
    val speed: Float = 1f,
    val still: Float = 0f,
    val loop: Float = 0f,
)

/**
 * Piece time as snapshot state. Only draw code reads it, so a frame redraws and never recomposes. [run] advances it
 * on every frame of the host's frame clock, which stops delivering frames while the app is in the background (the
 * Android recomposer pauses it when the lifecycle stops, desktop and iOS stop rendering hidden windows), so the
 * clock needs no lifecycle observer of its own.
 */
internal class AsciiClock(
    still: Float,
) {
    var time by mutableFloatStateOf(still)

    /** The spec the running loop reads each frame, so a new speed never restarts the clock. */
    var spec: ClockSpec = ClockSpec(running = false, still = still)

    suspend fun run() {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) {
                    time = PieceClock.next(time, (now - last) / NANOS, spec.speed, false, spec.still, spec.loop)
                }
                last = now
            }
        }
    }

    /** Holds the still frame when the spec stops the clock. */
    fun settle(spec: ClockSpec) {
        if (!spec.running) time = spec.still
    }
}

private const val NANOS = 1_000_000_000f

/** Whether any part of the element is inside the window, after every ancestor's bounds have clipped it. */
internal fun LayoutCoordinates.isOnScreen(): Boolean {
    if (!isAttached) return false
    val root = findRootCoordinates()
    return visibleIn(root.localBoundingBoxOf(this, clipBounds = true), root.size)
}

internal fun visibleIn(
    bounds: Rect,
    window: IntSize,
): Boolean {
    val visible = bounds.intersect(Rect(0f, 0f, window.width.toFloat(), window.height.toFloat()))
    return visible.width > 0f && visible.height > 0f
}
