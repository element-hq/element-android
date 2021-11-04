/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.api.session.contentscanner

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt

interface ContentScannerService {

    val serverPublicKey: String?

    fun getContentScannerServer(): String?
    /**
     * Get the current public curve25519 key that the AV server is advertising.
     * @param callback on success callback containing the server public key
     */
    fun getServerPublicKey(forceDownload: Boolean = false, callback: MatrixCallback<String?>)

    fun getScanResultForAttachment(mxcUrl: String, callback: MatrixCallback<ScanStatusInfo>)
    fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt, callback: MatrixCallback<ScanStatusInfo>)

    fun setScannerUrl(url: String?)

    fun enableScanner(enabled: Boolean)
    fun isScannerEnabled(): Boolean
    fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean = true): LiveData<Optional<ScanStatusInfo>>
    fun getLiveStatusForEncryptedFile(mxcUrl: String, fileInfo: ElementToDecrypt, fetchIfNeeded: Boolean = true): LiveData<Optional<ScanStatusInfo>>
    fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo?
}
