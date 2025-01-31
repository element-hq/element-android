/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.account

import org.matrix.android.sdk.api.auth.UserInteractiveAuthInterceptor
import org.matrix.android.sdk.api.session.account.AccountService
import javax.inject.Inject

internal class DefaultAccountService @Inject constructor(
        private val changePasswordTask: ChangePasswordTask,
        private val deactivateAccountTask: DeactivateAccountTask
) : AccountService {

    override suspend fun changePassword(password: String, newPassword: String, logoutAllDevices: Boolean) {
        changePasswordTask.execute(ChangePasswordTask.Params(password, newPassword, logoutAllDevices))
    }

    override suspend fun deactivateAccount(eraseAllData: Boolean, userInteractiveAuthInterceptor: UserInteractiveAuthInterceptor) {
        deactivateAccountTask.execute(DeactivateAccountTask.Params(eraseAllData, userInteractiveAuthInterceptor))
    }
}
