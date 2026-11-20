package com.jantursky.kascii.gallery.g

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("waking-giant", true) { wakingGiantPiece(it) },
        PieceCase("moon-angler", true) { moonAnglerPiece(it) },
    )
