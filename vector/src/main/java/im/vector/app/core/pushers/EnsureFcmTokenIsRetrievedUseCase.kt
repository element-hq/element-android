/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import im.vector.app.core.di.ActiveSessionHolder
import javax.inject.Inject

class EnsureFcmTokenIsRetrievedUseCase @Inject constructor(
        private val unifiedPushHelper: UnifiedPushHelper,
        private val fcmHelper: FcmHelper,
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(pushersManager: PushersManager, registerPusher: Boolean) {
        if (unifiedPushHelper.isEmbeddedDistributor()) {
            fcmHelper.ensureFcmTokenIsRetrieved(pushersManager, shouldAddHttpPusher(registerPusher))
        }
    }

    private fun shouldAddHttpPusher(registerPusher: Boolean) = if (registerPusher) {
        val currentSession = activeSessionHolder.getActiveSession()
        val currentPushers = currentSession.pushersService().getPushers()
        currentPushers.none { it.deviceId == currentSession.sessionParams.deviceId }
    } else {
        false
    }
}
