package com.jantursky.kascii.core

/** Deterministic value noise. Same inputs and seed always give the same output on every platform. */
public object Noise {
    /** Hash of integer lattice coordinates to 0..1. */
    public fun hash(x: Int, y: Int, z: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 362437
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFFFF) / 16777215f
    }

    private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Smooth value noise in 0..1. */
    public fun value(x: Float, y: Float, z: Float = 0f, seed: Int = 0): Float {
        val xi = kotlin.math.floor(x).toInt()
        val yi = kotlin.math.floor(y).toInt()
        val zi = kotlin.math.floor(z).toInt()
        val fx = smooth(x - xi)
        val fy = smooth(y - yi)
        val fz = smooth(z - zi)
        fun h(dx: Int, dy: Int, dz: Int) = hash(xi + dx, yi + dy, zi + dz, seed)
        val x00 = lerp(h(0, 0, 0), h(1, 0, 0), fx)
        val x10 = lerp(h(0, 1, 0), h(1, 1, 0), fx)
        val x01 = lerp(h(0, 0, 1), h(1, 0, 1), fx)
        val x11 = lerp(h(0, 1, 1), h(1, 1, 1), fx)
        return lerp(lerp(x00, x10, fy), lerp(x01, x11, fy), fz)
    }

    /** Fractal sum of [octaves] noise layers, normalised to 0..1. */
    public fun fbm(x: Float, y: Float, z: Float = 0f, octaves: Int = 4, seed: Int = 0): Float {
        var sum = 0f
        var amp = 0.5f
        var freq = 1f
        var norm = 0f
        for (o in 0 until octaves) {
            sum += amp * value(x * freq, y * freq, z * freq, seed + o * 101)
            norm += amp
            amp *= 0.5f
            freq *= 2f
        }
        return sum / norm
    }
}
