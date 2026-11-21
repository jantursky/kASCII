package com.jantursky.kascii.gallery.k

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("lightning-storm", true) { lightningStormPiece(it) },
        PieceCase("fireworks-harbor", true) { fireworksHarborPiece(it) },
        PieceCase("galaxy-spiral", true) { galaxySpiralPiece(it) },
        PieceCase("aurora-ribbons", true) { auroraRibbonsPiece(it) },
        PieceCase("liquid-metal", true) { liquidMetalPiece(it) },
        PieceCase("topographic-contours", true) { topographicContoursPiece(it) },
    )
