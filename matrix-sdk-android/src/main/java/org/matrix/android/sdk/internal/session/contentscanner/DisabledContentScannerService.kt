/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.contentscanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

/**
 * Created to by-pass ProfileTask execution in LoginWizard.
 */
@SessionScope
internal class DisabledContentScannerService @Inject constructor() : ContentScannerService {

    override val serverPublicKey: String?
        get() = null

    override fun getContentScannerServer(): String? {
        return null
    }

    override suspend fun getServerPublicKey(forceDownload: Boolean): String? {
        return null
    }

    override suspend fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt?): ScanStatusInfo {
        TODO("Not yet implemented")
    }

    override fun setScannerUrl(url: String?) {
    }

    override fun enableScanner(enabled: Boolean) {
    }

    override fun isScannerEnabled(): Boolean {
        return false
    }

    override fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean, fileInfo: ElementToDecrypt?): LiveData<Optional<ScanStatusInfo>> {
        return MutableLiveData()
    }

    override fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo? {
        return null
    }
}
