# Gallery pieces, set D

Four original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/d/`. Each file has a
`xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with
`AsciiPieceView`. `galleryPiecesD` in `CatalogD.kt` lists them for the catalog. The art and code are original;
the motifs are common ones, drawn from scratch.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Aurora cabin | scenes | original | done | JVM text grid only |
| Rainy city | scenes | original | done | JVM text grid only |
| Cherry blossom | scenes | original | done | JVM text grid only |
| Rocket launch | scenes | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure (`PiecesDTest`) and golden frames (`GoldenPiecesDTest`, files in `srd/jvmTest/resources/golden/d`).
- Every piece composes through `AsciiPieceView` under reduced motion (`GalleryComposeTest`).

## Not verified

- Colors on real light and dark pages, and frame times on any device.
