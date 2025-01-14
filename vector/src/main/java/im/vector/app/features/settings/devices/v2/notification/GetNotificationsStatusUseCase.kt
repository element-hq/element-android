/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import javax.inject.Inject

class GetNotificationsStatusUseCase @Inject constructor(
        private val canToggleNotificationsViaPusherUseCase: CanToggleNotificationsViaPusherUseCase,
        private val canToggleNotificationsViaAccountDataUseCase: CanToggleNotificationsViaAccountDataUseCase,
) {

    fun execute(session: Session, deviceId: String): Flow<NotificationsStatus> {
        return canToggleNotificationsViaAccountDataUseCase.execute(session, deviceId)
                .flatMapLatest { canToggle ->
                    if (canToggle) {
                        notificationStatusFromAccountData(session, deviceId)
                    } else {
                        notificationStatusFromPusher(session, deviceId)
                    }
                }
                .distinctUntilChanged()
    }

    private fun notificationStatusFromAccountData(session: Session, deviceId: String) =
            session.flow()
                    .liveUserAccountData(UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + deviceId)
                    .unwrap()
                    .map { it.content.toModel<LocalNotificationSettingsContent>()?.isSilenced?.not() }
                    .map { if (it == true) NotificationsStatus.ENABLED else NotificationsStatus.DISABLED }

    private fun notificationStatusFromPusher(session: Session, deviceId: String) =
            canToggleNotificationsViaPusherUseCase.execute(session)
                    .flatMapLatest { canToggle ->
                        if (canToggle) {
                            session.flow()
                                    .livePushers()
                                    .map { it.filter { pusher -> pusher.deviceId == deviceId } }
                                    .map { it.takeIf { it.isNotEmpty() }?.any { pusher -> pusher.enabled } }
                                    .map {
                                        when (it) {
                                            true -> NotificationsStatus.ENABLED
                                            false -> NotificationsStatus.DISABLED
                                            else -> NotificationsStatus.NOT_SUPPORTED
                                        }
                                    }
                                    .distinctUntilChanged()
                        } else {
                            flowOf(NotificationsStatus.NOT_SUPPORTED)
                        }
                    }
}
