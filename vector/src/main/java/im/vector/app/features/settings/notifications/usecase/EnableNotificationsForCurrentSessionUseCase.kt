/*
 * Copyright (c) 2023 New Vector Ltd
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
