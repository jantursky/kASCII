# Gallery pieces, set E

Two original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/e/`. Each file has a
`xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with
`AsciiPieceView`. `galleryPiecesE` in `CatalogE.kt` lists them for the catalog. The art and code are original;
the motifs are common ones, drawn from scratch.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Orbit sunrise | scenes | original | done | JVM text grid only |
| Volcano night | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure (`PiecesETest`) and golden frames (`GoldenPiecesETest`, files in `src/jvmTest/resources/golden/e`).
- Every piece composes through `AsciiPieceView` under reduced motion (`GalleryComposeTest`).

## Not verified

- Colors on real light and dark pages, and frame times on any device.
