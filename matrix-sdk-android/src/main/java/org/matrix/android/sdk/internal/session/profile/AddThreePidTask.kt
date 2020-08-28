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
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import java.util.UUID
import javax.inject.Inject

internal abstract class AddThreePidTask : Task<AddThreePidTask.Params, Unit> {
    data class Params(
            val threePid: ThreePid
    )
}

internal class DefaultAddThreePidTask @Inject constructor(
        private val profileAPI: ProfileAPI,
        @SessionDatabase private val monarchy: Monarchy,
        private val pendingThreePidMapper: PendingThreePidMapper,
        private val eventBus: EventBus) : AddThreePidTask() {

    override suspend fun execute(params: Params) {
        val clientSecret = UUID.randomUUID().toString()
        val sendAttempt = 1
        val result = when (params.threePid) {
            is ThreePid.Email ->
                executeRequest<AddThreePidResponse>(eventBus) {
                    val body = AddEmailBody(
                            email = params.threePid.email,
                            sendAttempt = sendAttempt,
                            clientSecret = clientSecret
                    )
                    apiCall = profileAPI.addEmail(body)
                }
            is ThreePid.Msisdn -> TODO()
        }

        // Store as a pending three pid
        monarchy.awaitTransaction { realm ->
            PendingThreePid(
                    threePid = params.threePid,
                    clientSecret = clientSecret,
                    sendAttempt = sendAttempt,
                    sid = result.sid
            )
                    .let { pendingThreePidMapper.map(it) }
                    .let { realm.copyToRealm(it) }
        }
    }
}
