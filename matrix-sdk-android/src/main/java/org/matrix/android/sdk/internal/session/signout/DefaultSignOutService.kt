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

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.session.signout.SignOutService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import javax.inject.Inject

internal class DefaultSignOutService @Inject constructor(private val signOutTask: SignOutTask,
                                                         private val signInAgainTask: SignInAgainTask,
                                                         private val sessionParamsStore: SessionParamsStore,
                                                         private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                         private val taskExecutor: TaskExecutor) : SignOutService {

    override fun signInAgain(password: String,
                             callback: MatrixCallback<Unit>): Cancelable {
        return signInAgainTask
                .configureWith(SignInAgainTask.Params(password)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun updateCredentials(credentials: Credentials,
                                   callback: MatrixCallback<Unit>): Cancelable {
        return taskExecutor.executorScope.launchToCallback(coroutineDispatchers.main, callback) {
            sessionParamsStore.updateCredentials(credentials)
        }
    }

    override fun signOut(signOutFromHomeserver: Boolean,
                         callback: MatrixCallback<Unit>): Cancelable {
        return signOutTask
                .configureWith(SignOutTask.Params(signOutFromHomeserver)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
