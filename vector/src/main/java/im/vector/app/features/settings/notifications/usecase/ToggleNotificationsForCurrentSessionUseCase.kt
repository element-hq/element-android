/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
