/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import im.vector.app.config.Config
import im.vector.app.features.VectorFeatures
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.timer.Clock
import javax.inject.Inject

class ShouldShowUnverifiedSessionsAlertUseCase @Inject constructor(
        private val vectorFeatures: VectorFeatures,
        private val vectorPreferences: VectorPreferences,
        private val clock: Clock,
) {

    fun execute(deviceId: String?): Boolean {
        deviceId ?: return false

        val isUnverifiedSessionsAlertEnabled = vectorFeatures.isUnverifiedSessionsAlertEnabled()
        val unverifiedSessionsAlertLastShownMillis = vectorPreferences.getUnverifiedSessionsAlertLastShownMillis(deviceId)
        return isUnverifiedSessionsAlertEnabled &&
                clock.epochMillis() - unverifiedSessionsAlertLastShownMillis >= Config.SHOW_UNVERIFIED_SESSIONS_ALERT_AFTER_MILLIS
    }
}
