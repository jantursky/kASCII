# Kascii design system

Mood: a deep-ocean terminal. Pressurized darkness where text-mode art generates itself,
and controls look like faintly glowing specimens. Monospace is the identity.

Dark is the native theme. The light theme is derived (see "Light mode") and is not part of
the original system.

## Palette (dark)

| Token | Value | Role |
| --- | --- | --- |
| `--void` | `#06051d` | Page background. Never pure black. |
| `--void-end` | `#061434` | Gradient terminus: `linear-gradient(0deg, #06051d 30%, #061434)` |
| `--surface-1` | `#0f1c36` | Secondary surface, button container |
| `--surface-2` | `#1d293d` | Cards, nav, interactive containers |
| `--surface-3` | `#314062` | Elevated card, hover |
| `--text` | `#cad5e2` | Body and UI text |
| `--text-strong` | `#ffffff` | Headings |
| `--text-bright` | `#ebf8ff` | High-brightness text on dark surfaces |
| `--border` | `#e5e7eb` | Used at 10-15% opacity only |
| `--link` | `#63b3ed` | Inline links and flat nav links only |
| `--green` | `#00bc7d` | Success, primary action text, icon strokes |
| `--green-fill` | `#004f3b` | Green tint, used at 20% opacity |
| `--amber` | `#f0b100` | Section icons, accents |
| `--amber-fill` | `#733e0a` | Amber tint, used at 20% opacity |
| `--red` | `#ff2056` | Errors |
| `--red-fill` | `#8b0836` | Red tint, used at 20% opacity |

Secondary text is `--text` at 65% opacity. The source system lists `#2e3038` for this role, which
has too little contrast on these surfaces, so it is not used.

Elevation comes from surface steps (void, 1, 2, 3), not shadows.

## Typography

- Primary: JetBrains Mono, weight 400 for everything, including headings. Bundled (SIL OFL 1.1).
- Prose in the docs: DM Sans 400/500 (SIL OFL 1.1), body paragraphs only. Never headings.
- Scale (minor second from 16): caption 14 / 1.63, body 16 / 1.5, heading 30 / 1.25.
- No bold above weight 500. No uppercase, no letter-spacing changes.

## Spacing and shape

- 8 px base. Scale: 8, 16, 24, 32, 48, 64.
- Section gap 64. Card padding 32. Element gap 8-16. Content max width 800, prose 640.
- Radius: 8 for cards, images, inputs. 9999 for pills. Nothing in between.
- Borders: white at 10%. Image tiles: `--border` at 15%.
- Shadow only on pills: `0 4px 6px -1px rgb(0 0 0 / .1), 0 2px 4px -2px rgb(0 0 0 / .1)`.

## Components

- Pill button: 20% tint of the accent fill, accent text, 10% white border, padding 8 x 20, 14 px.
  Three tints only: green (primary), amber (discover), red (destructive or secondary).
  No opaque solid buttons.
- Flat link: transparent, `--link`, 16 px, no underline.
- Section heading: 30 px weight 400, `--text-strong`, with a 16 px amber icon and 8 px gap.
- Nav: full width, `--surface-2`, 8 px vertical padding, 48 px horizontal.
- Hero: full viewport, live ASCII piece at low opacity (about 5-10%) in `--text` over `--void`,
  title centered at 36-40 px.
- Cards: `--surface-2`, 8 px radius, 32 px padding, no shadow.
- Prose: directly on `--void`, no colored backgrounds.

## Light mode (derived)

| Token | Value |
| --- | --- |
| `--void` | `#f4f6fb` |
| `--surface-1` | `#e8edf6` |
| `--surface-2` | `#dbe3f0` |
| `--surface-3` | `#c9d3e6` |
| `--text` | `#1d293d` |
| `--text-strong` | `#06051d` |
| `--link` | `#1d6fb8` |
| `--green` | `#00785a` |
| `--amber` | `#9a6a00` |
| `--red` | `#c2143f` |
| `--border` | `#1d293d` at 12% |

Same shapes, spacing and type. Accent tints stay at 20% of their fill color. Text must keep
a 4.5:1 contrast ratio against its surface in both modes.

## Rules

Do:
- Use monospace everywhere in UI chrome.
- Keep accents to the three tints plus link blue.
- Keep a 64 px gap between sections.

Don't:
- Put a sans-serif or serif face on headings.
- Add a fourth accent tint or solid opaque buttons.
- Add photography or decorative imagery. The only imagery is live ASCII.
- Use pure black, or weight above 500.

## Logo

Colors come from the Kotlin ecosystem, not from this system: `--logo-a: #7f52ff`,
`--logo-b: #e44857`. Dark mode uses the same pair (both read on the void). The shape is chosen
with the author and drawn as plain SVG paths.
