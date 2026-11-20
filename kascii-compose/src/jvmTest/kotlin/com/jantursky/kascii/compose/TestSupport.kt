package com.jantursky.kascii.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.asciiPiece
import kotlin.math.abs

/** Runs [block] in a [width] by [height] window with a paused clock, so tests step frames themselves. */
@OptIn(ExperimentalTestApi::class)
fun runAsciiTest(
    width: Int = 200,
    height: Int = 200,
    block: ComposeUiTest.() -> Unit,
) = runDesktopComposeUiTest(width, height) {
    mainClock.autoAdvance = false
    block()
}

/** Sets [content] at density 1 with the dark theme. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setAsciiContent(content: @Composable () -> Unit) {
    setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            ProvideKasciiTheme(KasciiTheme.Dark) { content() }
        }
    }
}

/** Runs [content] in its own test window and returns the window's pixels once it has drawn. */
@OptIn(ExperimentalTestApi::class)
fun snapshot(
    width: Int = 200,
    height: Int = 200,
    content: @Composable () -> Unit,
): ImageBitmap {
    lateinit var image: ImageBitmap
    runAsciiTest(width, height) {
        setAsciiContent(content)
        image = capture()
    }
    return image
}

/** The whole test window as pixels. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.capture(): ImageBitmap = onRoot().captureToImage()

val Paper: Color = KasciiTheme.Dark.paper.toColor()
val Ink: Color = KasciiTheme.Dark.ink.toColor()

/** How many pixels in the given rectangle are exactly [color]. */
fun ImageBitmap.countOf(
    color: Color,
    x0: Int = 0,
    y0: Int = 0,
    x1: Int = width,
    y1: Int = height,
): Int {
    val map = toPixelMap()
    var n = 0
    for (y in y0 until y1) for (x in x0 until x1) if (map[x, y] == color) n++
    return n
}

/**
 * How many pixels differ between two images of the same size by more than [tolerance] in any channel. The shader
 * and the atlas painter blend in different precisions, so their pixels may differ by one step of 255.
 */
fun ImageBitmap.pixelsDifferentFrom(
    other: ImageBitmap,
    tolerance: Float = 0f,
): Int {
    val a = toPixelMap()
    val b = other.toPixelMap()
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val p = a[x, y]
            val q = b[x, y]
            val d = maxOf(abs(p.red - q.red), abs(p.green - q.green), abs(p.blue - q.blue), abs(p.alpha - q.alpha))
            if (d > tolerance) n++
        }
    }
    return n
}

/** One step of an 8-bit channel, with room for float rounding. */
const val ONE_STEP: Float = 1.5f / 255f

/** The column of the first cell whose pixels are not all paper, in a one-row grid of [cellWidth] cells. */
fun ImageBitmap.firstInkColumn(cellWidth: Int): Int {
    val map = toPixelMap()
    for (x in 0 until width) for (y in 0 until height) if (map[x, y] != Paper) return x / cellWidth
    return -1
}

/** A white square bitmap [side] pixels wide. */
fun whiteBitmap(side: Int): ImageBitmap =
    ImageBitmap(side, side).also {
        Canvas(it).drawRect(0f, 0f, side.toFloat(), side.toFloat(), Paint().apply { color = Color.White })
    }

/** A ten-cell piece that puts a full block in column `floor(t)`, so a picture tells the time. */
fun stopwatch(
    fps: Int = 30,
    still: Float = 0f,
) = asciiPiece("stopwatch", "test", 10, 1, fps = fps, still = still) { t, grid ->
    grid.put(t.toInt().coerceIn(0, 9), 0, '█')
}
