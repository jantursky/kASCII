package com.jantursky.kascii.gallery.f

import com.jantursky.kascii.core.AsciiPiece
import com.jantursky.kascii.gallery.abyssLurePiece
import com.jantursky.kascii.gallery.witchMoonPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("abyss-lure", true) { abyssLurePiece(it) },
        PieceCase("witch-moon", true) { witchMoonPiece(it) },
    )
