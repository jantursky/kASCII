package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBigText
import com.jantursky.kascii.components.AsciiDissolveText
import com.jantursky.kascii.components.AsciiGlitchText
import com.jantursky.kascii.components.AsciiMarquee
import com.jantursky.kascii.components.AsciiMorse
import com.jantursky.kascii.components.AsciiRainbowText
import com.jantursky.kascii.components.AsciiScrambleText
import com.jantursky.kascii.components.AsciiShimmerText
import com.jantursky.kascii.components.AsciiSplitFlap
import com.jantursky.kascii.components.AsciiTypewriter
import com.jantursky.kascii.components.AsciiWaveText

// Text effects are plain text to a screen reader and hold their final state under reduced motion.
@Composable
fun TextEffectsExample(modifier: Modifier = Modifier) {
    Column(modifier, Arrangement.spacedBy(12.dp)) {
        AsciiBigText("KASCII")
        AsciiTypewriter("Typed one glyph at a time.")
        AsciiScrambleText("Scrambled into place")
        AsciiGlitchText("Glitch in the signal")
        AsciiDissolveText("Dissolving into view")
        AsciiMarquee("Marquee scrolling past the edge of its box. ", cols = 32)
        AsciiWaveText("A wave runs through it")
        AsciiSplitFlap("DEPARTURES")
        AsciiMorse("SOS", symbolsPerSecond = 4f)
        AsciiRainbowText("Rainbow from the theme palette")
        AsciiShimmerText("Shimmer sweeps across")
    }
}
