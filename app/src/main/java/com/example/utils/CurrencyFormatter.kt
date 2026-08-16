package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object CurrencyFormatter {
    private val numberFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
    private val preciseFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun format(amount: Double, symbol: String = "৳", forceDecimals: Boolean = false): String {
        val formattedNumber = if (forceDecimals || amount % 1.0 != 0.0) {
            preciseFormat.format(amount)
        } else {
            numberFormat.format(amount)
        }
        return "$symbol $formattedNumber"
    }

    fun formatCompact(amount: Double, symbol: String = "৳"): String {
        return when {
            amount >= 100_000_000 -> "$symbol ${numberFormat.format(amount / 10_000_000)} Cr"
            amount >= 100_000 -> "$symbol ${numberFormat.format(amount / 100_000)} Lk"
            amount >= 10_000 -> "$symbol ${numberFormat.format(amount / 1_000)}k"
            else -> format(amount, symbol)
        }
    }
}

object DateUtils {
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val time12hFormatter = DateTimeFormatter.ofPattern("h:mm a")

    fun formatFullDate(date: LocalDate): String = date.format(fullDateFormatter)
    fun formatMonthYear(yearMonth: YearMonth): String = yearMonth.format(monthYearFormatter)
    fun formatShortDate(date: LocalDate): String = date.format(shortDateFormatter)
    fun formatTime(time: LocalTime): String = time.format(time12hFormatter)

    fun formatRelativeDate(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(shortDateFormatter)
        }
    }

    fun getGreeting(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Good morning, Master"
            in 12..16 -> "Good afternoon, Master"
            in 17..21 -> "Good evening, Master"
            else -> "Good night, Master"
        }
    }
}
