/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

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
        if (!params.mxcUrl.startsWith("mxc://")) {
            throw IllegalAccessException("Invalid mxc url")
        }
        val scannerUrl = contentScannerStore.getScannerUrl()
        contentScannerStore.updateStateForContent(params.mxcUrl, ScanState.IN_PROGRESS, scannerUrl)

        var serverAndMediaId = params.mxcUrl.removePrefix("mxc://")
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
