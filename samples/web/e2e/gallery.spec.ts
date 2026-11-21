import { readFileSync } from 'node:fs'

import { expect, test, type Page } from '@playwright/test'
import sharp from 'sharp'

// Skiko warns about APIs it uses itself, and WebKit reports a WebGL extension query as an error. Nothing else may
// reach the console as an error.
const KNOWN_ERRORS = [
  /Accessing `memory` via `wasmExports` is deprecated/,
  /WEBGL_debug_renderer_info not enabled/,
]

const collectErrors = (page: Page) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error' && !KNOWN_ERRORS.some((known) => known.test(message.text()))) {
      errors.push(message.text())
    }
  })
  return errors
}

const read = (path: string) => readFileSync(new URL(`../../../${path}`, import.meta.url), 'utf8')

// Every page id of the gallery, read from the catalog and the piece registries so a new page or piece is covered
// without touching this file.
const pageIds = (file: string) =>
  [...read(`samples/gallery/src/commonMain/kotlin/com/jantursky/kascii/samples/gallery/${file}`).matchAll(/Page\(\s*"([a-z-]+)"/g)].map(
    (match) => match[1],
  )
const SHOWCASE = pageIds('Catalog.kt')
const COMPONENTS = pageIds('ComponentPages.kt')
const PIECES = ['a/CatalogA.kt', 'b/CatalogB.kt'].flatMap((file) =>
  [...read(`kascii-gallery/src/commonMain/kotlin/com/jantursky/kascii/gallery/${file}`).matchAll(/GalleryEntry\("([a-z-]+)"/g)].map(
    (match) => match[1],
  ),
)
const PAGES = [...SHOWCASE, ...COMPONENTS, ...PIECES]

// The gallery is one Compose canvas, so the page is ready when it has been created and has a size.
const ready = async (page: Page) => {
  await expect(page.locator('canvas')).toHaveCount(1, { timeout: 30_000 })
  await expect
    .poll(async () => (await page.locator('canvas').evaluate((c: HTMLCanvasElement) => c.width)) > 0, { timeout: 30_000 })
    .toBe(true)
}

// How much the pixels vary: the largest standard deviation of a channel. A blank page has none.
const variation = async (png: Buffer) => Math.max(...(await sharp(png).stats()).channels.map((channel) => channel.stdev))

// How far two screenshots are apart: the mean difference per channel, 0 to 255.
const difference = async (a: Buffer, b: Buffer) => {
  const [x, y] = await Promise.all([a, b].map((png) => sharp(png).raw().toBuffer()))
  let sum = 0
  for (let i = 0; i < x.length; i++) sum += Math.abs(x[i] - y[i])
  return sum / x.length
}

test('the catalog has pages to test', () => {
  expect(SHOWCASE).toEqual(['overview', 'pieces', 'fields', 'renderers', 'modifier', 'theme', 'terminal-app'])
  expect(COMPONENTS).toEqual(['forms', 'surfaces', 'navigation', 'terminal', 'progress', 'charts', 'meters', 'text-effects'])
  expect(PIECES).toHaveLength(26)
})

test('the gallery loads without console errors', async ({ page }) => {
  const errors = collectErrors(page)
  await page.goto('/')
  await expect(page).toHaveTitle('kASCII gallery')
  await ready(page)
  expect(errors).toEqual([])
})

for (const id of PAGES) {
  test(`${id} opens from a deep link, draws something and keeps its address`, async ({ page }) => {
    const errors = collectErrors(page)
    await page.goto(`/#${id}`)
    await ready(page)
    await page.waitForTimeout(800)
    expect(await variation(await page.screenshot())).toBeGreaterThan(5)
    await expect(page).toHaveURL(new RegExp(`#${id}$`))
    expect(errors).toEqual([])
  })
}

test('an unknown address opens the overview', async ({ page }) => {
  await page.goto('/#no-such-page')
  await ready(page)
  await expect(page).toHaveURL(/#overview$/)
})

for (const path of ['/', '/#pieces', '/#fields', '/#campfire', ...COMPONENTS.map((id) => `/#${id}`), '/#terminal-app']) {
  test(`the page never scrolls sideways (${path})`, async ({ page }) => {
    await page.goto(path)
    await ready(page)
    await page.waitForTimeout(500)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow).toBeLessThanOrEqual(0)
  })
}

test('the grid of pieces is alive', async ({ page }) => {
  await page.goto('/#pieces')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  expect(await variation(first)).toBeGreaterThan(5)
  await expect.poll(async () => difference(first, await page.screenshot()), { timeout: 5_000 }).toBeGreaterThan(0.03)
})

test('a piece animates by default in an embed', async ({ page }) => {
  await page.goto('/?embed=1#campfire')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  expect(await variation(first)).toBeGreaterThan(5)
  await expect.poll(async () => difference(first, await page.screenshot()), { timeout: 5_000 }).toBeGreaterThan(0.2)
})

test('animated=0 makes an embed hold still', async ({ page }) => {
  await page.goto('/?embed=1&animated=0#campfire')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  await page.waitForTimeout(1_500)
  expect(await difference(first, await page.screenshot())).toBeLessThan(0.2)
})

test('reduced motion holds an animated embed still', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/?embed=1&animated=1#campfire')
  await ready(page)
  await page.waitForTimeout(800)
  const first = await page.screenshot()
  await page.waitForTimeout(1_500)
  expect(await difference(first, await page.screenshot())).toBeLessThan(0.2)
})

test('the page follows the system theme', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' })
  await page.goto('/')
  await ready(page)
  await page.waitForTimeout(500)
  const dark = await sharp(await page.screenshot()).stats()
  await page.emulateMedia({ colorScheme: 'light' })
  await page.reload()
  await ready(page)
  await page.waitForTimeout(500)
  const light = await sharp(await page.screenshot()).stats()
  expect(light.channels[0].mean).toBeGreaterThan(dark.channels[0].mean + 100)
})

// The controls are drawn on the canvas, so they are reached by position. The viewport is fixed and the colors are
// forced so the layout is the same on every run; the phone project has another layout and skips them.
test.describe('controls', () => {
  test.beforeEach(({}, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', 'positions are for the desktop layout')
  })
  test.use({ viewport: { width: 1280, height: 1500 }, colorScheme: 'light' })

  const open = async (page: Page, path: string) => {
    await page.goto(path)
    await ready(page)
    await page.waitForTimeout(800)
  }

  test('the theme button switches between light and dark', async ({ page }) => {
    await open(page, '/?animated=0#campfire')
    const light = await sharp(await page.screenshot()).stats()
    await page.mouse.click(1101, 32)
    await page.waitForTimeout(500)
    const dark = await sharp(await page.screenshot()).stats()
    expect(light.channels[0].mean).toBeGreaterThan(dark.channels[0].mean + 100)
  })

  test('choosing full motion makes a held piece move', async ({ page }) => {
    await open(page, '/?animated=0#campfire')
    await page.mouse.click(392, 1027)
    await page.waitForTimeout(500)
    const first = await page.screenshot()
    await expect.poll(async () => difference(first, await page.screenshot()), { timeout: 5_000 }).toBeGreaterThan(0.1)
  })

  test('the copy buttons put the frame on the clipboard', async ({ page }) => {
    await open(page, '/?animated=0#campfire')
    await page.mouse.click(328, 1189)
    await expect
      .poll(async () => (await page.evaluate(() => navigator.clipboard.readText())).split('\n').length, { timeout: 5_000 })
      .toBeGreaterThan(10)
    await page.mouse.click(447, 1189)
    await expect
      .poll(async () => page.evaluate(() => navigator.clipboard.readText()), { timeout: 5_000 })
      .toContain('\u001b[')
    await page.mouse.click(564, 1189)
    await expect
      .poll(async () => page.evaluate(() => navigator.clipboard.readText()), { timeout: 5_000 })
      .toContain('<svg')
  })

  test('the field, charset and color pickers change the field', async ({ page }) => {
    await open(page, '/?animated=0#fields')
    const plasma = await page.screenshot({ clip: { x: 272, y: 293, width: 736, height: 360 } })
    await page.mouse.click(396, 727)
    await page.waitForTimeout(400)
    const noise = await page.screenshot({ clip: { x: 272, y: 293, width: 736, height: 360 } })
    expect(await difference(plasma, noise)).toBeGreaterThan(1)
    await page.mouse.click(688, 1208)
    await page.waitForTimeout(400)
    const blocks = await page.screenshot({ clip: { x: 272, y: 293, width: 736, height: 360 } })
    expect(await difference(noise, blocks)).toBeGreaterThan(1)
  })
})

// The terminal app is a full screen of its own: embedded, it fills the window. It is driven by the keyboard alone,
// which is what the phone apps and screen readers rely on. The canvas has no DOM, so changes are seen in pixels.
test.describe('terminal app', () => {
  test.beforeEach(({}, testInfo) => {
    test.skip(testInfo.project.name !== 'desktop', 'the keyboard flow is checked on the desktop layout')
  })
  test.use({ viewport: { width: 1280, height: 900 }, colorScheme: 'light' })

  const open = async (page: Page) => {
    await page.goto('/?embed=1&animated=0#terminal-app')
    await ready(page)
    await page.waitForTimeout(800)
  }
  const press = async (page: Page, ...keys: string[]) => {
    for (const key of keys) await page.keyboard.press(key)
    await page.waitForTimeout(300)
  }
  // Everything under the tabs; the focus marks on the tabs themselves are left out.
  const body = { x: 0, y: 90, width: 1280, height: 600 }
  const shot = (page: Page, clip = body) => page.screenshot({ clip })
  const click = async (page: Page, x: number, y: number) => {
    await page.mouse.click(x, y)
    await page.waitForTimeout(300)
  }
  const TAB_SERVERS = [59, 59] as const
  const ROW = [400, 125] as const
  const RESTART = [67, 540] as const

  test('the tabs switch with the arrow keys and with Alt and a digit', async ({ page }) => {
    await open(page)
    await click(page, ...TAB_SERVERS)
    const servers = await shot(page)
    await press(page, 'ArrowRight')
    const detail = await shot(page)
    expect(await difference(servers, detail)).toBeGreaterThan(1)
    await press(page, 'ArrowRight')
    const settings = await shot(page)
    expect(await difference(detail, settings)).toBeGreaterThan(1)
    await press(page, 'ArrowLeft', 'ArrowLeft')
    expect(await difference(servers, await shot(page))).toBeLessThan(0.2)
    await press(page, 'Alt+3')
    expect(await difference(settings, await shot(page))).toBeLessThan(0.2)
    await press(page, 'Alt+1')
    expect(await difference(servers, await shot(page))).toBeLessThan(0.2)
  })

  test('a settings toggle changes state, by pointer and by keyboard', async ({ page }) => {
    await open(page)
    await click(page, 232, 59)
    const toggle = { x: 0, y: 125, width: 400, height: 40 }
    const before = await shot(page, toggle)
    await click(page, 140, 146)
    const off = await shot(page, toggle)
    expect(await difference(before, off)).toBeGreaterThan(0.2)
    await click(page, 232, 59)
    await press(page, 'Tab', 'Space')
    expect(await difference(off, await shot(page, toggle))).toBeGreaterThan(0.2)
  })

  test('the restart dialog opens and closes with Escape and with its buttons', async ({ page }) => {
    await open(page)
    await click(page, ...ROW)
    const detail = await page.screenshot()
    await click(page, ...RESTART)
    expect(await difference(detail, await page.screenshot())).toBeGreaterThan(5)
    await press(page, 'Escape')
    expect(await difference(detail, await page.screenshot())).toBeLessThan(0.3)
    await click(page, ...RESTART)
    await click(page, 631, 482)
    expect(await difference(detail, await page.screenshot())).toBeLessThan(0.3)
    await click(page, ...RESTART)
    await click(page, 731, 482)
    const restarted = await page.screenshot()
    expect(await difference(detail, restarted)).toBeGreaterThan(0.05)
  })
})
