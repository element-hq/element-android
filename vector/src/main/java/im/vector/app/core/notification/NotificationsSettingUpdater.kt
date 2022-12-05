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

package im.vector.app.core.notification

import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listen changes in Pusher or Account Data to update the local setting for notification toggle.
 */
@Singleton
class NotificationsSettingUpdater @Inject constructor(
        private val updateEnableNotificationsSettingOnChangeUseCase: UpdateEnableNotificationsSettingOnChangeUseCase,
) {

    private var job: Job? = null

    fun onSessionStarted(session: Session) {
        updateEnableNotificationsSettingOnChange(session)
    }

    private fun updateEnableNotificationsSettingOnChange(session: Session) {
        job?.cancel()
        job = session.coroutineScope.launch {
            updateEnableNotificationsSettingOnChangeUseCase.execute(session)
        }
    }
}
