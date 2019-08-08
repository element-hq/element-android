/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.signout

import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal interface SignOutTask : Task<Unit, Unit>

internal class DefaultSignOutTask @Inject constructor(private val credentials: Credentials,
                                                      private val signOutAPI: SignOutAPI,
                                                      private val sessionManager: SessionManager,
                                                      private val sessionParamsStore: SessionParamsStore) : SignOutTask {

    override suspend fun execute(params: Unit) {
        executeRequest<Unit> {
            apiCall = signOutAPI.signOut()
        }
        sessionParamsStore.delete(credentials.userId)
        sessionManager.releaseSession(credentials.userId)
    }
}