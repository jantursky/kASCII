// Copies the changelog and the gallery status tables into the docs site before a build.
//  - CHANGELOG.md -> docs/_generated/changelog.md (without its top-level title, gitignored)
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const root = new URL('../../../', import.meta.url)
const here = new URL('../', import.meta.url)
const path = (url) => fileURLToPath(url)

const changelog = new URL('CHANGELOG.md', root)
if (!existsSync(changelog)) {
  console.error(`sync: ${path(changelog)} is missing. The docs changelog page is generated from it.`)
  process.exit(1)
}
const lines = readFileSync(changelog, 'utf8').split('\n')
const body = lines[0]?.startsWith('# ') ? lines.slice(1).join('\n').trimStart() : lines.join('\n')
mkdirSync(new URL('docs/_generated/', here), { recursive: true })
writeFileSync(new URL('docs/_generated/changelog.md', here), body)

// Collects the gallery status tables (kascii-gallery/PIECES_*.md) into docs/_generated/pieces.md.
import { readdirSync } from 'node:fs'

const gallery = new URL('kascii-gallery/', root)
const tables = existsSync(gallery)
  ? readdirSync(gallery)
      .filter((name) => /^PIECES_.+\.md$/.test(name))
      .sort()
      .map((name) => readFileSync(new URL(name, gallery), 'utf8').replace(/^# .*\n+/, '').trim())
  : []
writeFileSync(
  new URL('docs/_generated/pieces.md', here),
  tables.length > 0 ? tables.join('\n\n') : 'No pieces are listed yet.',
)
