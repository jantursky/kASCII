package com.jantursky.kascii.gallery.h

import com.jantursky.kascii.gallery.GalleryEntry

/** The animated landscape scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesH: List<GalleryEntry> =
    listOf(
        GalleryEntry("mountain-lake", "Mountain lake", "scenes", null) { mountainLakePiece(it) },
        GalleryEntry("windmill-meadow", "Windmill meadow", "scenes", null) { windmillMeadowPiece(it) },
        GalleryEntry("jungle-temple", "Jungle temple", "scenes", null) { jungleTemplePiece(it) },
        GalleryEntry("ice-cave", "Ice cave", "scenes", null) { iceCavePiece(it) },
        GalleryEntry("floating-islands", "Floating islands", "scenes", null) { floatingIslandsPiece(it) },
        GalleryEntry("canyon-river", "Canyon river", "scenes", null) { canyonRiverPiece(it) },
        GalleryEntry("snow-village", "Snow village", "scenes", null) { snowVillagePiece(it) },
    )
