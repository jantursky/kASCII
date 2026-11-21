package com.jantursky.kascii.gallery.n

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("pirate-ship-storm", true) { pirateShipStormPiece(it) },
        PieceCase("space-elevator", true) { spaceElevatorPiece(it) },
        PieceCase("underwater-city", true) { underwaterCityPiece(it) },
        PieceCase("treehouse-dusk", true) { treehouseDuskPiece(it) },
        PieceCase("wizard-tower", true) { wizardTowerPiece(it) },
        PieceCase("market-street", true) { marketStreetPiece(it) },
    )
