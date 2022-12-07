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
