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

package org.matrix.android.sdk.internal.session.profile

import com.zhuinden.monarchy.Monarchy
import org.greenrobot.eventbus.EventBus
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.toRegistrationFlowResponse
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.crypto.model.rest.UserPasswordAuth
import org.matrix.android.sdk.internal.database.model.PendingThreePidEntity
import org.matrix.android.sdk.internal.database.model.PendingThreePidEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import javax.inject.Inject

internal abstract class FinalizeAddingThreePidTask : Task<FinalizeAddingThreePidTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid,
            val session: String?,
            val accountPassword: String?,
            val userWantsToCancel: Boolean
    )
}

internal class DefaultFinalizeAddingThreePidTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val pendingThreePidMapper: PendingThreePidMapper,
        @UserId private val userId: String,
        private val eventBus: EventBus) : FinalizeAddingThreePidTask() {

    override suspend fun execute(params: Params) {
        if (params.userWantsToCancel.not()) {
            // Get the required pending data
            val pendingThreePids = monarchy.fetchAllMappedSync(
                    { it.where(PendingThreePidEntity::class.java) },
                    { pendingThreePidMapper.map(it) }
            )
                    .firstOrNull { it.threePid == params.threePid }
                    ?: throw IllegalArgumentException("unknown threepid")

            try {
                executeRequest<Unit>(eventBus) {
                    val body = FinalizeAddThreePidBody(
                            clientSecret = pendingThreePids.clientSecret,
                            sid = pendingThreePids.sid,
                            auth = if (params.session != null && params.accountPassword != null) {
                                UserPasswordAuth(
                                        session = params.session,
                                        user = userId,
                                        password = params.accountPassword
                                )
                            } else null
                    )
                    apiCall = profileAPI.finalizeAddThreePid(body)
                }
            } catch (throwable: Throwable) {
                throw throwable.toRegistrationFlowResponse()
                        ?.let { Failure.RegistrationFlowError(it) }
                        ?: throwable
            }
        }

        cleanupDatabase(params)
    }

    private suspend fun cleanupDatabase(params: Params) {
        // Delete the pending three pid
        monarchy.awaitTransaction { realm ->
            realm.where(PendingThreePidEntity::class.java)
                    .equalTo(PendingThreePidEntityFields.EMAIL, params.threePid.value)
                    .or()
                    .equalTo(PendingThreePidEntityFields.MSISDN, params.threePid.value)
                    .findAll()
                    .deleteAllFromRealm()
        }
    }
}
