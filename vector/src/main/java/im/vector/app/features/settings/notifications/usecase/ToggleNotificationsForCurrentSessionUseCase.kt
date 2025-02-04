/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.features.settings.devices.v2.notification.CheckIfCanToggleNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.DeleteNotificationSettingsAccountDataUseCase
import im.vector.app.features.settings.devices.v2.notification.SetNotificationSettingsAccountDataUseCase
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import timber.log.Timber
import javax.inject.Inject

class ToggleNotificationsForCurrentSessionUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val unifiedPushHelper: UnifiedPushHelper,
        private val checkIfCanToggleNotificationsViaPusherUseCase: CheckIfCanToggleNotificationsViaPusherUseCase,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase,
        private val deleteNotificationSettingsAccountDataUseCase: DeleteNotificationSettingsAccountDataUseCase,
) {

    suspend fun execute(enabled: Boolean) {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val deviceId = session.sessionParams.deviceId

        if (unifiedPushHelper.isBackgroundSync()) {
            Timber.d("background sync is enabled, setting account data event")
            val newNotificationSettingsContent = LocalNotificationSettingsContent(isSilenced = !enabled)
            setNotificationSettingsAccountDataUseCase.execute(session, deviceId, newNotificationSettingsContent)
        } else {
            Timber.d("push notif is enabled, deleting any account data and updating pusher")
            deleteNotificationSettingsAccountDataUseCase.execute(session)

            if (checkIfCanToggleNotificationsViaPusherUseCase.execute(session)) {
                val devicePusher = session.pushersService().getPushers().firstOrNull { it.deviceId == deviceId }
                devicePusher?.let { pusher ->
                    session.pushersService().togglePusher(pusher, enabled)
                }
            }
        }
    }
}
