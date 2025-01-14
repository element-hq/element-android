/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import im.vector.lib.core.utils.timer.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckIfSessionIsInactiveUseCase @Inject constructor(
        private val clock: Clock,
) {

    fun execute(lastSeenTsMillis: Long?): Boolean {
        return if (lastSeenTsMillis == null || lastSeenTsMillis <= 0) {
            // in these situations we cannot say anything about the inactivity of the session
            false
        } else {
            val diffMilliseconds = clock.epochMillis() - lastSeenTsMillis
            diffMilliseconds >= TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong())
        }
    }
}
