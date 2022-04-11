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
     * @param callback on success callback containing the server public key
     */
    suspend fun getServerPublicKey(forceDownload: Boolean = false): String?
    suspend fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt? = null): ScanStatusInfo
}
