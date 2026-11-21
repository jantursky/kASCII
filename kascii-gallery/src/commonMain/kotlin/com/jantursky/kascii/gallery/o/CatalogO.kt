package com.jantursky.kascii.gallery.o

import com.jantursky.kascii.gallery.GalleryEntry

/** The lit-object and phenomenon pieces of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesO: List<GalleryEntry> =
    listOf(
        GalleryEntry("gear-train", "Gear train", "objects", null) { gearTrainPiece(it) },
        GalleryEntry("candle-flame", "Candle flame", "nature", null) { candleFlamePiece(it) },
        GalleryEntry("snow-globe", "Snow globe", "objects", null) { snowGlobePiece(it) },
        GalleryEntry("lantern-festival", "Lantern festival", "nature", null) { lanternFestivalPiece(it) },
        GalleryEntry("vinyl-record", "Vinyl record", "objects", null) { vinylRecordPiece(it) },
        GalleryEntry("ocean-waves", "Ocean waves", "effects", null) { oceanWavesPiece(it) },
    )
