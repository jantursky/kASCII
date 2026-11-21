package com.jantursky.kascii.gallery.k

import com.jantursky.kascii.gallery.GalleryEntry

/** The shader-like animated visuals of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesK: List<GalleryEntry> =
    listOf(
        GalleryEntry("lightning-storm", "Lightning storm", "effects", null) { lightningStormPiece(it) },
        GalleryEntry("fireworks-harbor", "Fireworks harbor", "effects", null) { fireworksHarborPiece(it) },
        GalleryEntry("galaxy-spiral", "Galaxy spiral", "space", null) { galaxySpiralPiece(it) },
        GalleryEntry("aurora-ribbons", "Aurora ribbons", "effects", null) { auroraRibbonsPiece(it) },
        GalleryEntry("liquid-metal", "Liquid metal", "generative", null) { liquidMetalPiece(it) },
        GalleryEntry(
            "topographic-contours",
            "Topographic contours",
            "generative",
            null,
        ) { topographicContoursPiece(it) },
    )
