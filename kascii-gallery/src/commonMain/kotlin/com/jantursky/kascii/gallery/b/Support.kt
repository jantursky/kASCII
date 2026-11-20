package com.jantursky.kascii.gallery.b

import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.Rng
import kotlin.math.floor
import kotlin.time.Clock

internal const val TAU: Float = 6.2831855f

/** Mid-tone accents of the design system, readable on the dark and on the light theme. */
internal object Palette {
    val Violet: Rgb = Rgb.fromHex("#7f52ff")
    val Red: Rgb = Rgb.fromHex("#e44857")
    val Green: Rgb = Rgb.fromHex("#00bc7d")
    val Amber: Rgb = Rgb.fromHex("#f0b100")
    val Blue: Rgb = Rgb.fromHex("#63b3ed")
}

/** The frame a time falls in, so a stepped simulation advances by whole frames. */
internal fun frameIndex(t: Float, fps: Int): Int = floor(t * fps + 1e-3f).toInt().coerceAtLeast(0)

/** The fractional part of [v], always in 0 until 1. */
internal fun frac(v: Float): Float = v - floor(v)

/** Hermite ease between 0 and 1. */
internal fun ease(x: Float): Float {
    val k = x.coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

/** A generator for one purpose ([salt]) of a piece, so adding a draw call never shifts another one. */
internal fun rngFor(seed: Int, salt: Int): Rng = Rng(seed * 1_000_003L + salt)

/** A color along [colors], spread evenly, for [pos] in 0..1. */
internal fun gradient(colors: List<Rgb>, pos: Float): Rgb {
    if (colors.size == 1) return colors[0]
    val scaled = pos.coerceIn(0f, 1f) * (colors.size - 1)
    val i = scaled.toInt().coerceAtMost(colors.size - 2)
    return colors[i].lerp(colors[i + 1], scaled - i)
}

/**
 * Replays a simulation to any frame. It keeps one live state and steps it forward, and only rebuilds from the
 * start when asked for an earlier frame, so a frame's content depends on the frame alone, never on call order.
 * Not thread safe: a piece is drawn from one thread at a time.
 */
internal class Stepper<S>(private val create: () -> S, private val advance: (S) -> Unit) {
    private var state: S = create()
    private var frame = 0

    /** The state after [target] steps. The returned object is reused, so read it before the next call. */
    fun at(target: Int): S {
        if (target < frame) {
            state = create()
            frame = 0
        }
        while (frame < target) {
            advance(state)
            frame++
        }
        return state
    }
}

/** Seconds since midnight UTC read from the system clock. */
internal fun utcSecondsOfDay(): Double = (Clock.System.now().toEpochMilliseconds() % 86_400_000L) / 1000.0

/** A twinkling star: it cycles [rate] times per loop, starting at [phase]. */
internal class Star(val x: Int, val y: Int, val rate: Int, val phase: Float)
