package com.jantursky.kascii.gallery.h

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("mountain-lake", true) { mountainLakePiece(it) },
        PieceCase("windmill-meadow", true) { windmillMeadowPiece(it) },
        PieceCase("jungle-temple", true) { jungleTemplePiece(it) },
        PieceCase("ice-cave", true) { iceCavePiece(it) },
        PieceCase("floating-islands", true) { floatingIslandsPiece(it) },
        PieceCase("canyon-river", true) { canyonRiverPiece(it) },
        PieceCase("snow-village", true) { snowVillagePiece(it) },
    )
