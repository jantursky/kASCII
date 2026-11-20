package com.jantursky.kascii.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rgb
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RendererTest {
    // A 10 px cell is 22 px tall with the bundled font: 16.67 px glyphs on a 1.32 em line.
    private val blockThenSpace = Grid(2, 1).apply { put(0, 0, '█') }

    @Test
    fun manyDistinctColorsStillDrawWithTheCpuPainter() =
        runAsciiTest {
            // Many distinct colors: each gets its own tint filter, and none must break drawing.
            val grid = Grid(20, 9)
            for (i in 0 until 180) grid.put(i % 20, i / 20, '█', Rgb(0xFF000000.toInt() or (i * 997 + 1)))
            setAsciiContent {
                AsciiGridView(grid, Modifier.size(200.dp, 198.dp), renderer = AsciiRenderer.Cpu)
            }
            assertTrue(capture().countOf(Paper, 0, 0, 200, 198) < 200 * 198)
        }

    @Test
    fun theAtlasPainterDrawsGlyphsInInkOnPaper() =
        runAsciiTest {
            setAsciiContent { AsciiGridView(blockThenSpace, Modifier.size(20.dp, 22.dp)) }
            val image = capture()
            assertEquals(220, image.countOf(Ink, 0, 0, 10, 22))
            assertEquals(220, image.countOf(Paper, 10, 0, 20, 22))
        }

    @Test
    fun cellsWithTheirOwnColorAreTintedWithIt() =
        runAsciiTest {
            val red = Rgb(255, 0, 0)
            val grid = Grid(1, 1).apply { put(0, 0, '█', red) }
            setAsciiContent { AsciiGridView(grid, Modifier.size(10.dp, 22.dp)) }
            assertEquals(220, capture().countOf(red.toColor(), 0, 0, 10, 22))
        }

    @Test
    fun theShaderDrawsTheSamePixelsAsTheAtlasPainter() {
        val field = Fields.plasma()

        fun draw(renderer: AsciiRenderer) =
            snapshot {
                AsciiCanvas(
                    field,
                    Modifier.size(200.dp),
                    charset = Charsets.Dense,
                    cell = 6.dp,
                    colorMode = ColorMode.Gradient(listOf(Rgb(0, 0, 255), Rgb(255, 128, 0))),
                    motion = Motion.Reduced,
                    renderer = renderer,
                )
            }
        val cpu = draw(AsciiRenderer.Cpu)
        val gpu = draw(AsciiRenderer.Gpu)
        assertTrue(cpu.countOf(Paper) < 200 * 200 * 0.9, "the field draws glyphs")
        assertEquals(0, cpu.pixelsDifferentFrom(gpu, ONE_STEP))
    }

    @Test
    fun autoDrawsLargeGridsWithTheShaderAndSmallOnesWithTheAtlas() {
        fun draw(
            renderer: AsciiRenderer,
            cell: Int,
        ) = snapshot {
            val modifier = Modifier.size(200.dp)
            AsciiCanvas(Fields.waves(), modifier, cell = cell.dp, motion = Motion.Reduced, renderer = renderer)
        }
        // 2 px cells make 100 x 23 cells, over the threshold; 10 px cells make 20 x 10.
        assertEquals(0, draw(AsciiRenderer.Auto, 2).pixelsDifferentFrom(draw(AsciiRenderer.Gpu, 2)))
        val apart = draw(AsciiRenderer.Cpu, 2).pixelsDifferentFrom(draw(AsciiRenderer.Gpu, 2))
        assertTrue(apart > 0, "the two renderers are told apart by rounding")
        assertEquals(0, draw(AsciiRenderer.Auto, 10).pixelsDifferentFrom(draw(AsciiRenderer.Cpu, 10)))
    }

    @Test
    fun aViewThatStartsEmptyStillGetsTheShaderOnceItHasASize() {
        lateinit var gpu: ImageBitmap
        runAsciiTest {
            var side by mutableStateOf(0)
            setAsciiContent {
                Plasma(side, AsciiRenderer.Gpu)
                // An empty element has nothing to sample.
                Box(Modifier.size(0.dp).ascii(renderer = AsciiRenderer.Gpu))
            }
            side = 200
            mainClock.advanceTimeByFrame()
            gpu = capture()
        }
        assertEquals(0, gpu.pixelsDifferentFrom(snapshot { Plasma(200, AsciiRenderer.Cpu) }, ONE_STEP))
    }

    @Test
    fun theTextRendererShowsTheGridAsText() =
        runAsciiTest {
            val grid =
                Grid(5, 2).apply {
                    write(0, 0, "hello")
                    write(0, 1, "⣿ノ█", Rgb(0, 255, 0))
                }
            setAsciiContent { AsciiGridView(grid, Modifier.size(100.dp, 50.dp), renderer = AsciiRenderer.Text) }
            onNode(hasText(grid.toText())).assertExists()
            assertTrue(capture().countOf(Paper) > 0)
        }

    @Test
    fun anImageIsDrawnAsGlyphs() =
        runAsciiTest {
            // White on the left half, transparent on the right.
            val bitmap = ImageBitmap(20, 10)
            Canvas(bitmap).drawRect(0f, 0f, 10f, 10f, Paint().apply { color = Color.White })
            setAsciiContent {
                AsciiImage(bitmap, Modifier.size(20.dp, 22.dp), Charsets.Blocks, 10.dp, renderer = AsciiRenderer.Cpu)
            }
            waitUntil(timeoutMillis = 5_000) {
                mainClock.advanceTimeByFrame()
                capture().countOf(Ink, 0, 0, 10, 22) == 220
            }
            assertEquals(220, capture().countOf(Paper, 10, 0, 20, 22))
        }

    @Test
    fun screenshots() {
        val grid =
            Grid(24, 6).apply {
                write(1, 1, "kascii ░▒▓█ ⣿⡇⠁")
                write(1, 2, "┌──┐ ・ノヲ力", Rgb(0, 188, 125))
                write(1, 3, "└──┘ 0123", Rgb(240, 177, 0))
            }
        for (renderer in listOf(AsciiRenderer.Cpu, AsciiRenderer.Gpu, AsciiRenderer.Text)) {
            runAsciiTest(240, 132) {
                setAsciiContent { Box { AsciiGridView(grid, Modifier.size(240.dp, 132.dp), renderer = renderer) } }
                onRoot().captureRoboImage("src/jvmTest/screenshots/grid_${renderer.name.lowercase()}.png")
            }
        }
    }
}

@Composable
private fun Plasma(
    side: Int,
    renderer: AsciiRenderer,
) {
    AsciiCanvas(Fields.plasma(), Modifier.size(side.dp), cell = 6.dp, motion = Motion.Reduced, renderer = renderer)
}
