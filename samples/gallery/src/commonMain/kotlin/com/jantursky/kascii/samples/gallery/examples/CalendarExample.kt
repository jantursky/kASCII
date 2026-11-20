package com.jantursky.kascii.samples.gallery.examples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kascii.components.AsciiBigFont
import com.jantursky.kascii.components.AsciiCalendar
import com.jantursky.kascii.components.AsciiClock

// Neither component reads the platform clock: you give them the date and the time, which keeps them testable.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarExample(modifier: Modifier = Modifier) {
    FlowRow(modifier, Arrangement.spacedBy(32.dp), Arrangement.spacedBy(24.dp)) {
        AsciiCalendar(year = 2026, month = 11, selectedDay = 20, today = 12)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AsciiClock(hour = 9, minute = 41)
            AsciiClock(hour = 23, minute = 5, second = 30, font = AsciiBigFont.Tall)
        }
    }
}
