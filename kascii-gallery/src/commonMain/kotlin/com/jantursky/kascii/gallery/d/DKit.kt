package com.jantursky.kascii.gallery

import kotlin.math.floor

/**
 * [t] folded into `0 until loop`. Drawing from the folded time keeps a frame exactly equal to the same frame one
 * loop later, and keeps float precision once the clock has run for a long time.
 */
internal fun loopTime(
    t: Float,
    loop: Float,
): Float = t - floor(t / loop) * loop
