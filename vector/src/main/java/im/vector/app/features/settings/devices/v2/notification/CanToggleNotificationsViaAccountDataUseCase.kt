/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

class CanToggleNotificationsViaAccountDataUseCase @Inject constructor(
        private val getNotificationSettingsAccountDataUpdatesUseCase: GetNotificationSettingsAccountDataUpdatesUseCase,
) {

    fun execute(session: Session, deviceId: String): Flow<Boolean> {
        return getNotificationSettingsAccountDataUpdatesUseCase.execute(session, deviceId)
                .map { it?.isSilenced != null }
    }
}
