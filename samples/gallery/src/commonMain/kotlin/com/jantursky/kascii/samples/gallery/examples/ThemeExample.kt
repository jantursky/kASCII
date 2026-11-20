package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiCanvas
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.Charsets
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.Fields
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion

/** The built-in themes, by name. Build your own with `KasciiTheme(ink, paper, accent, palette)`. */
val BuiltInThemes: List<Pair<String, KasciiTheme>> =
    listOf(
        "Light" to KasciiTheme.Light,
        "Dark" to KasciiTheme.Dark,
        "Phosphor" to KasciiTheme.Phosphor,
        "Amber" to KasciiTheme.Amber,
    )

// A theme is ink, paper, an accent and a palette. Everything inside ProvideKasciiTheme reads it: ColorMode.Theme
// draws in the ink, ColorMode.Palette picks from the palette by tone. The paper is the page the glyphs sit on, so
// the host draws it.
@Composable
fun ThemeExample(
    theme: KasciiTheme,
    motion: Motion,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(Color(theme.paper.argb)).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProvideKasciiTheme(theme) {
            listOf(ColorMode.Theme, ColorMode.Palette).forEach { colorMode ->
                AsciiCanvas(
                    Fields.plasma(),
                    Modifier.weight(1f).height(160.dp),
                    charset = Charsets.Standard,
                    colorMode = colorMode,
                    stillTime = 1.5f,
                    motion = motion,
                    contentDescription = "A plasma field in this theme",
                )
            }
        }
    }
}
