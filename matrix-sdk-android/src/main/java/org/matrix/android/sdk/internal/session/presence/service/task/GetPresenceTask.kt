/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 *
 */

package org.matrix.android.sdk.internal.session.presence.service.task

import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.presence.PresenceAPI
import org.matrix.android.sdk.internal.session.presence.model.GetPresenceResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal abstract class GetPresenceTask : Task<GetPresenceTask.Params, GetPresenceResponse> {
    data class Params(
            val userId: String
    )
}

internal class DefaultGetPresenceTask @Inject constructor(
        private val presenceAPI: PresenceAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetPresenceTask() {
    override suspend fun execute(params: Params): GetPresenceResponse {
        return executeRequest(globalErrorReceiver) {
            presenceAPI.getPresence(params.userId)
        }
    }
}
