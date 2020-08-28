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
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Returns the timestamp for the start of the day of the provided time.
 * For example, for the time "Jul 21, 11:11" the start of the day: "Jul 21, 00:00" is returned.
 */
fun startOfDay(time: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.time = Date(time)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time.time
}

class VectorDateFormatter @Inject constructor(private val context: Context,
                                              private val localeProvider: LocaleProvider,
                                              private val dateFormatterProviders: DateFormatterProviders) {

    private val hourFormatter by lazy {
        if (DateFormat.is24HourFormat(context)) {
            DateTimeFormatter.ofPattern("H:mm", localeProvider.current())
        } else {
            DateTimeFormatter.ofPattern("h:mm a", localeProvider.current())
        }
    }

    private val dayFormatter by lazy {
        DateTimeFormatter.ofPattern("EEE", localeProvider.current())
    }

    private val fullDateFormatter by lazy {
        if (DateFormat.is24HourFormat(context)) {
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy H:mm", localeProvider.current())
        } else {
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy h:mm a", localeProvider.current())
        }
    }

    fun formatMessageHour(localDateTime: LocalDateTime): String {
        return hourFormatter.format(localDateTime)
    }

    fun formatMessageDay(localDateTime: LocalDateTime): String {
        return dayFormatter.format(localDateTime)
    }

    fun formatMessageDayWithMonth(localDateTime: LocalDateTime, abbrev: Boolean = false): String {
        return dateFormatterProviders.provide(abbrev).dateWithMonthFormatter.format(localDateTime)
    }

    fun formatMessageDayWithYear(localDateTime: LocalDateTime, abbrev: Boolean = false): String {
        return dateFormatterProviders.provide(abbrev).dateWithYearFormatter.format(localDateTime)
    }

    fun formatMessageDate(
            date: LocalDateTime?,
            showFullDate: Boolean = false,
            onlyTimeIfSameDay: Boolean = false,
            useRelative: Boolean = false,
            alwaysShowYear: Boolean = false,
            abbrev: Boolean = false
    ): String {
        if (date == null) {
            return ""
        }
        if (showFullDate) {
            return fullDateFormatter.format(date)
        }
        val currentDate = DateProvider.currentLocalDateTime()
        val isSameDay = date.toLocalDate() == currentDate.toLocalDate()
        return if (onlyTimeIfSameDay && isSameDay) {
            formatMessageHour(date)
        } else {
            val period = Period.between(date.toLocalDate(), currentDate.toLocalDate())
            if (period.years >= 1 || alwaysShowYear) {
                formatMessageDayWithYear(date, abbrev)
            } else if (period.months >= 1) {
                formatMessageDayWithMonth(date, abbrev)
            } else if (useRelative && period.days < 2) {
                getRelativeDay(date.toTimestamp())
            } else if (useRelative && period.days < 7) {
                formatMessageDay(date)
            } else {
                formatMessageDayWithMonth(date, abbrev)
            }
        }
    }

    /**
     * Formats a localized relative date time for the last 2 days, e.g, "Today, HH:MM", "Yesterday, HH:MM" or
     * "2 days ago, HH:MM".
     * For earlier timestamps the absolute date time is returned, e.g. "Month Day, HH:MM".
     *
     * @param time the absolute timestamp [ms] that should be formatted relative to now
     */
    fun formatRelativeDateTime(time: Long?): String {
        if (time == null) {
            return ""
        }
        val now = System.currentTimeMillis()
        var flags = DateUtils.FORMAT_SHOW_WEEKDAY

        return DateUtils.getRelativeDateTimeString(
                context,
                time,
                DateUtils.DAY_IN_MILLIS,
                now - startOfDay(now - 2 * DateUtils.DAY_IN_MILLIS),
                flags
        ).toString()
    }

    private fun getRelativeDay(ts: Long): String {
        return DateUtils.getRelativeTimeSpanString(
                ts,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY).toString()
    }
}
