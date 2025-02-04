/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.core.di.ActiveSessionHolder
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import javax.inject.Inject

class ToggleNotificationsUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val checkIfCanToggleNotificationsViaPusherUseCase: CheckIfCanToggleNotificationsViaPusherUseCase,
        private val checkIfCanToggleNotificationsViaAccountDataUseCase: CheckIfCanToggleNotificationsViaAccountDataUseCase,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase,
) {

    suspend fun execute(deviceId: String, enabled: Boolean) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return

        if (checkIfCanToggleNotificationsViaPusherUseCase.execute(session)) {
            val devicePusher = session.pushersService().getPushers().firstOrNull { it.deviceId == deviceId }
            devicePusher?.let { pusher ->
                session.pushersService().togglePusher(pusher, enabled)
            }
        }

        if (checkIfCanToggleNotificationsViaAccountDataUseCase.execute(session, deviceId)) {
            val newNotificationSettingsContent = LocalNotificationSettingsContent(isSilenced = !enabled)
            setNotificationSettingsAccountDataUseCase.execute(session, deviceId, newNotificationSettingsContent)
        }
    }
}
