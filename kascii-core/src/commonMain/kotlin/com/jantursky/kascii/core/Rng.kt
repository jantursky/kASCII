package com.jantursky.kascii.core

/** A small seeded random generator (SplitMix64) so a piece with the same seed draws the same frames everywhere. */
public class Rng(seed: Long = 0L) {
    private var state: Long = seed

    private fun next(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** A float in 0..1. */
    public fun float(): Float = ((next() ushr 40).toInt()) / 16777216f

    /** A float in [from]..[until]. */
    public fun float(from: Float, until: Float): Float = from + (until - from) * float()

    /** An int in 0 until [bound]. */
    public fun int(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return ((next() ushr 33) % bound).toInt()
    }

    /** True with probability [p]. */
    public fun chance(p: Float): Boolean = float() < p
}
