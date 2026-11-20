package com.jantursky.kascii.components

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * The words the components speak to screen readers, and the month and weekday names the calendar draws. Provide a
 * translated copy through [LocalAsciiStrings]; every default is English. [monthNames] has 12 entries starting with
 * January, [weekdays] has 7 starting with Monday. Labels you pass to a component directly win over these.
 *
 * @property loading the name of a spinner or skeleton without a label.
 * @property dismiss the name of a toast's close button.
 * @property calendar the first word of a calendar's description.
 * @property monthNames the 12 month names, January first.
 * @property weekdays the 7 short weekday names drawn above the days, Monday first.
 * @property morseCode the prefix of a Morse text's description.
 * @property cpuLoad the prefix of the CPU meters' description.
 * @property core the word for one CPU core.
 * @property uptime the first word of an uptime bar's description.
 * @property slotsDown the words after the count of down slots.
 * @property slider the name of a slider without a label.
 * @property textField the name of a text field without a label.
 * @property table the first word of a table's description.
 * @property columns the word for table columns.
 * @property row the word for a table row.
 * @property toneName how a toast announces its tone.
 */
public data class AsciiStrings(
    val loading: String = "Loading",
    val dismiss: String = "Dismiss",
    val calendar: String = "Calendar",
    val monthNames: List<String> =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        ),
    val weekdays: List<String> = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"),
    val morseCode: String = "Morse code",
    val cpuLoad: String = "CPU load",
    val core: String = "core",
    val uptime: String = "Uptime",
    val slotsDown: String = "slots down",
    val slider: String = "Slider",
    val textField: String = "Text field",
    val table: String = "Table",
    val columns: String = "columns",
    val row: String = "Row",
    val toneName: (AsciiTone) -> String = { it.name },
) {
    init {
        require(monthNames.size == 12) { "monthNames needs 12 entries" }
        require(weekdays.size == 7) { "weekdays needs 7 entries" }
    }
}

/** The [AsciiStrings] the components below this point use. */
public val LocalAsciiStrings: ProvidableCompositionLocal<AsciiStrings> = compositionLocalOf { AsciiStrings() }
