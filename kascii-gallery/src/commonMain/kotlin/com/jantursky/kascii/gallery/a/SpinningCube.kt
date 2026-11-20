package com.jantursky.kascii.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import com.jantursky.kascii.core.asciiPiece
import com.jantursky.kascii.core.line
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val COLS = 60
private const val ROWS = 24
private const val LOOP = 12f
private const val CAMERA = 4f
private const val FOCAL = 16f

private class CubeEdge(val a: Int, val b: Int)

private val VERTICES = List(8) { i ->
    floatArrayOf(if (i and 1 == 0) -1f else 1f, if (i and 2 == 0) -1f else 1f, if (i and 4 == 0) -1f else 1f)
}

private val EDGES = listOf(
    CubeEdge(0, 1), CubeEdge(2, 3), CubeEdge(4, 5), CubeEdge(6, 7),
    CubeEdge(0, 2), CubeEdge(1, 3), CubeEdge(4, 6), CubeEdge(5, 7),
    CubeEdge(0, 4), CubeEdge(1, 5), CubeEdge(2, 6), CubeEdge(3, 7),
)

private class CubePoint(val x: Int, val y: Int, val z: Float)

/**
 * A wireframe cube turning in perspective. Edges behind the middle are drawn with a thin glyph, edges in front
 * with a heavy one, and the corners are marked. One loop is two turns about the vertical axis and one about the
 * horizontal axis.
 */
public fun cubePiece(color: Rgb = AKit.Green, corner: Rgb = AKit.Amber): AsciiPiece = asciiPiece(
    name = "cube",
    category = "shapes",
    cols = COLS,
    rows = ROWS,
    still = 1.2f,
    loop = LOOP,
    note = "a wireframe cube turning in perspective",
) { t, grid ->
    val ay = AKit.TAU * t / (LOOP / 2)
    val ax = 0.6f + AKit.TAU * t / LOOP
    val points = VERTICES.map { v -> project(v, ax, ay) }
    EDGES.sortedByDescending { points[it.a].z + points[it.b].z }.forEach { e ->
        val p = points[e.a]
        val q = points[e.b]
        val front = (p.z + q.z) / 2f < 0f
        grid.line(p.x, p.y, q.x, q.y, if (front) '#' else '.', color)
    }
    points.sortedByDescending { it.z }.forEach { grid.put(it.x, it.y, if (it.z < 0f) '@' else 'o', corner) }
}

private fun project(v: FloatArray, ax: Float, ay: Float): CubePoint {
    val x1 = v[0] * cos(ay) + v[2] * sin(ay)
    val z1 = -v[0] * sin(ay) + v[2] * cos(ay)
    val y2 = v[1] * cos(ax) - z1 * sin(ax)
    val z2 = v[1] * sin(ax) + z1 * cos(ax)
    val s = FOCAL / (CAMERA + z2)
    return CubePoint((COLS / 2f + x1 * s * 2f).roundToInt(), (ROWS / 2f - y2 * s).roundToInt(), z2)
}

/**
 * A wireframe cube turning in perspective.
 *
 * @param color the edges.
 * @param corner the corner marks.
 */
@Composable
public fun SpinningCube(
    modifier: Modifier = Modifier,
    color: Rgb = AKit.Green,
    corner: Rgb = AKit.Amber,
    speed: Float = 1f,
    motion: Motion = Motion.System,
    renderer: AsciiRenderer = AsciiRenderer.Auto,
    contentDescription: String? = "A wireframe cube turning in space",
) {
    val piece = remember(color, corner) { cubePiece(color, corner) }
    AsciiPieceView(piece, modifier, speed, motion, renderer, contentDescription = contentDescription)
}
