/*
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

import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.account.AccountService
import javax.inject.Inject

internal class DefaultAccountService @Inject constructor(private val changePasswordTask: ChangePasswordTask,
                                                         private val deactivateAccountTask: DeactivateAccountTask) : AccountService {

    override suspend fun changePassword(password: String, newPassword: String) {
        changePasswordTask.execute(ChangePasswordTask.Params(password, newPassword))
    }

    override suspend fun deactivateAccount(eraseAllData: Boolean, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) {
        deactivateAccountTask.execute(DeactivateAccountTask.Params(eraseAllData, userInteractiveAuthInterceptor))
    }
}
