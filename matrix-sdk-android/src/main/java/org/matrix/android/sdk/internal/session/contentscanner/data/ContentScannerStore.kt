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

package org.matrix.android.sdk.internal.session.contentscanner.data

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional

internal interface ContentScannerStore {

    fun getScannerUrl(): String?

    fun setScannerUrl(url: String?)

    fun enableScanner(enabled: Boolean)

    fun isScanEnabled(): Boolean

    fun getScanResult(mxcUrl: String): ScanStatusInfo?
    fun getLiveScanResult(mxcUrl: String): LiveData<Optional<ScanStatusInfo>>
    fun isScanResultKnownOrInProgress(mxcUrl: String, scannerUrl: String?): Boolean

    fun updateStateForContent(mxcUrl: String, state: ScanState, scannerUrl: String?)
    fun updateScanResultForContent(mxcUrl: String, scannerUrl: String?, state: ScanState, humanReadable: String)
}
