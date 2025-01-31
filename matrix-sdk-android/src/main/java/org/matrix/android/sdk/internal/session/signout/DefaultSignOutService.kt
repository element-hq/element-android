/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.signout

import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.session.signout.SignOutService
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import javax.inject.Inject

internal class DefaultSignOutService @Inject constructor(
        private val signOutTask: SignOutTask,
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
