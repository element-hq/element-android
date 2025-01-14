/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset

object DateProvider {

    // recompute the zoneId each time we access it to handle change of timezones
    private val defaultZoneId: ZoneId
        get() = ZoneId.systemDefault()

    // recompute the zoneOffset each time we access it to handle change of timezones
    private val defaultZoneOffset: ZoneOffset
        get() = defaultZoneId.rules.getOffset(currentLocalDateTime())

    fun toLocalDateTime(timestamp: Long?): LocalDateTime {
        val instant = Instant.ofEpochMilli(timestamp ?: 0)
        return LocalDateTime.ofInstant(instant, defaultZoneId)
    }

    fun currentLocalDateTime(): LocalDateTime {
        val instant = Instant.now()
        return LocalDateTime.ofInstant(instant, defaultZoneId)
    }

    fun toTimestamp(localDateTime: LocalDateTime): Long {
        return localDateTime.toInstant(defaultZoneOffset).toEpochMilli()
    }
}

fun LocalDateTime.toTimestamp(): Long = DateProvider.toTimestamp(this)
