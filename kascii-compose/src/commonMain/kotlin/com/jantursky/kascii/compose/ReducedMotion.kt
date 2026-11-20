package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import com.jantursky.kascii.compose.platform.platformReducedMotion
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.PieceClock

/**
 * The motion setting for everything below it that does not take a `motion` parameter of its own, such as the
 * components. Provide [Motion.Full] or [Motion.Reduced] to override the platform preference for a whole screen.
 */
public val LocalKasciiMotion: ProvidableCompositionLocal<Motion> = compositionLocalOf { Motion.System }

/** Whether motion is reduced, from [motion] (the [LocalKasciiMotion] setting by default) and the platform. */
@Composable
public fun rememberReducedMotion(motion: Motion = LocalKasciiMotion.current): Boolean {
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
