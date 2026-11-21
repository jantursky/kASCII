package com.jantursky.kascii.gallery.j

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.gallery.AKit
import com.jantursky.kascii.gallery.loopTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val LOOP = 16f
private const val CX = 136f
private const val BEACON_X = 157f
private const val BEACON_Y = 7f
private const val SUN_X = 34f
private const val STARS = 60

/** When the dragon breathes fire, seconds into the loop, while it passes in front of the castle. */
private const val FIRE_A = 3.2f
private const val FIRE_B = 5.4f

private class DragonCastle(
    seed: Int,
) {
    private val lat = JLattice(seed, 41)
    private val sky = JFrame()
    private val castle = JFrame()
    private val frame = JFrame()
    private val palette: JPalette
    private val windows = ArrayList<IntArray>()
    private val starX = IntArray(STARS)
    private val starY = IntArray(STARS)
    private val starPhase = FloatArray(STARS)
    private val stoneLit = jcol("#9a5a52")
    private val stoneDark = jcol("#2c1828")
    private val hide = jcol("#1f5a4e")
    private val hideLit = jcol("#e8a850")
    private val wing = jcol("#3a8a70")
    private val flame = jramp(5, jcol("#7a1a08"), jcol("#e04a10"), jcol("#ff9a20"), jcol("#ffe070"), jcol("#fffbe0"))

    init {
        buildSky()
        buildCastle()
        val rng = AKit.rng(seed, 17)
        for (s in 0 until STARS) {
            starX[s] = rng.int(JCOLS)
            starY[s] = rng.int(14)
            starPhase[s] = rng.float()
        }
        palette = JPalette(buildPalette())
    }

    private fun buildPalette(): List<JCol> =
        jramp(
            10,
            jcol("#0e0820"),
            jcol("#2a1240"),
            jcol("#5a1e4a"),
            jcol("#a83a3a"),
            jcol("#f07a30"),
            jcol("#ffc870"),
        ) +
            jramp(6, jcol("#14081a"), jcol("#2a1230"), jcol("#44203e")) +
            jramp(8, jcol("#120a14"), stoneDark, jcol("#5a3040"), stoneLit, jcol("#e0a080")) +
            jramp(6, jcol("#081c18"), hide, wing, jcol("#7ac8a0")) +
            jramp(4, jcol("#6a5a30"), hideLit, jcol("#fff0b0")) +
            flame +
            listOf(jcol("#ffd870"), jcol("#ffffff"), jcol("#c0a0d0"), jcol("#3a0a10"))

    private fun buildSky() {
        for (y in 0 until JROWS) {
            val g = y / 36f
            for (x in 0 until JCOLS) {
                val sun = jgauss(x.toFloat(), SUN_X, 60f)
                var c =
                    jcol(
                        "#140a2c",
                    ).mix(jcol("#4a1a4e"), jsmooth(0f, 0.6f, g)).mix(jcol("#d24a30"), jsmooth(0.45f, 1f, g))
                c = c.mix(jcol("#ffb050"), jsmooth(0.7f, 1f, g) * sun * 0.8f)
                val n = lat.fbm(x * 0.025f + 4f, y * 0.4f, 3)
                val cloud = jsmooth(0.6f, 0.72f, n) * jsmooth(5f, 12f, y.toFloat()) * jsmooth(28f, 20f, y.toFloat())
                // Clouds are lit from below by the sun that has just set.
                c = c.mix(jcol("#ff7a4a").mix(jcol("#ffc080"), sun), cloud * (0.5f + 0.4f * g))
                val cv = 0.2f + 0.75f * g * g + cloud * 0.9f + (n - 0.5f) * 0.2f
                sky.put(x, y, c, if (cloud > 0.6f) 1.8f else jclamp(cv))
            }
        }
        // Far mountains.
        for (x in 0 until JCOLS) {
            val top = 33f - 6f * lat.fbm(x * 0.03f + 20f, 2f, 3) - 3f * jgauss(x.toFloat(), 60f, 20f)
            for (y in top.toInt() until JROWS) {
                val rim = jsmooth(1.5f, 0f, y - top) * jgauss(x.toFloat(), SUN_X, 70f)
                val n = lat.fbm(x * 0.1f + 70f, y * 0.4f, 2)
                // Valley mist pooling low down.
                val mist = jsmooth(36f, 42f, y.toFloat()) * (0.5f + 0.5f * n)
                val c =
                    jcol("#1c0c26").mix(jcol("#2e1636"), n).mix(jcol("#a04a4a"), rim * 0.7f).mix(
                        jcol("#5a2a4a"),
                        mist * 0.6f,
                    )
                sky.put(x, y, c, if (n > 0.35f || rim > 0.2f) 2f else 1.8f)
            }
        }
    }

    private fun cragTop(x: Int): Float {
        val d = (x - CX) / 40f
        return 24.5f + d * d * d * d * 24f + 1.4f * (lat.fbm(x * 0.15f, 7f, 2) - 0.5f)
    }

    private fun stone(
        x: Int,
        y: Int,
        light: Float,
    ): Pair<JCol, Float> {
        // Masonry: courses of blocks, offset every other row.
        val off = if (y % 2 == 0) 0 else 2
        val joint = (x + off) % 4 == 0
        val n = lat.at(x * 0.8f, y * 1.3f)
        val c = stoneDark.mix(stoneLit, jclamp(light + (n - 0.5f) * 0.25f)).scale(if (joint) 0.8f else 1f)
        return Pair(c, if (joint) 1.8f else 2f)
    }

    private fun wall(
        x0: Int,
        x1: Int,
        top: Int,
        bottom: Int,
        crenels: Boolean,
    ) {
        for (y in top..bottom) {
            for (x in x0..x1) {
                val face = (x - x0).toFloat() / max(1, x1 - x0)
                val light = 0.75f - 0.55f * face
                val (c, cv) = stone(x, y, light)
                castle.put(x, y, c, cv)
            }
        }
        if (crenels) {
            for (x in x0..x1) {
                if ((x - x0) % 3 == 2) continue
                val (c, _) = stone(x, top - 1, 0.7f - 0.5f * (x - x0).toFloat() / max(1, x1 - x0))
                castle.put(x, top - 1, c, 2f)
            }
        }
    }

    private fun buildCastle() {
        // The crag.
        for (x in 80 until JCOLS) {
            val top = cragTop(x)
            if (top >= JROWS) continue
            for (y in top.toInt() until JROWS) {
                val slope = cragTop(x + 1) - cragTop(x - 1)
                val n = lat.fbm(x * 0.2f + 50f, y * 0.5f, 3)
                val strata = jfract(y * 0.33f + n * 1.2f) < 0.2f
                val light = jclamp(0.38f - slope * 0.2f + (n - 0.5f) * 0.3f - (y - top) * 0.015f)
                var c = jcol("#1a0c18").mix(jcol("#8a4a4a"), light)
                if (strata) c = c.scale(0.8f)
                castle.put(x, y, c, 2f)
            }
        }
        // Walls, towers and the keep.
        wall(114, 158, 18, 25, true)
        wall(109, 118, 11, 25, true)
        wall(153, 161, 9, 25, true)
        wall(127, 144, 7, 25, false)
        // Conical roof of the keep and a pennant.
        for (y in 0..6) {
            val half = (y + 1) * 1.45f
            for (x in (CX - half).toInt()..(CX + half).toInt()) {
                val face = (x - (CX - half)) / (2f * half)
                castle.put(x, y, jcol("#5a1a2a").mix(jcol("#c04a3a"), jclamp(0.9f - face)), 2f)
            }
        }
        for (y in 18..25) {
            for (x in 133..138) castle.put(x, y, jcol("#0a0408"), 2f)
        }
        castle.put(135, 17, jcol("#0a0408"), 2f, '▄')
        castle.put(136, 17, jcol("#0a0408"), 2f, '▄')
        // Windows: slits in towers and keep.
        val spots =
            listOf(
                intArrayOf(113, 14),
                intArrayOf(113, 19),
                intArrayOf(131, 10),
                intArrayOf(139, 10),
                intArrayOf(131, 14),
                intArrayOf(139, 14),
                intArrayOf(157, 13),
                intArrayOf(157, 18),
                intArrayOf(122, 21),
                intArrayOf(148, 21),
            )
        for (s in spots) {
            windows.add(s)
            castle.put(s[0], s[1], jcol("#0a0408"), 2f)
            castle.put(s[0], s[1] + 1, jcol("#0a0408"), 2f)
        }
        // Foreground hill with a few dead trees, in silhouette.
        for (x in 0 until JCOLS) {
            val top = 43f + 2.5f * sin(x * 0.03f + 1f) + 1.2f * sin(x * 0.11f)
            for (y in top.toInt() until JROWS) {
                castle.put(
                    x,
                    y,
                    jcol("#08040a").mix(
                        jcol("#24101c"),
                        jsmooth(2f, 0f, y - top) * 0.6f,
                    ),
                    2f,
                )
            }
        }
        for (tx in intArrayOf(18, 64, 182)) {
            val base = (43f + 2.5f * sin(tx * 0.03f + 1f)).toInt()
            for (y in base - 9..base) castle.put(tx, y, jcol("#08040a"), 2f, if (y < base - 6) '|' else 0.toChar())
            castle.put(tx - 1, base - 6, jcol("#08040a"), 1f, '\\')
            castle.put(tx - 2, base - 7, jcol("#08040a"), 1f, '\\')
            castle.put(tx + 1, base - 4, jcol("#08040a"), 1f, '/')
            castle.put(tx + 2, base - 5, jcol("#08040a"), 1f, '/')
        }
    }

    private class Pose {
        var x = 0f
        var y = 0f
        var s = 1f
        var dir = 1f
        var depth = 0f
    }

    private val pose = Pose()

    private fun place(t: Float) {
        val phi = JTAU * t / LOOP
        pose.x = CX + 66f * cos(phi)
        pose.depth = sin(phi)
        pose.y = 15f + 9f * sin(phi) - 3f * cos(phi)
        pose.s = 0.7f + 0.32f * pose.depth
        pose.dir = if (sin(phi) > 0f) -1f else 1f
    }

    fun draw(
        t: Float,
        grid: Grid,
    ) {
        frame.copyFrom(sky)
        stars(t)
        place(t)
        val flap = JTAU * 18f * t / LOOP
        if (pose.depth < 0f) dragon(flap, t)
        frame.over(castle)
        windowsAndBeacon(t)
        if (pose.depth >= 0f) dragon(flap, t)
        breath(t)
        frame.output(grid, palette)
    }

    private fun stars(t: Float) {
        for (s in 0 until STARS) {
            val tw = 0.5f + 0.5f * sin(JTAU * (3f * t / LOOP + starPhase[s]))
            if (tw < 0.35f) continue
            frame.put(starX[s], starY[s], jcol("#c0a0d0").scale(0.6f + 0.4f * tw), 1f, if (tw > 0.85f) '+' else '.')
        }
    }

    private fun windowsAndBeacon(t: Float) {
        for ((k, w) in windows.withIndex()) {
            val f = 0.75f + 0.25f * sin(JTAU * ((3 + k % 4) * t / LOOP) + k * 1.3f)
            frame.put(w[0], w[1], jcol("#ffd870").scale(f), 2f)
            frame.put(w[0], w[1] + 1, jcol("#ff9a40").scale(f), 2f)
            frame.glow(w[0] + 0.5f, w[1] + 1f, 2.2f, jcol("#ff8a30"), 0.25f * f)
        }
        // Beacon fire on the tall tower.
        for (y in 2..8) {
            for (x in (BEACON_X - 4f).toInt()..(BEACON_X + 4f).toInt()) {
                val dx = x + 0.5f - BEACON_X
                val h = (BEACON_Y + 1f - y) / 6f
                val n =
                    lat.at(x * 0.7f, y * 0.9f + JTAU * 6f * t / LOOP * 0.6f * 2f) +
                        0.3f * sin(JTAU * 10f * t / LOOP + x)
                val w = (1f - h) * 3.6f * (0.7f + 0.5f * n)
                if (abs(dx) > w || h < 0f) continue
                val k = jclamp(1.1f - h - abs(dx) / max(w, 0.5f) * 0.5f + (n - 0.5f) * 0.4f)
                if (k < 0.15f) continue
                val c = flameAt(k)
                frame.put(x, y, c, if (k > 0.4f) 2f else 1f, if (k > 0.4f) 0.toChar() else '^')
            }
        }
        val flick = 0.8f + 0.2f * sin(JTAU * 12f * t / LOOP)
        frame.glow(BEACON_X, BEACON_Y - 1f, 16f, jcol("#ff7a20"), 0.42f * flick)
    }

    private fun flameAt(k: Float): JCol {
        val p = jclamp(k) * (flame.size - 1)
        val i = min(p.toInt(), flame.size - 2)
        return flame[i].mix(flame[i + 1], p - i)
    }

    private fun segDist(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ): Float {
        val vx = bx - ax
        val vy = by - ay
        val s = jclamp(((px - ax) * vx + (py - ay) * vy) / (vx * vx + vy * vy))
        val ex = px - ax - vx * s
        val ey = py - ay - vy * s
        return sqrt(ex * ex + ey * ey)
    }

    private fun inTri(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
    ): Boolean {
        val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
        val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
        val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
        return !((d1 < 0f || d2 < 0f || d3 < 0f) && (d1 > 0f || d2 > 0f || d3 > 0f))
    }

    private fun dragon(
        flap: Float,
        t: Float,
    ) {
        val s = pose.s
        val up = 0.3f + 0.7f * sin(flap)
        val reachX = 46f * s
        val reachY = 30f * s / JROW_U
        val tailWave = sin(flap * 0.5f + 1f)
        val fireGlow = if (t in FIRE_A..FIRE_B) 1f else 0f
        for (y in max(0, (pose.y - reachY).toInt())..min(JROWS - 1, (pose.y + reachY).toInt())) {
            for (x in max(0, (pose.x - reachX).toInt())..min(JCOLS - 1, (pose.x + reachX).toInt())) {
                val lx = (x + 0.5f - pose.x) * pose.dir / s
                val ly = (y + 0.5f - pose.y) * JROW_U / s
                val c = dragonAt(lx, ly, up, tailWave, fireGlow) ?: continue
                frame.put(x, y, c, 2f)
            }
        }
    }

    /** The dragon in local width units, facing +x, y down; null outside it. */
    private fun dragonAt(
        lx: Float,
        ly: Float,
        up: Float,
        tailWave: Float,
        fire: Float,
    ): JCol? {
        // Underside catches the sunset and the fire.
        fun shade(
            base: JCol,
            v: Float,
        ): JCol = base.mix(hideLit, jclamp(v) * (0.55f + 0.35f * fire))
        // Head and snout.
        val hx = 21f
        val hy = -8f
        if (((lx - hx) / 4f).let { it * it } + ((ly - hy) / 2.6f).let { it * it } <= 1f ||
            inTri(lx, ly, hx + 1f, hy - 2f, hx + 1f, hy + 2.2f, hx + 9f, hy + 0.6f)
        ) {
            if (abs(lx - (hx + 1.5f)) < 1f && abs(ly - (hy - 1f)) < 0.9f) return jcol("#ffd870")
            return shade(hide, (ly - hy) / 3f + 0.2f)
        }
        // Horns.
        if (inTri(lx, ly, hx - 3f, hy - 1.5f, hx - 1f, hy - 2.5f, hx - 7f, hy - 6f)) return jcol("#e8d8a0")
        // Neck.
        if (segDist(lx, ly, 6f, -1f, hx - 1f, hy) < 2.6f - (lx - 6f) * 0.05f) return shade(hide, (ly + 1f) / 4f)
        // Wings: near wing over the body, membrane between bony fingers.
        val sx = 2f
        val sy = -4f
        val ex = sx - 4f
        val ey = sy - 14f * up
        val tx = sx - 22f
        val ty = sy - 22f * up + 4f
        val rx = -12f
        val ry = -1f
        if (inTri(lx, ly, sx, sy, ex, ey, rx, ry) || inTri(lx, ly, ex, ey, tx, ty, rx, ry)) {
            val bone =
                segDist(lx, ly, sx, sy, ex, ey) < 0.9f || segDist(lx, ly, ex, ey, tx, ty) < 0.8f ||
                    segDist(lx, ly, ex, ey, (tx + rx) * 0.5f, (ty + ry) * 0.5f + 2f) < 0.7f
            if (bone) return jcol("#0c2a24")
            return wing.mix(jcol("#7ac8a0"), 0.2f + 0.2f * up).mix(hideLit, 0.35f * fire)
        }
        // Body.
        if ((lx / 9.5f).let { it * it } + (ly / 4f).let { it * it } <= 1f) return shade(hide, ly / 4f + 0.3f)
        // Legs tucked under.
        if (segDist(lx, ly, 5f, 2f, 7f, 6f) < 1.2f ||
            segDist(lx, ly, -5f, 2.5f, -3f, 6.5f) < 1.3f
        ) {
            return shade(hide, 0.4f)
        }
        // Tail, tapering and waving, with a spade tip.
        var px = -8f
        var py = 0f
        for (q in 1..8) {
            val p = q / 8f
            val nx = -8f - p * 28f
            val ny = 1f + p * 3f + tailWave * 5f * p * p
            if (segDist(lx, ly, px, py, nx, ny) < 2.4f - p * 1.8f) return shade(hide, 0.25f)
            px = nx
            py = ny
        }
        if (inTri(lx, ly, px + 1f, py, px - 4f, py - 2.4f, px - 3f, py + 2.6f)) return wing
        return null
    }

    private fun breath(t: Float) {
        if (t !in FIRE_A..FIRE_B) return
        val s = pose.s
        val mx = pose.x + pose.dir * 30f * s
        val my = pose.y - 7.5f * s / JROW_U
        val age = (t - FIRE_A) / (FIRE_B - FIRE_A)
        val fade = jsmooth(0f, 0.1f, age) * jsmooth(1f, 0.75f, age)
        val len = 44f * s
        for (y in max(0, (my - 8f).toInt())..min(JROWS - 1, (my + 10f).toInt())) {
            for (x in 0 until JCOLS) {
                val ax = (x + 0.5f - mx) * pose.dir
                if (ax < 0f || ax > len) continue
                val p = ax / len
                val ay = (y + 0.5f - my - p * p * 5f) * JROW_U
                val w = 1.5f + p * 9f * s
                if (abs(ay) > w) continue
                val n = lat.at(ax * 0.35f - JTAU * 12f * t / LOOP * 1.5f, y * 0.8f)
                val k = jclamp((1f - abs(ay) / w) * (1.15f - p * 0.8f) + (n - 0.5f) * 0.6f) * fade
                if (k < 0.12f) continue
                frame.put(
                    x,
                    y,
                    flameAt(k),
                    if (k >
                        0.45f
                    ) {
                        2f
                    } else {
                        1f
                    },
                    if (k > 0.45f) {
                        0.toChar()
                    } else if (n > 0.5f) {
                        '*'
                    } else {
                        '•'
                    },
                )
            }
        }
        frame.glow(mx + pose.dir * len * 0.4f, my + 1f, 24f, jcol("#ff6a20"), 0.35f * fade)
    }
}

/**
 * A dragon circles a castle on a crag at dusk, wings beating, swinging behind the keep and back out in front, and
 * once per loop it breathes a plume of fire. A beacon burns on the tall tower, windows flicker and clouds glow from a
 * sun that has just set. The seed shapes the clouds, the rock and the stars. Every motion repeats a whole number of
 * times per loop.
 */
public fun dragonCastlePiece(seed: Int = 0): AsciiPiece {
    val scene = DragonCastle(seed)
    return asciiPiece(
        name = "dragon over castle",
        ground = Rgb.fromHex("#0a0614"),
        category = "creatures",
        cols = JCOLS,
        rows = JROWS,
        fps = 15,
        still = 4.3f,
        loop = LOOP,
        note = "a dragon circles a castle on a crag at dusk, wings beating, and breathes fire as it passes",
    ) { time, grid -> scene.draw(loopTime(time, LOOP), grid) }
}

/**
 * A dragon circling a castle on a crag at dusk, breathing fire as it passes in front of the keep.
 *
 * @param seed shapes the clouds, the rock and the stars.
 */
@Composable
public fun AsciiDragonCastle(
    modifier: Modifier = Modifier,
    seed: Int = 0,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A dragon circling a castle on a crag at dusk, breathing fire",
) {
    val piece = remember(seed) { dragonCastlePiece(seed) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
