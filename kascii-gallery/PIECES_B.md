# Gallery pieces, set B

Thirteen pieces in `src/commonMain/kotlin/com/jantursky/kascii/gallery/b/`. Each file has a
`xxxPiece(...)` factory returning an `AsciiPiece` and an `AsciiXxx(...)` composable that plays it with
`AsciiPieceView`. `galleryPiecesB` in `CatalogB.kt` lists them for the catalog.

"Ported from" means the idea and the structure were re-implemented in Kotlin from the MIT-licensed
[ascii.rest source](https://github.com/bas3line/ascii) (Copyright (c) 2026 bas3line); no code was copied and
nothing from its Logos, Companies or Distros groups was used.

| Piece | Category | Ported from | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Jellyfish | creatures | ascii.rest `jellyfish` | done | JVM text grid only |
| Owl | creatures | original | done | JVM text grid only |
| Analog clock | objects | original (drawn after ascii.rest `analog clock`) | done, UTC by default | JVM text grid only |
| Hourglass | objects | ascii.rest `hourglass` | done | JVM text grid only |
| Lava lamp | objects | ascii.rest `lava lamp` | done | JVM text grid only |
| Mandelbrot set | generative | ascii.rest `mandelbrot` | done | JVM text grid only |
| Glider gun | generative | ascii.rest `glider gun` | done | JVM text grid only |
| Langton's ant | generative | ascii.rest `langtons ant` | done | JVM text grid only |
| Doom fire | effects | ascii.rest `doom fire` | done | JVM text grid only |
| Matrix rain | effects | ascii.rest `matrix rain` | done | JVM text grid only |
| Synthwave | effects | ascii.rest `synthwave` | done | JVM text grid only |
| Tunnel | effects | ascii.rest `tunnel` | done | JVM text grid only |
| CRT filter | media | original | done | JVM text grid only |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure (`PiecesBTest`) and golden frames (`GoldenPiecesBTest`, files in `src/jvmTest/resources/golden/b`).
  Regenerate the goldens with `KASCII_UPDATE_GOLDEN=1`.

## Unverified

- Everything drawn: the composables were compiled for the JVM target only and never rendered, because
  `AsciiPieceView` had no implementation yet.
- Android, iOS and wasmJs: not built or run.
- Per-cell colors and how they read on the light theme.
- Frame time of the heavier pieces (Mandelbrot, tunnel, CRT filter) in a real renderer.

## Notes

- Stateful pieces (Langton's ant, doom fire) step a cached state by frame index and replay from the start when
  asked for an earlier frame. The glider gun precomputes one 30 generation period and looks frames up. Matrix
  rain is a pure function of time.
- The analog clock samples `now` (seconds since midnight) once when the piece is built and then counts with the
  piece's time. The default reads the system clock in UTC, since common code has no time zone.
