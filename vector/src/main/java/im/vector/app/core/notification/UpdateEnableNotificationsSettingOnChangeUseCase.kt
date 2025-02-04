/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.notification

import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.devices.v2.notification.GetNotificationsStatusUseCase
import im.vector.app.features.settings.devices.v2.notification.NotificationsStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Listen for changes in either Pusher or Account data to update the local enable notifications
 * setting for the current device.
 */
class UpdateEnableNotificationsSettingOnChangeUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val getNotificationsStatusUseCase: GetNotificationsStatusUseCase,
) {

    suspend fun execute(session: Session) {
        val deviceId = session.sessionParams.deviceId
        getNotificationsStatusUseCase.execute(session, deviceId)
                .onEach(::updatePreference)
                .collect()
    }

    private fun updatePreference(notificationStatus: NotificationsStatus) {
        when (notificationStatus) {
            NotificationsStatus.ENABLED -> vectorPreferences.setNotificationEnabledForDevice(true)
            NotificationsStatus.DISABLED -> vectorPreferences.setNotificationEnabledForDevice(false)
            else -> Unit
        }
    }
}
