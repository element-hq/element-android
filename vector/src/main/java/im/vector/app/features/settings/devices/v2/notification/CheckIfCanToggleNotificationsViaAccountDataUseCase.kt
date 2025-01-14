/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class CheckIfCanToggleNotificationsViaAccountDataUseCase @Inject constructor(
        private val getNotificationSettingsAccountDataUseCase: GetNotificationSettingsAccountDataUseCase,
) {

    fun execute(session: Session, deviceId: String): Boolean {
        return getNotificationSettingsAccountDataUseCase.execute(session, deviceId)?.isSilenced != null
    }
}
