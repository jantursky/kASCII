package com.jantursky.kascii.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.LocalKasciiTheme
import com.jantursky.kascii.core.Grid
import com.jantursky.kascii.core.KasciiTheme
import com.jantursky.kascii.core.label

/**
 * A month grid with weeks starting on Monday. [selectedDay] is drawn as `[14]`, [today] as `(14)`. [month] is
 * 1 to 12.
 */
@Composable
public fun AsciiCalendar(
    year: Int,
    month: Int,
    modifier: Modifier = Modifier,
    selectedDay: Int? = null,
    today: Int? = null,
) {
    val title = "${MONTH_NAMES[month - 1]} $year"
    val description = listOfNotNull(
        "Calendar, $title",
        selectedDay?.let { "selected day $it" },
        today?.let { "today $it" },
    )
    GridSurface(
        grid = renderCalendar(year, month, selectedDay, today, LocalKasciiTheme.current),
        modifier = modifier,
        description = description.joinToString(", "),
    )
}

internal fun renderCalendar(year: Int, month: Int, selectedDay: Int?, today: Int?, theme: KasciiTheme): Grid {
    val days = daysInMonth(year, month)
    val offset = (dayOfWeek(year, month, 1) + 6) % 7
    val grid = Grid(DAY_WIDTH * 7, 2 + (offset + days + 6) / 7)
    grid.label(grid.cols / 2, 0, "${MONTH_NAMES[month - 1]} $year")
    WEEKDAYS.forEachIndexed { i, name -> grid.write(i * DAY_WIDTH, 1, " $name ", theme.dim()) }
    for (day in 1..days) {
        val slot = offset + day - 1
        val text = when (day) {
            selectedDay -> "[${day.toString().padStart(2)}]"
            today -> "(${day.toString().padStart(2)})"
            else -> " ${day.toString().padStart(2)} "
        }
        grid.write(slot % 7 * DAY_WIDTH, 2 + slot / 7, text, if (day == selectedDay) theme.accent else null)
    }
    return grid
}

internal fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeap(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

/** Day of the week for a date: 0 is Sunday. Sakamoto's method. */
internal fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val y = if (month < 3) year - 1 else year
    return (y + y / 4 - y / 100 + y / 400 + MONTH_OFFSETS[month - 1] + day) % 7
}

private val MONTH_OFFSETS = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val WEEKDAYS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
private const val DAY_WIDTH = 4
