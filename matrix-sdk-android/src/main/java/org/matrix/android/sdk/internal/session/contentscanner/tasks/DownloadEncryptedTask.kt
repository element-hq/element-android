/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner.tasks

import okhttp3.ResponseBody
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
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
