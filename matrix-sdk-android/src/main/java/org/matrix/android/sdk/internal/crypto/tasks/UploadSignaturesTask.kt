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
package org.matrix.android.sdk.internal.crypto.tasks

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.internal.crypto.api.CryptoApi
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface UploadSignaturesTask : Task<UploadSignaturesTask.Params, Unit> {
    data class Params(
            val signatures: Map<String, Map<String, Any>>
    )
}

internal class DefaultUploadSignaturesTask @Inject constructor(
        private val cryptoApi: CryptoApi,
        private val globalErrorReceiver: GlobalErrorReceiver
) : UploadSignaturesTask {

    override suspend fun execute(params: UploadSignaturesTask.Params) {
        try {
            val response = executeRequest(
                    globalErrorReceiver,
                    canRetry = true,
                    maxRetriesCount = 10
            ) {
                cryptoApi.uploadSignatures(params.signatures)
            }
            if (response.failures?.isNotEmpty() == true) {
                throw Throwable(response.failures.toString())
            }
            return
        } catch (f: Failure) {
            throw f
        }
    }
}
