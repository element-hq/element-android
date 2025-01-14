/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
