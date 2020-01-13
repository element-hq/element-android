/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.profile

import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import javax.inject.Inject

internal abstract class GetProfileInfoTask : Task<GetProfileInfoTask.Params, JsonDict> {
    data class Params(
            val userId: String
    )
}

internal class DefaultGetProfileInfoTask @Inject constructor(private val profileAPI: ProfileAPI) : GetProfileInfoTask() {

    override suspend fun execute(params: Params): JsonDict {
        return executeRequest {
            apiCall = profileAPI.getProfile(params.userId)
        }
    }
}


