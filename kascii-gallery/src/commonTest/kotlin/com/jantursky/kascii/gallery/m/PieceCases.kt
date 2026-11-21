package com.jantursky.kascii.gallery.m

import com.jantursky.kascii.core.AsciiPiece

/** A piece under test: its file-safe [name], whether a seed changes it, and a builder for a seed. */
internal class PieceCase(
    val name: String,
    val seeded: Boolean,
    val create: (seed: Int) -> AsciiPiece,
)

internal val pieceCases: List<PieceCase> =
    listOf(
        PieceCase("reaction-diffusion", true) { reactionDiffusionPiece(it) },
        PieceCase("boids-flock", true) { boidsFlockPiece(it) },
        PieceCase("falling-sand", true) { fallingSandPiece(it) },
        PieceCase("voronoi-cells", true) { voronoiCellsPiece(it) },
        PieceCase("harmonograph", true) { harmonographPiece(it) },
        PieceCase("double-pendulum", true) { doublePendulumPiece(it) },
    )
