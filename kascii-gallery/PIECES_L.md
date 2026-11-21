# Gallery pieces, set L

Six original geometric studies in `src/commonMain/kotlin/com/jantursky/kascii/gallery/l/`, all 192 by 48 cells with a
dark ground, each a lit and shaded 3D or 2D form rather than a scene. Each file has a `xxxPiece(seed)` factory
returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with `AsciiPieceView`. `galleryPiecesL` in
`CatalogL.kt` lists them for the catalog, and `LKit.kt` holds the shared canvas, noise and palette helpers (a copy of
set K's kit with `L` prefixes, plus a depth buffer, a cell lookup in width units and a line-glyph picker). The art and
code are original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Torus knot | shapes | original | done | JVM text grid and SVG preview |
| Icosphere | shapes | original | done | JVM text grid and SVG preview |
| Mobius strip | shapes | original | done | JVM text grid and SVG preview |
| Tesseract | shapes | original | done | JVM text grid and SVG preview |
| DNA helix | shapes | original | done | JVM text grid and SVG preview |
| Spirograph | shapes | original | done | JVM text grid and SVG preview |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure, motion across the loop, the allowed glyph set and the `shapes` category (`PiecesLTest`) and golden frames
  (`GoldenPiecesLTest`, files in `src/jvmTest/resources/golden/l`).
- Common code compiles for WasmJs and iOS simulator.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light pages, and frame times on any device. On a desktop JVM a frame takes about 2 to 10 ms; the
  torus knot, Mobius strip and DNA helix splat 50 to 60 thousand surface samples per frame and are the heaviest.
