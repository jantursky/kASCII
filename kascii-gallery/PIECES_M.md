# Gallery pieces, set M

Six original generative and simulated animated visuals in `src/commonMain/kotlin/com/jantursky/kascii/gallery/m/`, all
192 by 48 cells with a dark ground. Each file has a `xxxPiece(seed)` factory returning an `AsciiPiece` and an
`AsciiXxx(...)` composable that plays it with `AsciiPieceView`. `galleryPiecesM` in `CatalogM.kt` lists them for the
catalog, and `MKit.kt` holds the shared canvas, noise and palette helpers (a copy of set K's kit with `M` prefixes). The
art and code are original.

Every piece loops exactly even where it simulates something: the state at a time is computed from that time and the
seed alone, never accumulated frame by frame.

| Piece | Category | How it loops | Status | Platforms checked |
| --- | --- | --- | --- | --- |
| Reaction diffusion | generative | warped wave superposition cut by a drifting feed level; every phase turns a whole number of times per loop | done | JVM text grid and SVG preview |
| Boids flock | physics | each bird rides a closed path on a folding, twisting sheet; trails are past positions on the same paths | done | JVM text grid and SVG preview |
| Falling sand | physics | grains are dropped and rolled once when the piece is built and each place records when it filled; the pour, the drain crater and the falling grains are functions of time | done | JVM text grid and SVG preview |
| Voronoi cells | generative | seeds move on closed Lissajous paths | done | JVM text grid and SVG preview |
| Harmonograph | generative | two precomputed damped pendulum figures per loop, drawn, held and faded | done | JVM text grid and SVG preview |
| Double pendulum | physics | one RK4 run from rest, played forward, eased to a stop where it is nearest rest, played back; a run from rest is symmetric in time | done | JVM text grid and SVG preview |

## Verified

- `Grid.toText()` output of every piece on the JVM: determinism, size, seeds, call-order independence, loop
  closure, motion across the loop and the allowed glyph set (`PiecesMTest`) and golden frames
  (`GoldenPiecesMTest`, files in `src/jvmTest/resources/golden/m`).
- Common code compiles for WasmJs and iOS simulator.

## Not verified

- Not yet wired into the demo gallery or `GalleryComposeTest`.
- Colors on real light pages, and frame times on any device.
- The double pendulum and falling sand simulations are precomputed in floating point; frames on other platforms may
  differ in the last digits from the JVM goldens, though each platform is deterministic on its own.
