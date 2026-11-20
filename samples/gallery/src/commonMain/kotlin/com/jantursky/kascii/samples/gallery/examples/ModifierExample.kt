package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.compose.SampledColors
import com.jantursky.kascii.compose.ascii
import com.jantursky.kascii.core.Charset
import com.jantursky.kascii.core.ColorMode
import com.jantursky.kascii.core.KasciiTheme

// Modifier.ascii draws whatever it modifies, then samples it into glyphs. SampledColors gives each glyph the
// color of the pixels under it; any other color mode ignores the content's colors.
@Composable
fun ModifierExample(
    charset: Charset,
    cell: Dp,
    sampled: Boolean,
    theme: KasciiTheme,
    modifier: Modifier = Modifier,
) {
    ProvideKasciiTheme(theme) {
        Box(
            modifier
                .fillMaxWidth()
                .height(220.dp)
                .ascii(charset, cell, if (sampled) SampledColors else ColorMode.Theme)
                .background(Brush.linearGradient(listOf(Color(0xFF7F52FF), Color(0xFFE44857), Color(0xFFF0B100)))),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("Kascii", style = TextStyle(fontSize = 72.sp, color = Color.White))
        }
    }
}
