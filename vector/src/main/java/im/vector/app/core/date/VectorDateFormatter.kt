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
import android.text.format.DateUtils
import im.vector.app.core.resources.LocaleProvider
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class VectorDateFormatter @Inject constructor(private val context: Context,
                                              private val localeProvider: LocaleProvider) {

    private val messageHourFormatter by lazy {
        DateTimeFormatter.ofPattern("H:mm", localeProvider.current())
    }
    private val messageDayFormatter by lazy {
        DateTimeFormatter.ofPattern("EEE d MMM", localeProvider.current())
    }

    fun formatMessageHour(localDateTime: LocalDateTime): String {
        return messageHourFormatter.format(localDateTime)
    }

    fun formatMessageDay(localDateTime: LocalDateTime): String {
        return messageDayFormatter.format(localDateTime)
    }

    fun formatRelativeDateTime(time: Long?): String {
        if (time == null) {
            return ""
        }
        return DateUtils.getRelativeDateTimeString(
                context,
                time,
                DateUtils.DAY_IN_MILLIS,
                2 * DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_TIME
        ).toString()
    }
}
