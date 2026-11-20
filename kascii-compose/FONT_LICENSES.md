# Font licenses

Kascii Compose bundles one font, `src/commonMain/composeResources/font/kascii_mono.ttf`, named Kascii Mono.
It is a Modified Version, in the sense of the SIL Open Font License 1.1, of two fonts that are both
licensed under that license:

| Source | Version | Copyright | License text |
| --- | --- | --- | --- |
| [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) Regular | 2.304 | 2020 The JetBrains Mono Project Authors | `files/licenses/OFL-JetBrainsMono.txt` |
| [GNU Unifont](https://unifoundry.com/unifont/), OFL 1.1 option | 18.0.01 | 1998-2026 Roman Czyborra, Paul Hardy et al. | `files/licenses/OFL-Unifont.txt` |

Both license texts ship inside the library under `composeResources/files/licenses/`. Kascii Mono is
distributed under the SIL Open Font License 1.1 as well. Neither source declares a Reserved Font Name;
the merged font still uses a name of its own.

## What was changed

- JetBrains Mono Regular keeps all of its glyphs. Its layout features are removed, so programming
  ligatures such as `->` never join glyphs in an ASCII grid.
- A subset of Unifont adds what JetBrains Mono lacks for the built-in charsets: Braille Patterns
  (U+2800-28FF), Katakana (U+30A0-30FF), Halfwidth Katakana (U+FF65-FF9F), U+2981 and the two CJK
  characters U+529B and U+535C. Glyphs JetBrains Mono already has are not taken from Unifont.
- Every added glyph is scaled to one 600-unit cell and to the full 1.32 em line of JetBrains Mono, so
  the font is strictly monospaced and Braille dots and blocks tile. Full-width glyphs are narrowed to
  one cell.

## How it was built

With fontTools 4.66:

```sh
pyftsubset JetBrainsMono-Regular.ttf --unicodes='*' --layout-features='' \
  --name-IDs='*' --name-languages='*' --glyph-names --output-file=jbm.ttf
pyftsubset unifont-18.0.01.otf \
  --unicodes='U+2800-28FF,U+30A0-30FF,U+FF65-FF9F,U+2981,U+529B,U+535C' \
  --layout-features='' --glyph-names --output-file=unifont_subset.otf
```

Then `python3 fonts/merge_fonts.py jbm.ttf unifont_subset.otf kascii_mono.ttf` copies each Unifont
glyph that JetBrains Mono lacks into the TrueType outlines, scaling it as described above, maps it in
`cmap`, and renames the family to Kascii Mono.
