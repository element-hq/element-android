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

package im.vector.app.features.remotewipe

import im.vector.app.core.di.ActiveSessionHolder
import timber.log.Timber
import javax.inject.Inject

class GetRemoteWipeNonceUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {
    fun execute(): String? {
        val session = activeSessionHolder.getSafeActiveSession()

        if(session == null) {
            Timber.e(RemoteWipeException("Could not get remote wipe nonce without valid session"))
            return null
        }

        val deviceId = session.sessionParams.deviceId

        if (deviceId == null) {
            Timber.e(RemoteWipeException("Could not get remote wipe nonce without valid device ID"))
            return null
        }

        val accountDataService = session.accountDataService()
        val accountDataKey = RemoteWipeAccountData.accountDataKey(deviceId)

        return accountDataService
                .getUserAccountDataEvents(setOf(accountDataKey))
                .firstOrNull()
                ?.content
                ?.get(RemoteWipeAccountData.NONCE_CONTENT_KEY)
                as? String

    }
}
