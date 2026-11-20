package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.ui.graphics.ImageBitmap
import com.jantursky.kascii.compose.toImageBitmap
import com.jantursky.kascii.core.Export
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme

enum class ExportFormat(
    val label: String,
) {
    Text("Text"),
    Ansi("ANSI"),
    Svg("SVG"),
}

// A frame is a Grid, and a Grid exports to text, ANSI and SVG in common code. PNG needs a bitmap, so it lives in
// the Compose module: Grid.toImageBitmap.
fun Grid.export(
    format: ExportFormat,
    theme: KasciiTheme,
): String =
    when (format) {
        ExportFormat.Text -> Export.toText(this, trimEnd = true)
        ExportFormat.Ansi -> Export.toAnsi(this)
        ExportFormat.Svg -> Export.toSvg(this, theme)
    }

fun Grid.toPng(theme: KasciiTheme): ImageBitmap = toImageBitmap(theme)
