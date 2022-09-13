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

import org.matrix.android.sdk.api.failure.toScanFailure
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.contentscanner.ContentScannerApiProvider
import org.matrix.android.sdk.internal.session.contentscanner.ScanEncryptorUtils
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.model.ScanResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ScanEncryptedTask : Task<ScanEncryptedTask.Params, ScanResponse> {
    data class Params(
            val mxcUrl: String,
            val publicServerKey: String?,
            val encryptedInfo: ElementToDecrypt
    )
}

internal class DefaultScanEncryptedTask @Inject constructor(
        private val contentScannerApiProvider: ContentScannerApiProvider,
        private val contentScannerStore: ContentScannerStore
) : ScanEncryptedTask {

    override suspend fun execute(params: ScanEncryptedTask.Params): ScanResponse {
        val mxcUrl = params.mxcUrl
        val dlBody = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(params.publicServerKey, params.mxcUrl, params.encryptedInfo)

        val scannerUrl = contentScannerStore.getScannerUrl()
        contentScannerStore.updateStateForContent(params.mxcUrl, ScanState.IN_PROGRESS, scannerUrl)

        try {
            val api = contentScannerApiProvider.contentScannerApi ?: throw IllegalArgumentException()
            val executeRequest = executeRequest<ScanResponse>(null) {
                api.scanFile(dlBody)
            }
            contentScannerStore.updateScanResultForContent(
                    mxcUrl,
                    scannerUrl,
                    ScanState.TRUSTED.takeIf { executeRequest.clean } ?: ScanState.INFECTED,
                    executeRequest.info ?: ""
            )
            return executeRequest
        } catch (failure: Throwable) {
            contentScannerStore.updateStateForContent(params.mxcUrl, ScanState.UNKNOWN, scannerUrl)
            throw failure.toScanFailure() ?: failure
        }
    }
}
