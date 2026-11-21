# Gallery pieces, set O

Six original animated scenes in `src/commonMain/kotlin/com/jantursky/kascii/gallery/o/`, each centred on one well-lit
object or phenomenon, all 192 by 48 cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an
`AsciiPiece` and an `AsciiXxx(...)` composable that plays it with `AsciiPieceView`. `galleryPiecesO` in `CatalogO.kt`
lists them for the catalog, and `OKit.kt` holds the shared canvas, noise and palette helpers (a copy of set K's kit
with `O` prefixes, plus a `tone` helper that turns a light value into halftone dots or shaded blocks). The art and
code are original.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Gear train | objects | original | done | JVM text grid and SVG preview |
| Candle flame | nature | original | done | JVM text grid and SVG preview |
| Snow globe | objects | original | done | JVM text grid and SVG preview |
| Lantern festival | nature | original | done | JVM text grid and SVG preview |
| Vinyl record | objects | original | done | JVM text grid and SVG preview |
| Ocean waves | effects | original | done | JVM text grid and SVG preview |

## Notes

- Gear train: every gear advances 36 teeth per 18 s loop, a multiple of each gear's teeth per spoke, so the loop
  closes after the common period; mesh phases are solved so a tooth always meets a gap.
- Snow globe: each flake's resting place is where its own fall ends, so the shake at the start of the loop lifts it
  from exactly where the previous loop left it.
- Vinyl record: 10 turns per 18 s loop is 33 1/3 rpm.

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure, motion across the loop and the allowed glyph set (`PiecesOTest`) and golden frames
  (`GoldenPiecesOTest`, files in `src/jvmTest/resources/golden/o`).
- Common code compiles for WasmJs and iOS simulator.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light pages, and frame times on any device.
