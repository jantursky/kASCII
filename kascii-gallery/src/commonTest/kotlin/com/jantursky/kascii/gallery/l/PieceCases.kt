package com.jantursky.kascii.gallery.l

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("torus-knot", true) { torusKnotPiece(it) },
        PieceCase("icosphere", true) { icospherePiece(it) },
        PieceCase("mobius-strip", true) { mobiusStripPiece(it) },
        PieceCase("tesseract", true) { tesseractPiece(it) },
        PieceCase("dna-helix", true) { dnaHelixPiece(it) },
        PieceCase("spirograph", true) { spirographPiece(it) },
    )
