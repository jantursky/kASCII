package com.jantursky.kascii.gallery.i

import com.jantursky.kascii.gallery.GalleryEntry

/** The animated scenes of built places and space in this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesI: List<GalleryEntry> =
    listOf(
        GalleryEntry("space-station", "Space station", "scenes", null) { spaceStationPiece(it) },
        GalleryEntry("moon-base", "Moon base", "scenes", null) { moonBasePiece(it) },
        GalleryEntry("observatory", "Observatory", "scenes", null) { observatoryPiece(it) },
        GalleryEntry("desert-highway", "Desert highway", "scenes", null) { desertHighwayPiece(it) },
        GalleryEntry("neon-arcade", "Neon arcade", "scenes", null) { neonArcadePiece(it) },
        GalleryEntry("clock-tower", "Clock tower", "scenes", null) { clockTowerPiece(it) },
        GalleryEntry("airship-sky", "Airship sky", "scenes", null) { airshipSkyPiece(it) },
    )
