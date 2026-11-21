package com.jantursky.kascii.gallery.j

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("whale-song", true) { whaleSongPiece(it) },
        PieceCase("fox-snow", true) { foxSnowPiece(it) },
        PieceCase("butterfly-meadow", true) { butterflyMeadowPiece(it) },
        PieceCase("dragon-castle", true) { dragonCastlePiece(it) },
        PieceCase("koi-pond", true) { koiPondPiece(it) },
        PieceCase("firefly-marsh", true) { fireflyMarshPiece(it) },
        PieceCase("sea-turtle-reef", true) { seaTurtleReefPiece(it) },
    )
