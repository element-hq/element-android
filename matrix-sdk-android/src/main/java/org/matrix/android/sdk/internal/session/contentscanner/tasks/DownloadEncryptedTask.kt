/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

import okhttp3.ResponseBody
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.contentscanner.ContentScannerApiProvider
import org.matrix.android.sdk.internal.session.contentscanner.ScanEncryptorUtils
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface DownloadEncryptedTask : Task<DownloadEncryptedTask.Params, ResponseBody> {
    data class Params(
            val publicServerKey: String?,
            val encryptedInfo: ElementToDecrypt,
            val mxcUrl: String
    )
}

internal class DefaultDownloadEncryptedTask @Inject constructor(
        private val contentScannerApiProvider: ContentScannerApiProvider
) : DownloadEncryptedTask {

    override suspend fun execute(params: DownloadEncryptedTask.Params): ResponseBody {
        val dlBody = ScanEncryptorUtils.getDownloadBodyAndEncryptIfNeeded(
                params.publicServerKey,
                params.mxcUrl,
                params.encryptedInfo
        )

        val api = contentScannerApiProvider.contentScannerApi ?: throw IllegalArgumentException()
        return executeRequest(null) {
            api.downloadEncrypted(dlBody)
        }
    }
}
