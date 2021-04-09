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

package org.matrix.android.sdk.internal.session.thirdparty

import org.matrix.android.sdk.api.session.thirdparty.model.ThirdPartyUser
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetThirdPartyUserTask : Task<GetThirdPartyUserTask.Params, List<ThirdPartyUser>> {

    data class Params(
            val protocol: String,
            val fields: Map<String, String> = emptyMap()
    )
}

internal class DefaultGetThirdPartyUserTask @Inject constructor(
        private val thirdPartyAPI: ThirdPartyAPI,
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetThirdPartyUserTask {

    override suspend fun execute(params: GetThirdPartyUserTask.Params): List<ThirdPartyUser> {
        return executeRequest(globalErrorReceiver) {
            thirdPartyAPI.getThirdPartyUser(params.protocol, params.fields)
        }
    }
}
