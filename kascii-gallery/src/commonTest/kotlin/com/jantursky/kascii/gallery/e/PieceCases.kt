package com.jantursky.kascii.gallery.e

import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.gallery.orbitSunrisePiece
import com.jantursky.kascii.gallery.volcanoNightPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("orbit-sunrise", true) { orbitSunrisePiece(it) },
        PieceCase("volcano-night", true) { volcanoNightPiece(it) },
    )
