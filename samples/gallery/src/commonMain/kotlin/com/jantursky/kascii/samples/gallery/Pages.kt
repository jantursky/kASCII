package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.core.Rasterizer
import com.jantursky.kascii.gallery.GalleryEntry
import com.jantursky.kascii.samples.gallery.examples.BuiltInThemes
import com.jantursky.kascii.samples.gallery.examples.FieldKind
import com.jantursky.kascii.samples.gallery.examples.FieldsExample
import com.jantursky.kascii.samples.gallery.examples.ImageExample
import com.jantursky.kascii.samples.gallery.examples.ModifierExample
import com.jantursky.kascii.samples.gallery.examples.RenderersExample
import com.jantursky.kascii.samples.gallery.examples.ThemeExample
import com.jantursky.kascii.samples.gallery.examples.field
import kotlin.math.roundToInt

private const val EXPORT_COLS = 72
private const val EXPORT_ROWS = 28
private const val STILL = 1.5f

/** One piece, large, with its controls and exports. */
@Composable
fun PiecePage(
    entry: GalleryEntry,
    state: GalleryState,
) {
    if (state.embed) {
        PieceStage(entry, state)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PieceStage(entry, state)
        Controls {
            SpeedControl(state)
            SeedControl(state)
            RendererControl(state)
            MotionControl(state)
            ThemeControl(state)
        }
        ExportBar(state.theme, entry.id) { entry.create(state.seed).render() }
        PieceUsage(entry)
    }
}

/** How to put the piece in an app: the dependency, the line that builds it and a link to its source. */
@Composable
private fun PieceUsage(entry: GalleryEntry) {
    val source = PieceSources[entry.id] ?: return
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading("Use it in your app")
        CodePanel(pieceSnippet(source))
        TextLink("View the source of ${entry.title} on GitHub", { uri.openUri(SOURCE_URL + source.file) })
    }
}

private const val SOURCE_URL =
    "https://github.com/jantursky/kASCII/blob/main/kascii-gallery/src/commonMain/kotlin/com/jantursky/kascii/gallery/"

internal fun pieceSnippet(source: PieceSource): String =
    """
    // build.gradle.kts: implementation("io.github.jantursky:kascii-gallery:$GALLERY_VERSION")
    import com.jantursky.kascii.compose.AsciiPieceView
    import ${source.pkg}.${source.factory}

    @Composable
    fun Scene(modifier: Modifier = Modifier) {
        val piece = remember { ${source.factory}(0) }
        AsciiPieceView(
            piece,
            modifier.fillMaxWidth().aspectRatio(piece.meta.cols / (piece.meta.rows * 2.2f)),
        )
    }
    """.trimIndent()

/** Procedural fields by charset by color mode. The previews are the pickers. */
@Composable
fun FieldsPage(state: GalleryState) {
    var kind by remember { mutableStateOf(FieldKind.Plasma) }
    var cell by remember { mutableFloatStateOf(8f) }
    val paper = Color(state.theme.paper.argb)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Stage(state, minHeight = 360) {
            FieldsExample(
                kind,
                state.seed,
                state.charset,
                state.colors.mode,
                cell.dp,
                state.speed,
                state.motion,
                state.theme,
                Modifier.fillMaxWidth().height(320.dp),
                state.renderer,
            )
        }
        if (state.embed) return@Column
        Controls {
            Labeled("Field") { Choice(FieldKind.entries, kind, { kind = it }, { it.label }) }
            CellControl(cell, { cell = it })
            SpeedControl(state)
            SeedControl(state)
            RendererControl(state)
            MotionControl(state)
            ThemeControl(state)
        }
        Labeled("Charset") {
            PreviewPicker(
                Charsets.All,
                state.charset,
                { state.charset = it },
                { it.name },
                paper,
            ) { charset, modifier -> FieldPreview(state, kind, charset, state.colors.mode, modifier) }
        }
        Labeled("Color mode") {
            PreviewPicker(
                ColorChoice.entries,
                state.colors,
                { state.colors = it },
                { it.label },
                paper,
            ) { choice, modifier -> FieldPreview(state, kind, state.charset, choice.mode, modifier) }
        }
        ExportBar(state.theme, "kascii-${kind.label.lowercase()}") {
            Rasterizer.rasterize(
                kind.field(state.seed),
                state.charset,
                EXPORT_COLS,
                EXPORT_ROWS,
                t = STILL,
                cellAspect = CELL_ASPECT,
                colorMode = state.colors.mode,
                theme = state.theme,
            )
        }
    }
}

/** A still thumbnail of a field with one charset and color mode. */
@Composable
private fun FieldPreview(
    state: GalleryState,
    kind: FieldKind,
    charset: Charset,
    colorMode: ColorMode,
    modifier: Modifier,
) = FieldsExample(kind, state.seed, charset, colorMode, 5.dp, 1f, Motion.Reduced, state.theme, modifier)

/** The same field through the Cpu, Gpu and Text renderers. */
@Composable
fun RenderersPage(state: GalleryState) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Stage(state, minHeight = 260) { RenderersExample(state.motion) }
        if (state.embed) return@Column
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                "Cpu: a glyph atlas, everywhere",
                "Gpu: a shader, Cpu where there is none",
                "Text: real selectable text",
            ).forEach {
                BasicText(it, Modifier.weight(1f), style = Gallery.type.caption.copy(color = Gallery.colors.muted))
            }
        }
        Controls {
            MotionControl(state)
            ThemeControl(state)
        }
    }
}

/** Modifier.ascii over a gradient box, with and without sampled colors, and AsciiImage over a bitmap. */
@Composable
fun ModifierPage(state: GalleryState) {
    var cell by remember { mutableFloatStateOf(8f) }
    val charsets = listOf(Charsets.Blocks, Charsets.Standard, Charsets.Dots, Charsets.Quadrants, Charsets.Braille)
    var charset by remember { mutableStateOf<Charset>(Charsets.Blocks) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Stage(state, minHeight = 240) { ModifierExample(charset, cell.dp, sampled = false, theme = state.theme) }
        Stage(state, minHeight = 240) { ModifierExample(charset, cell.dp, sampled = true, theme = state.theme) }
        Stage(state, minHeight = 240) { ImageExample(charset, cell.dp, state.colors.mode, state.theme) }
        if (state.embed) return@Column
        Controls {
            Labeled("Charset") { Choice(charsets, charset, { charset = it }, { it.name }) }
            CellControl(cell, { cell = it })
            ThemeControl(state)
        }
    }
}

/** The built-in themes side by side, with their contrast. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePage(state: GalleryState) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BuiltInThemes.forEach { (name, theme) ->
            Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeExample(theme, state.motion)
                val contrast = (theme.contrastRatio() * 10).roundToInt() / 10f
                BasicText(
                    "$name, contrast $contrast:1",
                    style = Gallery.type.caption.copy(color = Gallery.colors.muted),
                )
                if (!state.embed) Pill("Use $name", { state.themeOverride = theme })
            }
        }
    }
}
