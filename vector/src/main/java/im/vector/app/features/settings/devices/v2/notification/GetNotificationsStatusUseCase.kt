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

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.account.LocalNotificationSettingsContent
import org.matrix.android.sdk.api.session.accountdata.UserAccountDataTypes
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.flow.flow
import org.matrix.android.sdk.flow.unwrap
import javax.inject.Inject

class GetNotificationsStatusUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val checkIfCanTogglePushNotificationsViaPusherUseCase: CheckIfCanTogglePushNotificationsViaPusherUseCase,
        private val checkIfCanTogglePushNotificationsViaAccountDataUseCase: CheckIfCanTogglePushNotificationsViaAccountDataUseCase,
) {

    // TODO add unit tests
    fun execute(deviceId: String): Flow<NotificationsStatus> {
        val session = activeSessionHolder.getSafeActiveSession()
        return when {
            session == null -> emptyFlow()
            checkIfCanTogglePushNotificationsViaPusherUseCase.execute() -> {
                session.flow()
                        .livePushers()
                        .map { it.filter { pusher -> pusher.deviceId == deviceId } }
                        .map { it.takeIf { it.isNotEmpty() }?.any { pusher -> pusher.enabled } }
                        .map { if (it == true) NotificationsStatus.ENABLED else NotificationsStatus.DISABLED }
            }
            checkIfCanTogglePushNotificationsViaAccountDataUseCase.execute(deviceId) -> {
                session.flow()
                        .liveUserAccountData(UserAccountDataTypes.TYPE_LOCAL_NOTIFICATION_SETTINGS + deviceId)
                        .unwrap()
                        .map { it.content.toModel<LocalNotificationSettingsContent>()?.isSilenced?.not() }
                        .map { if (it == true) NotificationsStatus.ENABLED else NotificationsStatus.DISABLED }
            }
            else -> flowOf(NotificationsStatus.NOT_SUPPORTED)
        }
    }
}
