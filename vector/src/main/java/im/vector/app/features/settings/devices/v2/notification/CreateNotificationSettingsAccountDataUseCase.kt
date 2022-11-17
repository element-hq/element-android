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

class CreateNotificationSettingsAccountDataUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase
) {

    // TODO to be called on session start when background sync is enabled + when switching to background sync
    suspend fun execute(session: Session) {
        val deviceId = session.sessionParams.deviceId ?: return
        val isSilenced = !vectorPreferences.areNotificationEnabledForDevice()
        val notificationSettingsContent = LocalNotificationSettingsContent(
                isSilenced = isSilenced
        )
        setNotificationSettingsAccountDataUseCase.execute(deviceId, notificationSettingsContent)
    }
}
