# Gallery pieces, set K

Six original shader-like animated visuals in `src/commonMain/kotlin/com/jantursky/kascii/gallery/k/`, all 192 by 48
cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an `AsciiXxx(...)`
composable that plays it with `AsciiPieceView`. `galleryPiecesK` in `CatalogK.kt` lists them for the catalog, and
`KKit.kt` holds the shared canvas, noise and palette helpers (a copy of set G's kit with `K` prefixes). The art and
code are original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Lightning storm | effects | original | done | JVM text grid and SVG preview |
| Fireworks harbor | effects | original | done | JVM text grid and SVG preview |
| Galaxy spiral | space | original | done | JVM text grid and SVG preview |
| Aurora ribbons | effects | original | done | JVM text grid and SVG preview |
| Liquid metal | generative | original | done | JVM text grid and SVG preview |
| Topographic contours | generative | original | done | JVM text grid and SVG preview |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure, motion across the loop and the allowed glyph set (`PiecesKTest`) and golden frames
  (`GoldenPiecesKTest`, files in `src/jvmTest/resources/golden/k`).
- Common code compiles for WasmJs and iOS simulator.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light pages, and frame times on any device.
