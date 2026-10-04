package com.example.moviewather.ui.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting epoch millisecond timestamps into human-readable date strings.
 */
object DateFormatter {

    /**
     * Formats an epoch millisecond timestamp into a localized long-form date
     * (e.g. "October 2, 2026").
     *
     * @param timestamp Unix epoch timestamp in milliseconds.
     * @return Formatted date string, or empty string if timestamp is 0 or negative.
     */
    fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return ""
        val formatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
