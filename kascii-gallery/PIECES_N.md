# Gallery pieces, set N

Six original animated story scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/n/`, all 192 by 48 cells
with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an `AsciiXxx(...)`
composable that plays it with `AsciiPieceView`. `galleryPiecesN` in `CatalogN.kt` lists them for the catalog, and
`NKit.kt` holds the shared canvas, noise, palette, glow and star helpers (a copy of set I's kit with an `N` prefix,
plus a rope-sag helper). The art and code are original.

| Piece | Category | Loop | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- | --- |
| Pirate ship storm | scenes | 12 s | original | done | JVM text grid only |
| Space elevator | scenes | 20 s | original | done | JVM text grid only |
| Underwater city | scenes | 16 s | original | done | JVM text grid only |
| Treehouse dusk | scenes | 12 s | original | done | JVM text grid only |
| Wizard tower | scenes | 16 s | original | done | JVM text grid only |
| Market street | scenes | 12 s | original | done | JVM text grid only |

Notes:

- Pirate ship storm draws the galleon cell by cell in its own rotated frame, posed on the swell under it, so it pitches
  with the waves; water bands in front of it hide the hull below the waterline. Lightning strikes twice per loop.
- Space elevator is seen from orbit: the planet is a sphere lit from behind the limb, so most of it is night side, and
  the climber grows as it rises toward the camera. It reaches the top edge exactly at the end of the loop.
- Clouds and storm skies churn by moving the noise domain round a small circle once per loop, which closes the loop
  without a visible scroll seam.
- Market street is a one-point perspective: facades, cobbles, stalls, lantern strings and people all map world
  coordinates through the same depth, and depth-sorted things are drawn back to front.

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure and the allowed glyph set (`PiecesNTest`) and golden frames (`GoldenPiecesNTest`, files in
  `src/jvmTest/resources/golden/n`).
- Frames looked at as rasterised exports during development; a frame renders in roughly 1 to 11 ms on a laptop JVM.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light and dark pages, and frame times on any device.
