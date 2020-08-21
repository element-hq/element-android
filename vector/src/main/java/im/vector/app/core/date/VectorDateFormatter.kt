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
import im.vector.app.core.resources.LocaleProvider
import org.threeten.bp.LocalDateTime
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
                                              private val localeProvider: LocaleProvider) {

    private val messageHourFormatter by lazy {
        DateTimeFormatter.ofPattern("H:mm", localeProvider.current())
    }

    private val messageDayFormatter by lazy {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(localeProvider.current(), "EEE d MMM"))
    }

    fun formatMessageHour(localDateTime: LocalDateTime): String {
        return messageHourFormatter.format(localDateTime)
    }

    fun formatMessageDay(localDateTime: LocalDateTime): String {
        return messageDayFormatter.format(localDateTime)
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
        return DateUtils.getRelativeDateTimeString(
                context,
                time,
                DateUtils.DAY_IN_MILLIS,
                now - startOfDay(now - 2 * DateUtils.DAY_IN_MILLIS),
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_TIME
        ).toString()
    }
}
