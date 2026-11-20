import { readFileSync } from 'node:fs'

import { defineConfig } from 'blume'

const properties = readFileSync(new URL('../../gradle.properties', import.meta.url), 'utf8')
const version = /^VERSION_NAME=(.+)$/m.exec(properties)?.[1]?.trim()
if (!version) throw new Error('VERSION_NAME is missing from gradle.properties')

export default defineConfig({
  title: 'Kascii',
  description: 'ASCII and text-mode art for Compose Multiplatform: a glyph engine, modifiers, components and an animated gallery.',
  logo: '/logo.svg',
  github: {
    owner: 'jantursky',
    repo: 'kascii',
    dir: 'apps/docs',
  },
  navigation: {
    actions: [
      { href: 'https://kascii-demo.vercel.app', label: 'Demo' },
      { href: 'https://central.sonatype.com/artifact/io.github.jantursky/kascii-compose', label: 'Maven Central' },
    ],
  },
  content: {
    root: 'docs',
  },
  theme: {
    accent: '#63b3ed',
    radius: 'lg',
  },
  agents: {
    // The repository's skill (skills/kascii/SKILL.md) replaces the one Blume would generate for the site.
    skills: '../../skills',
  },
  deployment: {
    site: 'https://kascii-docs.vercel.app',
  },
  variables: {
    version,
    demo: 'https://kascii-demo.vercel.app',
  },
})
