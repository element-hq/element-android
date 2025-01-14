/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.date

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import im.vector.app.core.resources.DateProvider
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.toTimestamp
import im.vector.lib.core.utils.timer.Clock
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.absoluteValue

class VectorDateFormatter @Inject constructor(
        private val context: Context,
        private val localeProvider: LocaleProvider,
        private val dateFormatterProviders: DateFormatterProviders,
        private val clock: Clock,
) {

    private val hourFormatter by lazy {
        if (DateFormat.is24HourFormat(context)) {
            DateTimeFormatter.ofPattern("HH:mm", localeProvider.current())
        } else {
            DateTimeFormatter.ofPattern("h:mm a", localeProvider.current())
        }
    }

    private val fullDateFormatter by lazy {
        val pattern = if (DateFormat.is24HourFormat(context)) {
            DateFormat.getBestDateTimePattern(localeProvider.current(), "EEE, d MMM yyyy HH:mm")
        } else {
            DateFormat.getBestDateTimePattern(localeProvider.current(), "EEE, d MMM yyyy h:mm a")
        }
        DateTimeFormatter.ofPattern(pattern, localeProvider.current())
    }

    /**
     * This method is used to format some date in the app.
     * It will be able to show only time, only date or both with some logic.
     * @param ts the timestamp to format or null.
     * @param dateFormatKind the kind of format to use
     *
     * @return the formatted date as string.
     */
    fun format(ts: Long?, dateFormatKind: DateFormatKind): String {
        if (ts == null) return "-"
        val localDateTime = DateProvider.toLocalDateTime(ts)
        return when (dateFormatKind) {
            DateFormatKind.DEFAULT_DATE_AND_TIME -> formatDateAndTime(ts)
            DateFormatKind.ROOM_LIST -> formatTimeOrDate(
                    date = localDateTime,
                    showTimeIfSameDay = true,
                    abbrev = true,
                    useRelative = true
            )
            DateFormatKind.TIMELINE_DAY_DIVIDER -> formatTimeOrDate(
                    date = localDateTime,
                    alwaysShowYear = true
            )
            DateFormatKind.MESSAGE_DETAIL -> formatFullDate(localDateTime)
            DateFormatKind.MESSAGE_SIMPLE -> formatHour(localDateTime)
            DateFormatKind.EDIT_HISTORY_ROW -> formatHour(localDateTime)
            DateFormatKind.EDIT_HISTORY_HEADER -> formatTimeOrDate(
                    date = localDateTime,
                    abbrev = true,
                    useRelative = true
            )
        }
    }

    private fun formatFullDate(localDateTime: LocalDateTime): String {
        return fullDateFormatter.format(localDateTime)
    }

    private fun formatHour(localDateTime: LocalDateTime): String {
        return hourFormatter.format(localDateTime)
    }

    private fun formatDateWithMonth(localDateTime: LocalDateTime, abbrev: Boolean = false): String {
        return dateFormatterProviders.provide(abbrev).dateWithMonthFormatter.format(localDateTime)
    }

    private fun formatDateWithYear(localDateTime: LocalDateTime, abbrev: Boolean = false): String {
        return dateFormatterProviders.provide(abbrev).dateWithYearFormatter.format(localDateTime)
    }

    /**
     * This method will only show time or date following the parameters.
     */
    private fun formatTimeOrDate(
            date: LocalDateTime?,
            showTimeIfSameDay: Boolean = false,
            useRelative: Boolean = false,
            alwaysShowYear: Boolean = false,
            abbrev: Boolean = false
    ): String {
        if (date == null) {
            return ""
        }
        val currentDate = DateProvider.currentLocalDateTime()
        val isSameDay = date.toLocalDate() == currentDate.toLocalDate()
        return if (showTimeIfSameDay && isSameDay) {
            formatHour(date)
        } else {
            formatDate(date, currentDate, alwaysShowYear, abbrev, useRelative)
        }
    }

    private fun formatDate(
            date: LocalDateTime,
            currentDate: LocalDateTime,
            alwaysShowYear: Boolean,
            abbrev: Boolean,
            useRelative: Boolean
    ): String {
        val period = Period.between(date.toLocalDate(), currentDate.toLocalDate())
        return if (period.years.absoluteValue >= 1 || alwaysShowYear) {
            formatDateWithYear(date, abbrev)
        } else if (useRelative && period.days.absoluteValue < 2 && period.months.absoluteValue < 1) {
            getRelativeDay(date.toTimestamp())
        } else {
            formatDateWithMonth(date, abbrev)
        }
    }

    /**
     * This method will show date and time with a preposition.
     */
    private fun formatDateAndTime(ts: Long): String {
        val date = DateProvider.toLocalDateTime(ts)
        val currentDate = DateProvider.currentLocalDateTime()
        // This fake date is created to be able to use getRelativeTimeSpanString so we can get a "at"
        // preposition and the right am/pm management.
        val fakeDate = LocalDateTime.of(currentDate.toLocalDate(), date.toLocalTime())
        val formattedTime = DateUtils.getRelativeTimeSpanString(context, fakeDate.toTimestamp(), true).toString()
        val formattedDate = formatDate(date, currentDate, alwaysShowYear = false, abbrev = true, useRelative = true)
        return "$formattedDate $formattedTime"
    }

    /**
     * We are using this method for the keywords Today/Yesterday.
     */
    private fun getRelativeDay(ts: Long): String {
        return DateUtils.getRelativeTimeSpanString(
                ts,
                clock.epochMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY
        ).toString()
    }
}
