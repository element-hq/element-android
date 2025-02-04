/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.timer.Clock
import javax.inject.Inject

class SetUnverifiedSessionsAlertShownUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val clock: Clock,
) {

    fun execute(deviceIds: List<String>) {
        val epochMillis = clock.epochMillis()
        deviceIds.forEach {
            vectorPreferences.setUnverifiedSessionsAlertLastShownMillis(it, epochMillis)
        }
    }
}
