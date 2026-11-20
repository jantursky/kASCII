package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.kascii.compose.kasciiFontFamily

/** The colors of the gallery's own chrome: the tokens of design/DESIGN.md, dark native and light derived. */
@Immutable
class GalleryColors(
    val void: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val strong: Color,
    val muted: Color,
    val border: Color,
    val link: Color,
    val green: Color,
    val greenFill: Color,
    val amber: Color,
    val amberFill: Color,
    val red: Color,
    val redFill: Color,
    val code: Color,
    val dark: Boolean,
) {
    companion object {
        val Dark =
            GalleryColors(
                void = Color(0xFF06051D),
                surface1 = Color(0xFF0F1C36),
                surface2 = Color(0xFF1D293D),
                surface3 = Color(0xFF314062),
                text = Color(0xFFCAD5E2),
                strong = Color(0xFFFFFFFF),
                muted = Color(0xA6CAD5E2),
                border = Color(0x1FE5E7EB),
                link = Color(0xFF63B3ED),
                green = Color(0xFF00BC7D),
                greenFill = Color(0x33004F3B),
                amber = Color(0xFFF0B100),
                amberFill = Color(0x33733E0A),
                red = Color(0xFFFF2056),
                redFill = Color(0x338B0836),
                code = Color(0xFF0F1C36),
                dark = true,
            )
        val Light =
            GalleryColors(
                void = Color(0xFFF4F6FB),
                surface1 = Color(0xFFE8EDF6),
                surface2 = Color(0xFFDBE3F0),
                surface3 = Color(0xFFC9D3E6),
                text = Color(0xFF1D293D),
                strong = Color(0xFF06051D),
                muted = Color(0xB31D293D),
                border = Color(0x1F1D293D),
                link = Color(0xFF1D6FB8),
                green = Color(0xFF00785A),
                greenFill = Color(0x3300BC7D),
                amber = Color(0xFF9A6A00),
                amberFill = Color(0x33F0B100),
                red = Color(0xFFC2143F),
                redFill = Color(0x29FF2056),
                code = Color(0xFFE8EDF6),
                dark = false,
            )
    }
}

/** The scale of the design system: caption 14, body 16, heading 30, all weight 400 and monospace. */
@Immutable
class GalleryType(
    val hero: TextStyle,
    val heading: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
)

val LocalGalleryColors = compositionLocalOf { GalleryColors.Dark }
val LocalGalleryType =
    compositionLocalOf {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        GalleryType(base, base, base, base, base, base)
    }

/** The colors and type of the chrome. */
object Gallery {
    val colors: GalleryColors @Composable get() = LocalGalleryColors.current
    val type: GalleryType @Composable get() = LocalGalleryType.current
}

@Composable
fun GalleryTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val mono = kasciiFontFamily()
    val type =
        remember(mono) {
            fun style(
                size: Int,
                line: Int,
            ) = TextStyle(fontFamily = mono, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight.Normal)
            GalleryType(
                hero = style(40, 50),
                heading = style(30, 38),
                title = style(20, 28),
                body = style(16, 24),
                caption = style(14, 23),
                code = style(14, 22),
            )
        }
    CompositionLocalProvider(
        LocalGalleryColors provides if (dark) GalleryColors.Dark else GalleryColors.Light,
        LocalGalleryType provides type,
    ) {
        SelectionContainer(content = content)
    }
}
