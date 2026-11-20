package com.jantursky.kascii.components

import com.jantursky.kascii.core.KasciiTheme
import kotlin.test.Test

class TextEffectsGoldenTest {
    private val theme = KasciiTheme.Dark
    private val idle = ControlState()
    private val focused = ControlState(focused = true)

    @Test
    fun typewriterStart() {
        assertGrid(
            renderTypewriter("Hello", 0.0f, 10f, false),
            "|▌     |",
        )
    }

    @Test
    fun scrambleStart() {
        assertGrid(
            renderScramble("Hello world", 0.0f, 1.5f, 1L, false, theme),
            "|<%1&# =011*|",
        )
    }

    @Test
    fun dissolveStart() {
        assertGrid(
            renderDissolve("Hello world", 0.0f, 1.5f, 1L, false),
            "|           |",
        )
    }

    @Test
    fun splitFlapStart() {
        assertGrid(
            renderSplitFlap("Gate 7", 0.0f, 1L, false),
            "|LRDGYJ|",
        )
    }

    @Test
    fun marqueeStart() {
        assertGrid(
            renderMarquee("Hello", 10, 0.0f, 8f),
            "|Hello    H|",
        )
    }

    @Test
    fun waveStart() {
        assertGrid(
            renderWave("Hello wave", 0.0f, 1),
            "|    o     |",
            "|H  l  w  e|",
            "| el    av |",
        )
    }

    @Test
    fun morseStart() {
        assertGrid(
            renderMorse("SOS go", 0.0f, 6f, false, theme),
            "|··· ··· ··· / ··· ···|",
        )
    }

    @Test
    fun typewriterEarly() {
        assertGrid(
            renderTypewriter("Hello", 0.3f, 10f, false),
            "|Hel▌  |",
        )
    }

    @Test
    fun scrambleEarly() {
        assertGrid(
            renderScramble("Hello world", 0.3f, 1.5f, 1L, false, theme),
            "|He@|0 >?<0%|",
        )
    }

    @Test
    fun dissolveEarly() {
        assertGrid(
            renderDissolve("Hello world", 0.3f, 1.5f, 1L, false),
            "|      .    |",
        )
    }

    @Test
    fun splitFlapEarly() {
        assertGrid(
            renderSplitFlap("Gate 7", 0.3f, 1L, false),
            "|Z1JIYJ|",
        )
    }

    @Test
    fun marqueeEarly() {
        assertGrid(
            renderMarquee("Hello", 10, 0.3f, 8f),
            "|llo    Hel|",
        )
    }

    @Test
    fun waveEarly() {
        assertGrid(
            renderWave("Hello wave", 0.3f, 1),
            "|   lo    e|",
            "|  l     v |",
            "|He    wa  |",
        )
    }

    @Test
    fun morseEarly() {
        assertGrid(
            renderMorse("SOS go", 0.3f, 6f, false, theme),
            "|.·· ··· ··· / ··· ···|",
        )
    }

    @Test
    fun typewriterMiddle() {
        assertGrid(
            renderTypewriter("Hello", 0.8f, 10f, false),
            "|Hello |",
        )
    }

    @Test
    fun scrambleMiddle() {
        assertGrid(
            renderScramble("Hello world", 0.8f, 1.5f, 1L, false, theme),
            "|Hello =+\\=>|",
        )
    }

    @Test
    fun dissolveMiddle() {
        assertGrid(
            renderDissolve("Hello world", 0.8f, 1.5f, 1L, false),
            "|He.*. w.*  |",
        )
    }

    @Test
    fun splitFlapMiddle() {
        assertGrid(
            renderSplitFlap("Gate 7", 0.8f, 1L, false),
            "|MO76K2|",
        )
    }

    @Test
    fun marqueeMiddle() {
        assertGrid(
            renderMarquee("Hello", 10, 0.8f, 8f),
            "|   Hello  |",
        )
    }

    @Test
    fun waveMiddle() {
        assertGrid(
            renderWave("Hello wave", 0.8f, 1),
            "| el    av |",
            "|H  l  w  e|",
            "|    o     |",
        )
    }

    @Test
    fun morseMiddle() {
        assertGrid(
            renderMorse("SOS go", 0.8f, 6f, false, theme),
            "|... ··· ··· / ··· ···|",
        )
    }

    @Test
    fun typewriterSettled() {
        assertGrid(
            renderTypewriter("Hello", 3.0f, 10f, false),
            "|Hello▌|",
        )
    }

    @Test
    fun scrambleSettled() {
        assertGrid(
            renderScramble("Hello world", 3.0f, 1.5f, 1L, false, theme),
            "|Hello world|",
        )
    }

    @Test
    fun dissolveSettled() {
        assertGrid(
            renderDissolve("Hello world", 3.0f, 1.5f, 1L, false),
            "|Hello world|",
        )
    }

    @Test
    fun splitFlapSettled() {
        assertGrid(
            renderSplitFlap("Gate 7", 3.0f, 1L, false),
            "|GATE 7|",
        )
    }

    @Test
    fun marqueeSettled() {
        assertGrid(
            renderMarquee("Hello", 10, 3.0f, 8f),
            "|   Hello  |",
        )
    }

    @Test
    fun waveSettled() {
        assertGrid(
            renderWave("Hello wave", 3.0f, 1),
            "|H     w   |",
            "| e  o  a  |",
            "|  ll    ve|",
        )
    }

    @Test
    fun morseSettled() {
        assertGrid(
            renderMorse("SOS go", 3.0f, 6f, false, theme),
            "|... --- ... / --. ···|",
        )
    }
}
