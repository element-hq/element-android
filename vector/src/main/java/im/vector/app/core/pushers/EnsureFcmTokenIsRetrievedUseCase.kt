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

        currentPushers.none {
            // Check the push key because device ID may not be supported by the home server
            it.pushKey == unifiedPushHelper.getEndpointOrToken() &&

                    // TODO:
                    //  Is the home server guaranteed to store and return the data dictionary?
                    //  If not, this check can never succeed.
                     it.data.remoteWipeNonce != null
        }
    } else {
        false
    }
}
