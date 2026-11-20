---
name: kascii
description: Build ASCII and text-mode art in Compose Multiplatform with kASCII. Use when the user wants glyph-based visuals in a Kotlin Multiplatform app (Android, iOS, desktop, web): a procedural background, an image or any composable turned into ASCII, a terminal-style UI, an animated scene, a text effect, a chart in text, or a new animated piece built with the kit. Covers adding the dependency, choosing a charset and color mode, writing a field or a piece, reduced motion, accessibility, export, and tests.
license: MIT
---

# kASCII

kASCII draws text-mode art in Compose Multiplatform. A **field** gives a tone from 0 to 1 for every position and
moment, a **charset** turns tones into glyphs, a **color mode** colors them, and a **renderer** draws the grid.
Everything is deterministic for a given seed, pauses offscreen (canvas, modifier and gallery views) and holds a still frame under reduced motion.

Build what the user asked for. Do not drop in a nearby gallery piece and call it done. Draw glyphs, never pixels.

## 1. Set up

Add the modules you need (Maven Central, group `io.github.jantursky`):

```kotlin
commonMain.dependencies {
    implementation("io.github.jantursky:kascii-compose:<version>")    // AsciiCanvas, Modifier.ascii, renderers
    implementation("io.github.jantursky:kascii-components:<version>") // buttons, charts, terminal, text effects
    implementation("io.github.jantursky:kascii-gallery:<version>")    // animated scenes and shapes
}
```

`kascii-core` (fields, charsets, grid, export, the piece kit) comes in through the others and has no UI dependency.

Done when: the project builds and `AsciiCanvas(Fields.plasma(), Modifier.fillMaxSize())` draws on the target the
user cares about.

## 2. Pick the technique

| The user wants | Use | Notes |
| --- | --- | --- |
| An animated background or effect | `AsciiCanvas(field = ...)` | Start from `Fields.plasma`, `noise`, `waves`, `ripples`. |
| Any composable as ASCII | `Modifier.ascii(charset, cell)` | The content is drawn, sampled, then drawn as glyphs. |
| A picture as ASCII | `AsciiImage(bitmap, ...)` | Tones and per-cell colors from the bitmap. |
| A finished animated scene | A gallery composable such as `Donut()` or `Starfield()` | Each has `speed` and `motion`; most also take `seed`. |
| Terminal-style UI | `AsciiButton`, `AsciiCard`, `AsciiTabs`, `AsciiTerminal`, ... | Pure text frames with real semantics. |
| A chart in text | `AsciiBarChart`, `AsciiSparkline`, `AsciiPieChart`, ... | Braille gives 2x4 dots per cell. |
| Big or animated text | `AsciiBigText`, typewriter, scramble, glitch, marquee, ... | Reduced motion shows the final text. |
| Something new | `asciiPiece(...)` and `AsciiPieceView` | See step 3. |
| Output to a file or clipboard | `Export.toText`, `toAnsi`, `toSvg`, `Grid.toImageBitmap` | Strings for `toText`, `toAnsi`, `toSvg` (golden-testable); an `ImageBitmap` for `toImageBitmap`. |

## 3. Build it

### A field

```kotlin
val field = Field { x, y, t, aspect ->
    // x, y in 0..1 across the grid, t in seconds, aspect = grid width / height in pixels
    val d = hypot((x - 0.5f) * aspect, y - 0.5f)
    (sin(d * 20f - t * 2f) * 0.5f + 0.5f)
}
AsciiCanvas(field, Modifier.fillMaxSize(), charset = Charsets.Blocks, cell = 8.dp)
```

Return tones in 0..1. Use `aspect` to keep circles round. Use `Noise.fbm(x, y, t, octaves, seed)` for smooth
randomness and `Rng(seed)` for anything that needs random numbers, so the same seed draws the same frames.

### A charset

`Charsets.Standard` (`" .:-=+*#%@"`), `Dense`, `Short`, `Blocks`, `Quadrants` (2x2), `Braille` (2x4), `BoxDrawing`,
`Cp437`, `Petscii`, `Katakana`, `Dots`, `Binary`, `Digits`, `Edge` (slashes and bars along edges), or
`Charsets.custom(" .oO@")`, ordered light to dark. Braille and Quadrants lift the resolution without a smaller cell.

### A color mode

`ColorMode.Theme` (default, follows light and dark), `Mono(color)`, `Duotone(dark, light)`, `Gradient(stops)`,
`Palette` (theme palette by tone), `Source(colorField)` (a color per cell). Colors come from `KasciiTheme`; build one
from Material with `kasciiThemeOf(ink, paper, accent, palette)` and provide it with `ProvideKasciiTheme`.

### A piece with the kit

```kotlin
val orbit = asciiPiece(name = "orbit", category = "space", cols = 64, rows = 24, still = 2f, loop = 10f) { t, grid ->
    val rng = Rng(7)
    repeat(40) { grid.put(rng.int(grid.cols), rng.int(grid.rows), '.') }
    grid.disc(32f, 12f, 4f, '@')
    grid.disc(32f + 18f * cos(t), 12f + 6f * sin(t), 1.5f, 'o')
}
AsciiPieceView(orbit, Modifier.fillMaxWidth().aspectRatio(64f / 24f))
```

A piece is a pure function of `t` and its seed: the same `t` gives the same grid whatever order frames are asked in.
`still` is the frame shown under reduced motion, `loop` the seconds after which it repeats. Kit helpers:
`Grid.put/write/fill/line/rect/disc/label/blit`, `BoxStyle`, `BrailleCanvas`, `Rng`, `Noise`.

Done when: the piece draws, a second call at the same `t` gives identical text, and it looks right with `Charsets`
and theme swapped.

## 4. Check it

- **Test the grid, not pixels.** `piece.render(t).toText()` is platform independent. Add a golden file per piece
  and one determinism test (render twice, compare).
- **Reduced motion.** Set `motion = Motion.Reduced` (or the platform setting) and confirm the still frame is
  meaningful.
- **Offscreen.** Pieces pause when not visible. Do not add your own timers; use the frame clock the composables use.
- **Accessibility.** Give art a `contentDescription` when it carries meaning, leave it null when decorative.
  Interactive components expose role, state, keyboard and focus support and 48 dp touch targets.
- **Renderers.** `AsciiRenderer.Auto` picks GPU when available and the grid has more than 2,000 cells. Force `Text` for selectable, copyable art and `Cpu`
  as the baseline. A platform you did not run is unverified: say so.
- **Export.** `Export.toSvg` and `toAnsi` take a `Grid`; check them against a golden string.

## Rules

- Draw glyphs. A piece writes into a `Grid`; it never draws pixels.
- Tones are 0..1. Clamp inputs; never rely on a platform float quirk.
- Deterministic: all randomness through `Rng(seed)` or `Noise`.
- No third-party logos or trademarks in pieces; provide helpers for the user's own marks instead.
- Keep fonts redistributable. The bundled font is JetBrains Mono plus a Unifont subset (both OFL).
