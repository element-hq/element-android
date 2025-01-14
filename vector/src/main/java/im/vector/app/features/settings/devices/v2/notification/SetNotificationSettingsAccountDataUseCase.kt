/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toContent
import javax.inject.Inject

class SetNotificationSettingsAccountDataUseCase @Inject constructor() {

    suspend fun execute(session: Session, deviceId: String, localNotificationSettingsContent: LocalNotificationSettingsContent) {
        session.accountDataService().updateUserAccountData(
                UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + deviceId,
                localNotificationSettingsContent.toContent(),
        )
    }
}
