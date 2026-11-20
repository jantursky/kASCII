package com.jantursky.kascii.core

/** Whether animation runs or holds a still frame. */
public enum class Motion {
    /** Follow the platform's reduced-motion setting. */
    System,

    /** Always animate. */
    Full,

    /** Always hold the still frame. */
    Reduced,
}

/** Pure time bookkeeping for a piece. The Compose module feeds it frame deltas. */
public object PieceClock {
    /**
     * The next time value. When [reduced] the clock stays at [still]. [speed] scales real time. A positive [loop]
     * wraps the time so a piece repeats seamlessly.
     */
    public fun next(
        time: Float,
        deltaSeconds: Float,
        speed: Float,
        reduced: Boolean,
        still: Float,
        loop: Float = 0f,
    ): Float {
        if (reduced) return still
        val t = time + deltaSeconds.coerceAtLeast(0f) * speed
        return if (loop > 0f) t % loop else t
    }

    /** Resolves [motion] against the platform preference. */
    public fun isReduced(
        motion: Motion,
        platformReduced: Boolean,
    ): Boolean =
        when (motion) {
            Motion.System -> platformReduced
            Motion.Full -> false
            Motion.Reduced -> true
        }
}
