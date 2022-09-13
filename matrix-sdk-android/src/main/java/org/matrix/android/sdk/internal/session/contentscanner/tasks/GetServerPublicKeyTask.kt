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
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.contentscanner.ContentScannerApi
import org.matrix.android.sdk.internal.session.contentscanner.model.ServerPublicKeyResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface GetServerPublicKeyTask : Task<GetServerPublicKeyTask.Params, String?> {
    data class Params(
            val contentScannerApi: ContentScannerApi
    )
}

internal class DefaultGetServerPublicKeyTask @Inject constructor() : GetServerPublicKeyTask {

    override suspend fun execute(params: GetServerPublicKeyTask.Params): String? {
        return executeRequest<ServerPublicKeyResponse>(null) {
            params.contentScannerApi.getServerPublicKey()
        }.publicKey
    }
}
