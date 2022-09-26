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

package im.vector.app.features.settings.devices.v2.verification

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.firstOrNull
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.flow.flow
import timber.log.Timber
import javax.inject.Inject

class CheckIfCurrentSessionCanBeVerifiedUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    suspend fun execute(): Boolean {
        val session = activeSessionHolder.getSafeActiveSession()
        val cryptoSessionsCount = session?.flow()
                ?.liveUserCryptoDevices(session.myUserId)
                ?.firstOrNull()
                ?.size
                ?: 0
        val hasOtherSessions = cryptoSessionsCount > 1

        val isRecoverySetup = session
                ?.sharedSecretStorageService()
                ?.isRecoverySetup()
                .orFalse()

        Timber.d("hasOtherSessions=$hasOtherSessions (otherSessionsCount=$cryptoSessionsCount), isRecoverySetup=$isRecoverySetup")
        return hasOtherSessions || isRecoverySetup
    }
}
