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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import javax.inject.Inject

internal class DefaultSignOutService @Inject constructor(private val signOutTask: SignOutTask,
                                                         private val taskExecutor: TaskExecutor) : SignOutService {

    override fun signOut(callback: MatrixCallback<Unit>) {
        signOutTask
                .configureWith {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

}