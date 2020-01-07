/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.auth.registration

import im.vector.matrix.android.internal.auth.AuthAPI
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task

internal interface ValidateCodeTask : Task<ValidateCodeTask.Params, SuccessResult> {
    data class Params(
            val url: String,
            val body: ValidationCodeBody
    )
}

internal class DefaultValidateCodeTask(
        private val authAPI: AuthAPI
) : ValidateCodeTask {

    override suspend fun execute(params: ValidateCodeTask.Params): SuccessResult {
        return executeRequest(null) {
            apiCall = authAPI.validate3Pid(params.url, params.body)
        }
    }
}
