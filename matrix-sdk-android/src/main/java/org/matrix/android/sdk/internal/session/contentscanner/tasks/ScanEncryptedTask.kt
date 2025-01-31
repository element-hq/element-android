/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
