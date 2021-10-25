/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning.tasks

import im.vector.matrix.android.sdk.internal.session.contentscanning.ContentScanningApiProvider
import im.vector.matrix.android.sdk.internal.session.contentscanning.ScanEncryptorUtils
import im.vector.matrix.android.sdk.internal.session.contentscanning.data.ContentScanningStore
import im.vector.matrix.android.sdk.internal.session.contentscanning.model.ScanResponse
import org.matrix.android.sdk.api.failure.toScanFailure
import org.matrix.android.sdk.api.session.contentscanning.ScanState
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.network.executeRequest
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
        private val contentScanningApiProvider: ContentScanningApiProvider,
        private val contentScanningStore: ContentScanningStore
) : ScanEncryptedTask {

    override suspend fun execute(params: ScanEncryptedTask.Params): ScanResponse {
        val mxcUrl = params.mxcUrl
        val dlBody = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(params.publicServerKey, params.mxcUrl, params.encryptedInfo)

        val scannerUrl = contentScanningStore.getScannerUrl()
        contentScanningStore.updateStateForContent(params.mxcUrl, ScanState.IN_PROGRESS, scannerUrl)

        try {
            val api = contentScanningApiProvider.contentScannerApi ?: throw IllegalArgumentException()
            val executeRequest = executeRequest<ScanResponse>(null) {
                api.scanFile(dlBody)
            }
            contentScanningStore.updateScanResultForContent(
                    mxcUrl,
                    scannerUrl,
                    ScanState.TRUSTED.takeIf { executeRequest.clean } ?: ScanState.INFECTED,
                    executeRequest.info ?: ""
            )
            return executeRequest
        } catch (failure: Throwable) {
            contentScanningStore.updateStateForContent(params.mxcUrl, ScanState.UNKNOWN, scannerUrl)
            throw failure.toScanFailure() ?: failure
        }
    }
}
