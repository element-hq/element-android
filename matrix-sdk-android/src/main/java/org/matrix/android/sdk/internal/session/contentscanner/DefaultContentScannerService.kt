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

package org.matrix.android.sdk.internal.session.contentscanner

import androidx.lifecycle.LiveData
import dagger.Lazy
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.contentscanner.ContentScannerService
import org.matrix.android.sdk.api.session.contentscanner.ScanState
import org.matrix.android.sdk.api.session.contentscanner.ScanStatusInfo
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.contentscanner.data.ContentScannerStore
import org.matrix.android.sdk.internal.session.contentscanner.tasks.GetServerPublicKeyTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.ScanEncryptedTask
import org.matrix.android.sdk.internal.session.contentscanner.tasks.ScanMediaTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultContentScannerService @Inject constructor(
        private val retrofitFactory: RetrofitFactory,
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val contentScannerApiProvider: ContentScannerApiProvider,
        private val contentScannerStore: ContentScannerStore,
        private val getServerPublicKeyTask: GetServerPublicKeyTask,
        private val scanEncryptedTask: ScanEncryptedTask,
        private val scanMediaTask: ScanMediaTask,
        private val taskExecutor: TaskExecutor
) : ContentScannerService {

    // Cache public key in memory
    override var serverPublicKey: String? = null
        private set

    override fun getContentScannerServer(): String? {
        return contentScannerStore.getScannerUrl()
    }

    override suspend fun getServerPublicKey(forceDownload: Boolean): String? {
        val api = contentScannerApiProvider.contentScannerApi ?: throw IllegalArgumentException("No content scanner define")

        if (!forceDownload && serverPublicKey != null) {
            return serverPublicKey
        }

        return getServerPublicKeyTask.execute(GetServerPublicKeyTask.Params(api)).also {
            serverPublicKey = it
        }
    }

    override suspend fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt?): ScanStatusInfo {
        val result = if (fileInfo != null) {
            scanEncryptedTask.execute(ScanEncryptedTask.Params(
                    mxcUrl = mxcUrl,
                    publicServerKey = getServerPublicKey(false),
                    encryptedInfo = fileInfo
            ))
        } else {
            scanMediaTask.execute(ScanMediaTask.Params(mxcUrl))
        }

        return ScanStatusInfo(
                state = if (result.clean) ScanState.TRUSTED else ScanState.INFECTED,
                humanReadableMessage = result.info,
                scanDateTimestamp = System.currentTimeMillis()
        )
    }

    override fun setScannerUrl(url: String?) = contentScannerStore.setScannerUrl(url).also {
        if (url == null) {
            contentScannerApiProvider.contentScannerApi = null
            serverPublicKey = null
        } else {
            val api = retrofitFactory
                    .create(okHttpClient, url)
                    .create(ContentScannerApi::class.java)
            contentScannerApiProvider.contentScannerApi = api

            taskExecutor.executorScope.launch {
                try {
                    getServerPublicKey(true)
                } catch (failure: Throwable) {
                    Timber.e("Failed to get public server api")
                }
            }
        }
    }

    override fun enableScanner(enabled: Boolean) = contentScannerStore.enableScanner(enabled)

    override fun isScannerEnabled(): Boolean = contentScannerStore.isScanEnabled()

    override fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo? {
        return contentScannerStore.getScanResult(mxcUrl)
    }

    override fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean, fileInfo: ElementToDecrypt?): LiveData<Optional<ScanStatusInfo>> {
        val data = contentScannerStore.getLiveScanResult(mxcUrl)
        if (fetchIfNeeded && !contentScannerStore.isScanResultKnownOrInProgress(mxcUrl, getContentScannerServer())) {
            taskExecutor.executorScope.launch {
                try {
                    getScanResultForAttachment(mxcUrl, fileInfo)
                } catch (failure: Throwable) {
                    Timber.e("Failed to get file status : ${failure.localizedMessage}")
                }
            }
        }
        return data
    }
}
