package com.jantursky.kascii.samples.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.samples.gallery.examples.CalendarExample
import com.jantursky.kascii.samples.gallery.examples.ChartsExample
import com.jantursky.kascii.samples.gallery.examples.DialogExample
import com.jantursky.kascii.samples.gallery.examples.FormsExample
import com.jantursky.kascii.samples.gallery.examples.MetersExample
import com.jantursky.kascii.samples.gallery.examples.ProgressExample
import com.jantursky.kascii.samples.gallery.examples.SurfacesExample
import com.jantursky.kascii.samples.gallery.examples.TabsAndTablesExample
import com.jantursky.kascii.samples.gallery.examples.TerminalExample
import com.jantursky.kascii.samples.gallery.examples.TextEffectsExample

/** A page of components: links to its siblings, then the examples, each on its own stage. Embeds show the stages. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentsPage(
    state: GalleryState,
    vararg examples: @Composable () -> Unit,
) {
    val navigate = LocalNavigate.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!state.embed) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComponentPages.forEach { Pill(it.title, { navigate(it.id) }) }
            }
        }
        examples.forEach { example -> Stage(state, minHeight = 160) { Box(Modifier.fillMaxWidth()) { example() } } }
        if (!state.embed) Controls { ThemeControl(state) }
    }
}

/** The pages of the components group, in nav order. */
val ComponentPages: List<Page> =
    listOf(
        Page(
            "forms",
            Group.Components,
            "Buttons and forms",
            "Buttons, checkboxes, toggles, radio groups, sliders and text fields. They are focusable, " +
                "pressed with Space or Enter and named for screen readers.",
            listOf("FormsExample"),
        ) { ComponentsPage(it, { FormsExample() }) },
        Page(
            "surfaces",
            Group.Components,
            "Cards and badges",
            "Cards, dividers, badges and avatars: static art drawn from the theme.",
            listOf("SurfacesExample"),
        ) { ComponentsPage(it, { SurfacesExample() }) },
        Page(
            "navigation",
            Group.Components,
            "Tabs, tables and dates",
            "Tabs, tables, trees, a calendar and a clock. The caller owns the state and the date.",
            listOf("TabsAndTablesExample", "CalendarExample"),
        ) { ComponentsPage(it, { TabsAndTablesExample() }, { CalendarExample() }) },
        Page(
            "terminal",
            Group.Components,
            "Terminal and dialogs",
            "A terminal, a boot log, a not-found screen, a dialog and toasts.",
            listOf("TerminalExample", "DialogExample"),
        ) { ComponentsPage(it, { TerminalExample() }, { DialogExample() }) },
        Page(
            "progress",
            Group.Components,
            "Progress and loading",
            "Progress bars, spinners and skeletons, determinate or not.",
            listOf("ProgressExample"),
        ) { ComponentsPage(it, { ProgressExample() }) },
        Page(
            "charts",
            Group.Components,
            "Charts",
            "Bar, line, candlestick, pie, radar and heat map charts, and a sparkline.",
            listOf("ChartsExample"),
            wide = true,
        ) { ComponentsPage(it, { ChartsExample() }) },
        Page(
            "meters",
            Group.Components,
            "Meters",
            "A gauge, CPU meters, an uptime bar, a stat card, an equalizer and a heartbeat.",
            listOf("MetersExample"),
        ) { ComponentsPage(it, { MetersExample() }) },
        Page(
            "text-effects",
            Group.Components,
            "Text effects",
            "Big text, typewriter, scramble, glitch, dissolve, marquee, wave, split-flap, morse, rainbow and shimmer.",
            listOf("TextEffectsExample"),
        ) { ComponentsPage(it, { TextEffectsExample() }) },
    )
