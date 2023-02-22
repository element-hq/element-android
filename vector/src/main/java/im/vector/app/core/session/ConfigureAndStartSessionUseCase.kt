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

package im.vector.app.core.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import im.vector.app.core.extensions.startSyncing
import im.vector.app.core.notification.NotificationsSettingUpdater
import im.vector.app.core.notification.PushRulesUpdater
import im.vector.app.core.session.clientinfo.UpdateMatrixClientInfoUseCase
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.session.coroutineScope
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.settings.devices.v2.notification.UpdateNotificationSettingsAccountDataUseCase
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

class ConfigureAndStartSessionUseCase @Inject constructor(
        @ApplicationContext private val context: Context,
        private val webRtcCallManager: WebRtcCallManager,
        private val updateMatrixClientInfoUseCase: UpdateMatrixClientInfoUseCase,
        private val vectorPreferences: VectorPreferences,
        private val notificationsSettingUpdater: NotificationsSettingUpdater,
        private val updateNotificationSettingsAccountDataUseCase: UpdateNotificationSettingsAccountDataUseCase,
        private val pushRulesUpdater: PushRulesUpdater,
) {

    fun execute(session: Session, startSyncing: Boolean = true) {
        Timber.i("Configure and start session for ${session.myUserId}. startSyncing: $startSyncing")
        session.open()
        if (startSyncing) {
            session.startSyncing(context)
        }
        session.pushersService().refreshPushers()
        webRtcCallManager.checkForProtocolsSupportIfNeeded()
        updateMatrixClientInfoIfNeeded(session)
        createNotificationSettingsAccountDataIfNeeded(session)
        notificationsSettingUpdater.onSessionStarted(session)
        pushRulesUpdater.onSessionStarted(session)
    }

    private fun updateMatrixClientInfoIfNeeded(session: Session) {
        session.coroutineScope.launch {
            if (vectorPreferences.isClientInfoRecordingEnabled()) {
                updateMatrixClientInfoUseCase.execute(session)
            }
        }
    }

    private fun createNotificationSettingsAccountDataIfNeeded(session: Session) {
        session.coroutineScope.launch {
            updateNotificationSettingsAccountDataUseCase.execute(session)
        }
    }
}
