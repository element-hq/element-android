/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.notifications.usecase

import im.vector.app.core.pushers.EnsureFcmTokenIsRetrievedUseCase
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.pushers.RegisterUnifiedPushUseCase
import javax.inject.Inject

class EnableNotificationsForCurrentSessionUseCase @Inject constructor(
        private val pushersManager: PushersManager,
        private val toggleNotificationsForCurrentSessionUseCase: ToggleNotificationsForCurrentSessionUseCase,
        private val registerUnifiedPushUseCase: RegisterUnifiedPushUseCase,
        private val ensureFcmTokenIsRetrievedUseCase: EnsureFcmTokenIsRetrievedUseCase,
) {

    sealed interface EnableNotificationsResult {
        object Success : EnableNotificationsResult
        object NeedToAskUserForDistributor : EnableNotificationsResult
    }

    suspend fun execute(distributor: String = ""): EnableNotificationsResult {
        val pusherForCurrentSession = pushersManager.getPusherForCurrentSession()
        if (pusherForCurrentSession == null) {
            when (registerUnifiedPushUseCase.execute(distributor)) {
                is RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.NeedToAskUserForDistributor -> {
                    return EnableNotificationsResult.NeedToAskUserForDistributor
                }
                RegisterUnifiedPushUseCase.RegisterUnifiedPushResult.Success -> {
                    ensureFcmTokenIsRetrievedUseCase.execute(pushersManager, registerPusher = true)
                }
            }
        }

        toggleNotificationsForCurrentSessionUseCase.execute(enabled = true)

        return EnableNotificationsResult.Success
    }
}
