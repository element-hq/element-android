/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.account

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.account.AccountService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import javax.inject.Inject

internal class DefaultAccountService @Inject constructor(private val changePasswordTask: ChangePasswordTask,
                                                         private val deactivateAccountTask: DeactivateAccountTask,
                                                         private val taskExecutor: TaskExecutor) : AccountService {

    override fun changePassword(password: String, newPassword: String, callback: MatrixCallback<Unit>): Cancelable {
        return changePasswordTask
                .configureWith(ChangePasswordTask.Params(password, newPassword)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun deactivateAccount(password: String, eraseAllData: Boolean, callback: MatrixCallback<Unit>): Cancelable {
        return deactivateAccountTask
                .configureWith(DeactivateAccountTask.Params(password, eraseAllData)) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }
}
