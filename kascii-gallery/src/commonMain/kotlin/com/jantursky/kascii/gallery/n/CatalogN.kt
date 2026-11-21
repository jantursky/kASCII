package com.jantursky.kascii.gallery.n

import com.jantursky.kascii.gallery.GalleryEntry

/** The animated story scenes of this set, all originals, one entry per piece, each built for a seed. */
public val galleryPiecesN: List<GalleryEntry> =
    listOf(
        GalleryEntry("pirate-ship-storm", "Pirate ship storm", "scenes", null) { pirateShipStormPiece(it) },
        GalleryEntry("space-elevator", "Space elevator", "scenes", null) { spaceElevatorPiece(it) },
        GalleryEntry("underwater-city", "Underwater city", "scenes", null) { underwaterCityPiece(it) },
        GalleryEntry("treehouse-dusk", "Treehouse dusk", "scenes", null) { treehouseDuskPiece(it) },
        GalleryEntry("wizard-tower", "Wizard tower", "scenes", null) { wizardTowerPiece(it) },
        GalleryEntry("market-street", "Market street", "scenes", null) { marketStreetPiece(it) },
    )
