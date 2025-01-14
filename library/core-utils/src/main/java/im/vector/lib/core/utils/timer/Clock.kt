/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.core.utils.timer

interface Clock {
    fun epochMillis(): Long
}

class DefaultClock : Clock {

    /**
     * Provides a UTC epoch in milliseconds
     *
     * This value is not guaranteed to be correct with reality
     * as a User can override the system time and date to any values.
     */
    override fun epochMillis(): Long {
        return System.currentTimeMillis()
    }
}
