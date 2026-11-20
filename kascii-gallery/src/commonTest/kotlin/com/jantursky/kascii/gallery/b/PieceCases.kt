package com.jantursky.kascii.gallery.b

import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.gallery.analogClockPiece
import com.jantursky.kascii.gallery.crtFilterPiece
import com.jantursky.kascii.gallery.flamesPiece
import com.jantursky.kascii.gallery.gliderGunPiece
import com.jantursky.kascii.gallery.hourglassPiece
import com.jantursky.kascii.gallery.jellyfishPiece
import com.jantursky.kascii.gallery.langtonsAntPiece
import com.jantursky.kascii.gallery.lavaLampPiece
import com.jantursky.kascii.gallery.mandelbrotPiece
import com.jantursky.kascii.gallery.matrixRainPiece
import com.jantursky.kascii.gallery.owlPiece
import com.jantursky.kascii.gallery.synthwavePiece
import com.jantursky.kascii.gallery.tunnelPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

private const val TEN_PAST_TEN = 10 * 3600.0 + 8 * 60 + 35

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("jellyfish", true) { jellyfishPiece(it) },
        PieceCase("owl", true) { owlPiece(it) },
        PieceCase("analog-clock", false) { analogClockPiece(now = { TEN_PAST_TEN }) },
        PieceCase("hourglass", true) { hourglassPiece(it) },
        PieceCase("lava-lamp", true) { lavaLampPiece(it) },
        PieceCase("mandelbrot", false) { mandelbrotPiece() },
        PieceCase("glider-gun", false) { gliderGunPiece() },
        PieceCase("langtons-ant", true) { langtonsAntPiece(it) },
        PieceCase("flames", true) { flamesPiece(it) },
        PieceCase("matrix-rain", true) { matrixRainPiece(it) },
        PieceCase("synthwave", true) { synthwavePiece(it) },
        PieceCase("tunnel", true) { tunnelPiece(it) },
        PieceCase("crt-filter", true) { crtFilterPiece(it) },
    )
