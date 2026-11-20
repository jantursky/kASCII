package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiPieceView
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.compose.ProvideKasciiTheme
import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.gallery.GalleryEntry

// A piece is a function of time. AsciiPieceView plays it at its own size, pauses it offscreen and shows its still
// frame when the platform asks for reduced motion (or when motion is Motion.Reduced).
@Composable
fun PieceExample(
    entry: GalleryEntry,
    seed: Int,
    speed: Float,
    motion: Motion,
    renderer: AsciiRenderer,
    theme: KasciiTheme,
    modifier: Modifier = Modifier,
    piece: AsciiPiece = remember(entry, seed) { entry.create(seed) },
) {
    ProvideKasciiTheme(theme) {
        AsciiPieceView(
            piece,
            modifier,
            speed = speed,
            motion = motion,
            renderer = renderer,
            contentDescription = "${entry.title}, ${entry.category} ASCII art",
        )
    }
}
