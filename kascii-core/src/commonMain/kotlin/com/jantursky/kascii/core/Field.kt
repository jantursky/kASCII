package com.jantursky.kascii.core

/**
 * A function of position and time that returns a tone in 0..1. `x` and `y` are in 0..1 across the grid, `t` is
 * seconds, `aspect` is width over height of the whole grid in pixels so a field can keep circles round.
 */
public fun interface Field {
    /** The tone at `x`, `y` (0..1 across the grid) at time `t` seconds, for a grid of the given `aspect`. */
    public fun tone(
        x: Float,
        y: Float,
        t: Float,
        aspect: Float,
    ): Float
}

/** A function of position and time that returns a per-cell color. */
public fun interface ColorField {
    /** The color at `x`, `y` (0..1 across the grid) at time `t` seconds. */
    public fun color(
        x: Float,
        y: Float,
        t: Float,
    ): Rgb
}

/** Procedural fields. All are pure, and deterministic for a given `seed`. */
public object Fields {
    /** Classic demoscene plasma. */
    public fun plasma(
        scale: Float = 6f,
        speed: Float = 1f,
    ): Field =
        Field { x, y, t, a ->
            val px = x * a * scale
            val py = y * scale
            val tt = t * speed
            val v =
                sin(px + tt) + sin(py * 1.3f - tt * 0.7f) +
                    sin((px + py) * 0.7f + tt * 0.5f) + sin(sqrt(px * px + py * py) * 1.1f - tt)
            (v / 8f + 0.5f).coerceIn(0f, 1f)
        }

    /** Fractal value noise drifting over time. */
    public fun noise(
        scale: Float = 4f,
        speed: Float = 0.3f,
        octaves: Int = 4,
        seed: Int = 0,
    ): Field = Field { x, y, t, a -> Noise.fbm(x * a * scale, y * scale, t * speed, octaves, seed) }

    /** Interfering travelling waves. */
    public fun waves(
        count: Int = 3,
        scale: Float = 8f,
        speed: Float = 1f,
    ): Field =
        Field { x, y, t, a ->
            var v = 0f
            for (k in 0 until count) {
                val ang = k * (PI_F * 2f / count)
                v += sin((x * a * cos(ang) + y * sin(ang)) * scale + t * speed * (1f + k * 0.3f))
            }
            (v / (count * 2f) + 0.5f).coerceIn(0f, 1f)
        }

    /** Concentric ripples from a point. */
    public fun ripples(
        cx: Float = 0.5f,
        cy: Float = 0.5f,
        scale: Float = 18f,
        speed: Float = 2f,
    ): Field =
        Field { x, y, t, a ->
            val dx = (x - cx) * a
            val dy = y - cy
            (sin(sqrt(dx * dx + dy * dy) * scale - t * speed) * 0.5f + 0.5f)
        }

    /** A linear ramp, 0 at the start side and 1 at the other. [angle] is in radians, 0 is left to right. */
    public fun gradient(angle: Float = 0f): Field =
        Field { x, y, _, _ ->
            (x * cos(angle) + y * sin(angle)).coerceIn(0f, 1f)
        }

    /** A soft disc: 1 at the centre fading to 0 at [radius]. */
    public fun radial(
        cx: Float = 0.5f,
        cy: Float = 0.5f,
        radius: Float = 0.5f,
    ): Field =
        Field { x, y, _, a ->
            val dx = (x - cx) * a
            val dy = y - cy
            (1f - sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
        }

    /** A constant tone. */
    public fun solid(tone: Float): Field = Field { _, _, _, _ -> tone }

    /** Reads a tone grid sampled elsewhere, [tones] being [width] by [height] in row-major order. */
    public fun fromTones(
        tones: FloatArray,
        width: Int,
        height: Int,
    ): Field {
        require(tones.size == width * height) { "Expected ${width * height} tones, got ${tones.size}" }
        return Field { x, y, _, _ ->
            val ix = (x * width).toInt().coerceIn(0, width - 1)
            val iy = (y * height).toInt().coerceIn(0, height - 1)
            tones[iy * width + ix]
        }
    }
}
