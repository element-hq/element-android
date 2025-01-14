/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Delete the content of any associated notification settings to the current session.
 */
class DeleteNotificationSettingsAccountDataUseCase @Inject constructor(
        private val getNotificationSettingsAccountDataUseCase: GetNotificationSettingsAccountDataUseCase,
        private val setNotificationSettingsAccountDataUseCase: SetNotificationSettingsAccountDataUseCase,
) {

    suspend fun execute(session: Session) {
        val deviceId = session.sessionParams.deviceId
        if (getNotificationSettingsAccountDataUseCase.execute(session, deviceId)?.isSilenced != null) {
            val emptyNotificationSettingsContent = LocalNotificationSettingsContent(
                    isSilenced = null
            )
            setNotificationSettingsAccountDataUseCase.execute(session, deviceId, emptyNotificationSettingsContent)
        }
    }
}
