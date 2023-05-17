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

import im.vector.app.core.pushers.UnifiedPushHelper
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Update the notification settings account data for the current session depending on whether
 * the background sync is enabled or not.
 */
class UpdateNotificationSettingsAccountDataUseCase @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val unifiedPushHelper: UnifiedPushHelper,
        private val getNotificationSettingsAccountDataUseCase: GetNotificationSettingsAccountDataUseCase,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase,
        private val deleteNotificationSettingsAccountDataUseCase: DeleteNotificationSettingsAccountDataUseCase,
) {

    suspend fun execute(session: Session) {
        if (unifiedPushHelper.isBackgroundSync()) {
            setCurrentNotificationStatus(session)
        } else {
            deleteCurrentNotificationStatus(session)
        }
    }

    private suspend fun setCurrentNotificationStatus(session: Session) {
        val deviceId = session.sessionParams.deviceId
        val areNotificationsSilenced = !vectorPreferences.areNotificationEnabledForDevice()
        val isSilencedAccountData = getNotificationSettingsAccountDataUseCase.execute(session, deviceId)?.isSilenced
        if (areNotificationsSilenced != isSilencedAccountData) {
            val notificationSettingsContent = LocalNotificationSettingsContent(
                    isSilenced = areNotificationsSilenced
            )
            setNotificationSettingsAccountDataUseCase.execute(session, deviceId, notificationSettingsContent)
        }
    }

    private suspend fun deleteCurrentNotificationStatus(session: Session) {
        deleteNotificationSettingsAccountDataUseCase.execute(session)
    }
}
