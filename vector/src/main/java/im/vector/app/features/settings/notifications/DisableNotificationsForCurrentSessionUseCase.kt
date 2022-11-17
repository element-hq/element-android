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

package im.vector.app.features.settings.notifications

import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.features.settings.devices.v2.notification.CheckIfCanTogglePushNotificationsViaPusherUseCase
import im.vector.app.features.settings.devices.v2.notification.TogglePushNotificationUseCase
import javax.inject.Inject

class DisableNotificationsForCurrentSessionUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val unifiedPushHelper: UnifiedPushHelper,
        private val pushersManager: PushersManager,
        private val checkIfCanTogglePushNotificationsViaPusherUseCase: CheckIfCanTogglePushNotificationsViaPusherUseCase,
        private val togglePushNotificationUseCase: TogglePushNotificationUseCase,
) {

    suspend fun execute() {
        val session = activeSessionHolder.getSafeActiveSession() ?: return
        val deviceId = session.sessionParams.deviceId ?: return
        if (checkIfCanTogglePushNotificationsViaPusherUseCase.execute(session)) {
            togglePushNotificationUseCase.execute(deviceId, enabled = false)
        } else {
            unifiedPushHelper.unregister(pushersManager)
        }
    }
}
