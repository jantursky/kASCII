# Gallery pieces, set C

Four original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/c/`. Each file has a
`xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with
`AsciiPieceView`. `galleryPiecesC` in `CatalogC.kt` lists them for the catalog. The art and code are original;
the motifs are common ones, drawn from scratch.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Lighthouse | scenes | original | done | JVM text grid only |
| Steam train | scenes | original | done | JVM text grid only |
| Sailboat sunset | scenes | original | done | JVM text grid only |
| Desert caravan | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure (`PiecesCTest`) and golden frames (`GoldenPiecesCTest`, files in `src/jvmTest/resources/golden/c`).
- Every piece composes through `AsciiPieceView` under reduced motion (`GalleryComposeTest`).

## Not verified

- Colors on real light and dark pages, and frame times on any device.
- The desert caravan's first moments show only dunes, because the caravan walks in from off screen.
