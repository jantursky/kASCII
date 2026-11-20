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
const SHOWCASE = [...read('samples/gallery/src/commonMain/kotlin/com/jantursky/kascii/samples/gallery/Catalog.kt').matchAll(
  /Page\(\s*"([a-z-]+)"/g,
)].map((match) => match[1])
const PIECES = ['a/CatalogA.kt', 'b/CatalogB.kt'].flatMap((file) =>
  [...read(`kascii-gallery/src/commonMain/kotlin/com/jantursky/kascii/gallery/${file}`).matchAll(/GalleryEntry\("([a-z-]+)"/g)].map(
    (match) => match[1],
  ),
)
const PAGES = [...SHOWCASE, ...PIECES]

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
  expect(SHOWCASE).toEqual(['overview', 'pieces', 'fields', 'renderers', 'modifier', 'theme'])
  expect(PIECES).toHaveLength(26)
})

test('the gallery loads without console errors', async ({ page }) => {
  const errors = collectErrors(page)
  await page.goto('/')
  await expect(page).toHaveTitle('Kascii gallery')
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

for (const path of ['/', '/#pieces', '/#fields', '/#campfire']) {
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
    await page.mouse.click(853, 32)
    await page.waitForTimeout(500)
    const dark = await sharp(await page.screenshot()).stats()
    expect(light.channels[0].mean).toBeGreaterThan(dark.channels[0].mean + 100)
  })

  test('choosing full motion makes a held piece move', async ({ page }) => {
    await open(page, '/?animated=0#campfire')
    await page.mouse.click(392, 971)
    await page.waitForTimeout(500)
    const first = await page.screenshot()
    await expect.poll(async () => difference(first, await page.screenshot()), { timeout: 5_000 }).toBeGreaterThan(0.1)
  })

  test('the copy buttons put the frame on the clipboard', async ({ page }) => {
    await open(page, '/?animated=0#campfire')
    await page.mouse.click(328, 1133)
    await expect
      .poll(async () => (await page.evaluate(() => navigator.clipboard.readText())).split('\n').length, { timeout: 5_000 })
      .toBeGreaterThan(10)
    await page.mouse.click(447, 1133)
    await expect
      .poll(async () => page.evaluate(() => navigator.clipboard.readText()), { timeout: 5_000 })
      .toContain('\u001b[')
    await page.mouse.click(564, 1133)
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
