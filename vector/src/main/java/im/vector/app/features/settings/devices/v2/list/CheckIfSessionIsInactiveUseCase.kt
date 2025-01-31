/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.core.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckIfSessionIsInactiveUseCase @Inject constructor(
        private val clock: Clock,
) {

    fun execute(lastSeenTs: Long): Boolean {
        // In case of the server doesn't send the last seen date.
        if (lastSeenTs == 0L) return true

        val diffMilliseconds = clock.epochMillis() - lastSeenTs
        return diffMilliseconds >= TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong())
    }
}
