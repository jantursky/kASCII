package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import com.jantursky.kascii.compose.platform.platformReducedMotion
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.PieceClock

/** Whether the platform asks for reduced motion. Combine with [Motion] through [rememberReducedMotion]. */
@Composable
public fun rememberReducedMotion(motion: Motion = Motion.System): Boolean {
    val durationScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
    return resolveReducedMotion(motion, durationScale, platformReducedMotion())
}

/**
 * Compose sets [MotionDurationScale] to 0 when animations are off on Android and when Reduce Motion is on in iOS, so
 * that covers Android versions the platform check can't see.
 */
internal fun resolveReducedMotion(
    motion: Motion,
    durationScale: Float,
    platformReduced: Boolean,
): Boolean = PieceClock.isReduced(motion, durationScale == 0f || platformReduced)
