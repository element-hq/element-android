/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

import org.matrix.android.sdk.api.MatrixUrls.isMxcUrl
import org.matrix.android.sdk.api.MatrixUrls.removeMxcPrefix
import org.matrix.android.sdk.api.failure.toScanFailure
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.contentscanner.ContentScannerApiProvider
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.model.ScanResponse
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface ScanMediaTask : Task<ScanMediaTask.Params, ScanResponse> {
    data class Params(
            val mxcUrl: String
    )
}

internal class DefaultScanMediaTask @Inject constructor(
        private val contentScannerApiProvider: ContentScannerApiProvider,
        private val contentScannerStore: ContentScannerStore
) : ScanMediaTask {

    override suspend fun execute(params: ScanMediaTask.Params): ScanResponse {
        // "mxc://server.org/QNDpzLopkoQYNikJfoZCQuCXJ"
        if (!params.mxcUrl.isMxcUrl()) {
            throw IllegalAccessException("Invalid mxc url")
        }
        val scannerUrl = contentScannerStore.getScannerUrl()
        contentScannerStore.updateStateForContent(params.mxcUrl, ScanState.IN_PROGRESS, scannerUrl)

        var serverAndMediaId = params.mxcUrl.removeMxcPrefix()
        val fragmentOffset = serverAndMediaId.indexOf("#")
        if (fragmentOffset >= 0) {
            serverAndMediaId = serverAndMediaId.substring(0, fragmentOffset)
        }

        val split = serverAndMediaId.split("/")
        if (split.size != 2) {
            throw IllegalAccessException("Invalid mxc url")
        }

        try {
            val scanResponse = executeRequest<ScanResponse>(null) {
                val api = contentScannerApiProvider.contentScannerApi ?: throw IllegalArgumentException()
                api.scanMedia(split[0], split[1])
            }
            contentScannerStore.updateScanResultForContent(
                    params.mxcUrl,
                    scannerUrl,
                    ScanState.TRUSTED.takeIf { scanResponse.clean } ?: ScanState.INFECTED,
                    scanResponse.info ?: ""
            )
            return scanResponse
        } catch (failure: Throwable) {
            contentScannerStore.updateStateForContent(params.mxcUrl, ScanState.UNKNOWN, scannerUrl)
            throw failure.toScanFailure() ?: failure
        }
    }
}
