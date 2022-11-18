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

package im.vector.app.features.settings.devices.v2.notification

import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Update the notification settings account data for the current session.
 */
class UpdateNotificationSettingsAccountDataUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val getNotificationSettingsAccountDataUseCase: GetNotificationSettingsAccountDataUseCase,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase
) {

    // TODO to be called when switching to background sync (in notification method setting)
    suspend fun execute(session: Session) {
        val deviceId = session.sessionParams.deviceId ?: return
        val isSilencedLocal = !vectorPreferences.areNotificationEnabledForDevice()
        val isSilencedRemote = getNotificationSettingsAccountDataUseCase.execute(session, deviceId)?.isSilenced
        if (isSilencedLocal != isSilencedRemote) {
            val notificationSettingsContent = LocalNotificationSettingsContent(
                    isSilenced = isSilencedLocal
            )
            setNotificationSettingsAccountDataUseCase.execute(session, deviceId, notificationSettingsContent)
        }
    }
}
