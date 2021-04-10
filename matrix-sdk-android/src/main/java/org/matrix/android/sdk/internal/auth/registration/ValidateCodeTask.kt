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

package org.matrix.android.sdk.internal.auth.registration

import org.matrix.android.sdk.internal.auth.AuthAPI
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task

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
            authAPI.validate3Pid(params.url, params.body)
        }
    }
}
