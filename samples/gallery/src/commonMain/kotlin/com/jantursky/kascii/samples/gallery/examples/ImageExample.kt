package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.compose.AsciiImage
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.samples.gallery.sampleArt

// AsciiImage samples a bitmap once and draws it as glyphs. The bitmap here is drawn in code; any ImageBitmap works.
@Composable
fun ImageExample(
    charset: Charset,
    cell: Dp,
    colorMode: ColorMode,
    theme: KasciiTheme,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember { sampleArt() }
    ProvideKasciiTheme(theme) {
        AsciiImage(
            bitmap,
            modifier.fillMaxWidth().height(220.dp),
            charset = charset,
            cell = cell,
            colorMode = colorMode,
            contentDescription = "Overlapping discs and a diagonal band, drawn in code and shown as ASCII",
        )
    }
}
