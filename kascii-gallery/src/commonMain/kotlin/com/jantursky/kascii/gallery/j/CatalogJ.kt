package com.jantursky.kascii.gallery.j

import com.jantursky.kascii.gallery.GalleryEntry

/** The animated creature scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesJ: List<GalleryEntry> =
    listOf(
        GalleryEntry("whale-song", "Whale song", "creatures", null) { whaleSongPiece(it) },
        GalleryEntry("fox-snow", "Fox in snow", "creatures", null) { foxSnowPiece(it) },
        GalleryEntry("butterfly-meadow", "Butterfly meadow", "creatures", null) { butterflyMeadowPiece(it) },
        GalleryEntry("dragon-castle", "Dragon over castle", "creatures", null) { dragonCastlePiece(it) },
        GalleryEntry("koi-pond", "Koi pond", "creatures", null) { koiPondPiece(it) },
        GalleryEntry("firefly-marsh", "Firefly marsh", "creatures", null) { fireflyMarshPiece(it) },
        GalleryEntry("sea-turtle-reef", "Sea turtle reef", "creatures", null) { seaTurtleReefPiece(it) },
    )
