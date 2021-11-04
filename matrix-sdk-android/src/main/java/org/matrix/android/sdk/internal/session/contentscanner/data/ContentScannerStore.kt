/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package org.matrix.android.sdk.internal.session.contentscanner.data

import androidx.lifecycle.LiveData
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional

internal interface ContentScannerStore {

    fun getScannerUrl(): String?

    fun setScannerUrl(url: String?)

    fun enableScanning(enabled: Boolean)

    fun isScanEnabled(): Boolean

    fun getScanResult(mxcUrl: String): ScanStatusInfo?
    fun getLiveScanResult(mxcUrl: String): LiveData<Optional<ScanStatusInfo>>
    fun isScanResultKnownOrInProgress(mxcUrl: String, scannerUrl: String?): Boolean

    fun updateStateForContent(mxcUrl: String, state: ScanState, scannerUrl: String?)
    fun updateScanResultForContent(mxcUrl: String, scannerUrl: String?, state: ScanState, humanReadable: String)
}
