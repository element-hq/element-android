/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toModel
import javax.inject.Inject

class GetNotificationSettingsAccountDataUpdatesUseCase @Inject constructor() {

    fun execute(session: Session, deviceId: String): Flow<LocalNotificationSettingsContent?> {
        return session
                .accountDataService()
                .getLiveUserAccountDataEvent(UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + deviceId)
                .asFlow()
                .map { it.getOrNull()?.content?.toModel<LocalNotificationSettingsContent>() }
    }
}
