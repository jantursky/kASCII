package com.jantursky.kascii.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.IntSize

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

    /** Time summed in Double: a Float sum stops growing after days, because the step falls below its precision. */
    private var elapsed = still.toDouble()

    /** The spec the running loop reads each frame, so a new speed never restarts the clock. */
    var spec: ClockSpec = ClockSpec(running = false, still = still)

    suspend fun run() {
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) {
                    elapsed = advance(elapsed, (now - last) / NANOS.toDouble(), spec)
                    time = elapsed.toFloat()
                }
                last = now
            }
        }
    }

    /** Holds the still frame when the spec stops the clock. */
    fun settle(spec: ClockSpec) {
        if (!spec.running) {
            elapsed = spec.still.toDouble()
            time = spec.still
        }
    }
}

private const val NANOS = 1_000_000_000f

internal fun advance(
    elapsed: Double,
    deltaSeconds: Double,
    spec: ClockSpec,
): Double {
    val t = elapsed + deltaSeconds.coerceAtLeast(0.0) * spec.speed
    if (spec.loop <= 0f) return t
    val wrapped = t % spec.loop
    return if (wrapped < 0.0) wrapped + spec.loop else wrapped
}

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
