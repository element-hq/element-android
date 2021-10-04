/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.signout

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.session.signout.SignOutService
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import javax.inject.Inject

internal class DefaultSignOutService @Inject constructor(private val signOutTask: SignOutTask,
                                                         private val signInAgainTask: SignInAgainTask,
                                                         private val sessionParamsStore: SessionParamsStore
) : SignOutService {

    override suspend fun signInAgain(password: String) {
        signInAgainTask.execute(SignInAgainTask.Params(password))
    }

    override suspend fun updateCredentials(credentials: Credentials) {
        sessionParamsStore.updateCredentials(credentials)
    }

    override suspend fun signOut(signOutFromHomeserver: Boolean) {
        return signOutTask.execute(SignOutTask.Params(signOutFromHomeserver))
    }
}
