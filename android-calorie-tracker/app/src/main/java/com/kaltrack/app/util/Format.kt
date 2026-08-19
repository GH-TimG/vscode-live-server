package com.kaltrack.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

/** "Heute" / "Gestern" / "Montag, 17. August" – liest sich besser als ein Datum. */
fun formatDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Heute"
    today.minusDays(1) -> "Gestern"
    today.plusDays(1) -> "Morgen"
    else -> date.format(DAY_FORMAT)
}

/** Ganze Zahl, wenn möglich – "84 kcal" statt "84,0 kcal". */
fun formatNumber(value: Double, decimals: Int = 0): String =
    String.format(Locale.GERMANY, "%.${decimals}f", value)

fun formatGrams(value: Double): String {
    val decimals = if (value < 10 && value % 1.0 != 0.0) 1 else 0
    return formatNumber(value, decimals)
}
