package com.jantursky.kascii.gallery.m

import com.jantursky.kascii.gallery.GalleryEntry

/**
 * The generative and simulated animated visuals of this set, all originals, one entry per piece, each built for a seed.
 */
public val galleryPiecesM: List<GalleryEntry> =
    listOf(
        GalleryEntry("reaction-diffusion", "Reaction diffusion", "generative", null) { reactionDiffusionPiece(it) },
        GalleryEntry("boids-flock", "Boids flock", "physics", null) { boidsFlockPiece(it) },
        GalleryEntry("falling-sand", "Falling sand", "physics", null) { fallingSandPiece(it) },
        GalleryEntry("voronoi-cells", "Voronoi cells", "generative", null) { voronoiCellsPiece(it) },
        GalleryEntry("harmonograph", "Harmonograph", "generative", null) { harmonographPiece(it) },
        GalleryEntry("double-pendulum", "Double pendulum", "physics", null) { doublePendulumPiece(it) },
    )
