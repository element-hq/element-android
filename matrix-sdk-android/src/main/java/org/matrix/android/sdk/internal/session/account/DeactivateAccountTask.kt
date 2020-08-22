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

import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.cleanup.CleanupSession
import org.matrix.android.sdk.internal.session.identity.IdentityDisconnectTask
import org.matrix.android.sdk.internal.task.Task
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal interface DeactivateAccountTask : Task<DeactivateAccountTask.Params, Unit> {
    data class Params(
            val password: String,
            val eraseAllData: Boolean
    )
}

internal class DefaultDeactivateAccountTask @Inject constructor(
        private val accountAPI: AccountAPI,
        private val eventBus: EventBus,
        @UserId private val userId: String,
        private val identityDisconnectTask: IdentityDisconnectTask,
        private val cleanupSession: CleanupSession
) : DeactivateAccountTask {

    override suspend fun execute(params: DeactivateAccountTask.Params) {
        val deactivateAccountParams = DeactivateAccountParams.create(userId, params.password, params.eraseAllData)

        executeRequest<Unit>(eventBus) {
            apiCall = accountAPI.deactivate(deactivateAccountParams)
        }

        // Logout from identity server if any, ignoring errors
        runCatching { identityDisconnectTask.execute(Unit) }
                .onFailure { Timber.w(it, "Unable to disconnect identity server") }

        cleanupSession.handle()
    }
}
