/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.contentscanner

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.util.Optional

interface ContentScannerService {

    val serverPublicKey: String?

    fun getContentScannerServer(): String?
    fun setScannerUrl(url: String?)
    fun enableScanner(enabled: Boolean)
    fun isScannerEnabled(): Boolean
    fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean = true, fileInfo: ElementToDecrypt? = null): LiveData<Optional<ScanStatusInfo>>
    fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo?

    /**
     * Get the current public curve25519 key that the AV server is advertising.
     * @param forceDownload true to force the SDK to download again the server public key
     */
    suspend fun getServerPublicKey(forceDownload: Boolean = false): String?
    suspend fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt? = null): ScanStatusInfo
}
