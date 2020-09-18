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

package im.vector.app.core.resources

import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

object DateProvider {

    private val zoneId = ZoneId.systemDefault()
    private val zoneOffset by lazy {
        val now = currentLocalDateTime()
        zoneId.rules.getOffset(now)
    }

    fun toLocalDateTime(timestamp: Long?): LocalDateTime {
        val instant = Instant.ofEpochMilli(timestamp ?: 0)
        return LocalDateTime.ofInstant(instant, zoneId)
    }

    fun currentLocalDateTime(): LocalDateTime {
        val instant = Instant.now()
        return LocalDateTime.ofInstant(instant, zoneId)
    }

    fun toTimestamp(localDateTime: LocalDateTime): Long {
        return localDateTime.toInstant(zoneOffset).toEpochMilli()
    }
}

fun LocalDateTime.toTimestamp(): Long = DateProvider.toTimestamp(this)
