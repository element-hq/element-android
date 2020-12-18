/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.date

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import im.vector.app.core.resources.DateProvider
import im.vector.app.core.resources.LocaleProvider
import im.vector.app.core.resources.toTimestamp
import org.threeten.bp.LocalDateTime
import org.threeten.bp.Period
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.absoluteValue

class VectorDateFormatter @Inject constructor(private val context: Context,
                                              private val localeProvider: LocaleProvider,
                                              private val dateFormatterProviders: DateFormatterProviders) {

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
            DateFormatKind.ROOM_LIST             -> formatTimeOrDate(
                    date = localDateTime,
                    showTimeIfSameDay = true,
                    abbrev = true,
                    useRelative = true
            )
            DateFormatKind.TIMELINE_DAY_DIVIDER  -> formatTimeOrDate(
                    date = localDateTime,
                    alwaysShowYear = true
            )
            DateFormatKind.MESSAGE_DETAIL        -> formatFullDate(localDateTime)
            DateFormatKind.MESSAGE_SIMPLE        -> formatHour(localDateTime)
            DateFormatKind.EDIT_HISTORY_ROW      -> formatHour(localDateTime)
            DateFormatKind.EDIT_HISTORY_HEADER   -> formatTimeOrDate(
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
     * This method will show date and time with a preposition
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
     * We are using this method for the keywords Today/Yesterday
     */
    private fun getRelativeDay(ts: Long): String {
        return DateUtils.getRelativeTimeSpanString(
                ts,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString()
    }
}
