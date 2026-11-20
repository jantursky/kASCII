package com.jantursky.kascii.samples.gallery.terminal

import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.Motion
import com.jantursky.kascii.samples.gallery.GalleryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalStateTest {
    private val web = Fleet.servers.first { it.id == "web-1" }

    @Test
    fun theAppStartsOnTheListWithNothingSelected() {
        val state = TerminalState()
        assertEquals(Screen.Servers, state.screen)
        assertNull(state.selected)
        assertEquals(Fleet.servers.size, state.visible.size)
    }

    @Test
    fun openingAServerShowsItsDetail() {
        val state = TerminalState()
        state.open(web.id)
        assertEquals(Screen.Detail, state.screen)
        assertEquals(web.id, state.selected?.id)
    }

    @Test
    fun stepMovesBetweenTabsAndStopsAtTheEnds() {
        val state = TerminalState()
        state.step(-1)
        assertEquals(Screen.Servers, state.screen)
        state.step(1)
        state.step(1)
        state.step(1)
        assertEquals(Screen.Settings, state.screen)
    }

    @Test
    fun aRestartWaitsForItsConfirmation() {
        val state = TerminalState()
        state.open("web-2")
        state.askRestart()
        assertEquals("web-2", state.restarting?.id)
        assertEquals(Health.Degraded, state.selected?.health)
        state.confirmRestart()
        assertNull(state.restarting)
        assertEquals(Health.Healthy, state.selected?.health)
        assertEquals("web-2 restarted", state.toast?.message)
        assertTrue(
            state.selected!!
                .log
                .last()
                .endsWith("started"),
        )
    }

    @Test
    fun dismissingTheDialogChangesNothing() {
        val state = TerminalState()
        state.open("web-2")
        state.askRestart()
        state.dismissRestart()
        assertNull(state.restarting)
        assertNull(state.toast)
        assertEquals(Health.Degraded, state.selected?.health)
    }

    @Test
    fun confirmingWithNothingAskedDoesNothing() {
        val state = TerminalState()
        state.confirmRestart()
        assertNull(state.toast)
        assertEquals(Fleet.servers, state.servers)
    }

    @Test
    fun noToastWhenNotificationsAreOff() {
        val state = TerminalState()
        state.settings.notifications = false
        state.open("web-2")
        state.askRestart()
        state.confirmRestart()
        assertNull(state.toast)
        assertEquals(Health.Healthy, state.selected?.health)
    }

    @Test
    fun aToastCanBeDismissed() {
        val state = TerminalState()
        state.open("web-2")
        state.askRestart()
        state.confirmRestart()
        assertNotNull(state.toast)
        state.dismissToast()
        assertNull(state.toast)
    }

    @Test
    fun theFiltersNarrowTheList() {
        val state = TerminalState()
        state.settings.problemsOnly = true
        assertEquals(listOf("web-2", "api-2", "db-2"), state.visible.map { it.id })
        state.settings.region = SettingsState.Regions.indexOf("us-east")
        assertEquals(listOf("db-2"), state.visible.map { it.id })
        state.settings.problemsOnly = false
        assertEquals(listOf("web-3", "api-1", "db-2", "batch-1"), state.visible.map { it.id })
    }

    @Test
    fun refreshAddsAReadingAndKeepsTheHistoryShort() {
        val state = TerminalState()
        repeat(HISTORY + 5) { state.refresh() }
        assertTrue(state.servers.all { it.load.size == HISTORY })
        assertNotEquals(Fleet.servers.first().load, state.servers.first().load)
    }

    @Test
    fun aServerThatIsDownReadsZero() {
        val down = Fleet.servers.first { it.health == Health.Down }
        assertEquals(0f, down.sampled(0.9f).currentLoad)
        assertEquals(0.9f, web.sampled(0.9f).currentLoad)
    }

    @Test
    fun theLoadChangeIsAPercentageOfTheLastReading() {
        val server = web.copy(load = listOf(0.4f, 0.5f))
        assertEquals(25f, server.loadChange, 0.01f)
        assertEquals(0f, server.copy(load = listOf(0.5f)).loadChange)
        assertEquals(0f, server.copy(load = listOf(0f, 0.5f)).loadChange)
    }

    @Test
    fun hostLabelsAreShortLowercaseAndSafe() {
        assertEquals("guest", hostLabel(""))
        assertEquals("guest", hostLabel("  !! "))
        assertEquals("ada-lovelace", hostLabel("  Ada Lovelace "))
        assertEquals("a-b", hostLabel("a___b"))
        assertEquals(16, hostLabel("x".repeat(40)).length)
        assertFalse(hostLabel("abcdefghijklmno pq").endsWith("-"))
    }

    @Test
    fun theThemeChoiceFollowsTheGalleryState() {
        val gallery = GalleryState(dark = true, motion = Motion.System)
        assertEquals(1, gallery.themeIndex())
        gallery.chooseTheme(0)
        assertFalse(gallery.dark)
        assertEquals(0, gallery.themeIndex())
        gallery.chooseTheme(2)
        assertEquals(KasciiTheme.Phosphor, gallery.theme)
        assertEquals(2, gallery.themeIndex())
        gallery.chooseTheme(1)
        assertNull(gallery.themeOverride)
        assertTrue(gallery.dark)
    }

    @Test
    fun columnsFitTheWidthInWholeCellsWithinTheLimits() {
        assertEquals(60, fitCols(2000f, 60))
        assertEquals(41, fitCols(331f, 60))
        assertEquals(20, fitCols(40f, 60))
    }
}
