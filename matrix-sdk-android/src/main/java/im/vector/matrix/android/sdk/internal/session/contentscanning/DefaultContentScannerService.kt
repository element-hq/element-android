/*
 * Copyright 2020 New Vector Ltd - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

package im.vector.matrix.android.sdk.internal.session.contentscanning

import androidx.lifecycle.LiveData
import dagger.Lazy
import im.vector.matrix.android.sdk.internal.session.contentscanning.data.ContentScanningStore
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.GetServerPublicKeyTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.ScanEncryptedTask
import im.vector.matrix.android.sdk.internal.session.contentscanning.tasks.ScanMediaTask
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.session.contentscanning.ContentScannerService
import org.matrix.android.sdk.api.session.contentscanning.ScanState
import org.matrix.android.sdk.api.session.contentscanning.ScanStatusInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.launchToCallback
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.awaitCallback
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultContentScannerService @Inject constructor(
        private val retrofitFactory: RetrofitFactory,
        @Unauthenticated
        private val okHttpClient: Lazy<OkHttpClient>,
        private val contentScanningApiProvider: ContentScanningApiProvider,
        private val contentScanningStore: ContentScanningStore,
//        private val sessionParams: SessionParams,
        private val getServerPublicKeyTask: GetServerPublicKeyTask,
        private val scanEncryptedTask: ScanEncryptedTask,
        private val scanMediaTask: ScanMediaTask,
        private val taskExecutor: TaskExecutor,
        private val coroutineDispatchers: MatrixCoroutineDispatchers
) : ContentScannerService {

    // Cache public key in memory
    override var serverPublicKey: String? = null
        private set

    override fun getContentScannerServer(): String? {
        return contentScanningStore.getScannerUrl()
    }

    override fun getServerPublicKey(forceDownload: Boolean, callback: MatrixCallback<String?>) {
        val api = contentScanningApiProvider.contentScannerApi ?: return Unit.also {
            callback.onFailure(IllegalArgumentException("No content scanner defined"))
        }

        if (!forceDownload && serverPublicKey != null) {
            callback.onSuccess(serverPublicKey)
            return
        }
        taskExecutor.executorScope.launchToCallback(coroutineDispatchers.io, callback) {
            getServerPublicKeyTask.execute(GetServerPublicKeyTask.Params(api)).also {
                serverPublicKey = it
            }
        }
    }

    override fun getScanResultForAttachment(mxcUrl: String, fileInfo: ElementToDecrypt, callback: MatrixCallback<ScanStatusInfo>) {
        taskExecutor.executorScope.launchToCallback(coroutineDispatchers.io, callback) {
            val serverPublicKey = serverPublicKey ?: awaitCallback<String?> {
                getServerPublicKey(false, it)
            }

            val result = scanEncryptedTask.execute(ScanEncryptedTask.Params(
                    mxcUrl = mxcUrl,
                    publicServerKey = serverPublicKey,
                    encryptedInfo = fileInfo
            ))

            ScanStatusInfo(
                    state = if (result.clean) ScanState.TRUSTED else ScanState.INFECTED,
                    humanReadableMessage = result.info,
                    scanDateTimestamp = System.currentTimeMillis()
            )
        }
    }

    override fun getScanResultForAttachment(mxcUrl: String, callback: MatrixCallback<ScanStatusInfo>) {
        taskExecutor.executorScope.launchToCallback(coroutineDispatchers.io, callback) {
            val result = scanMediaTask.execute(ScanMediaTask.Params(mxcUrl))

            ScanStatusInfo(
                    state = if (result.clean) ScanState.TRUSTED else ScanState.INFECTED,
                    humanReadableMessage = result.info,
                    scanDateTimestamp = System.currentTimeMillis()
            )
        }
    }

    override fun setScannerUrl(url: String?) = contentScanningStore.setScannerUrl(url).also {
        if (url == null) {
            contentScanningApiProvider.contentScannerApi = null
            serverPublicKey = null
        } else {
            val api = retrofitFactory
                    .create(okHttpClient, url)
                    .create(ContentScanApi::class.java)
            contentScanningApiProvider.contentScannerApi = api

            taskExecutor.executorScope.launch(coroutineDispatchers.io) {
                try {
                    awaitCallback<String?> {
                        getServerPublicKey(true, it)
                    }
                } catch (failure: Throwable) {
                    Timber.e("Failed to get public server api")
                }
            }
        }
    }

    override fun enableScanner(enabled: Boolean) = contentScanningStore.enableScanning(enabled)

    override fun isScannerEnabled(): Boolean = contentScanningStore.isScanEnabled()

    override fun getCachedScanResultForFile(mxcUrl: String): ScanStatusInfo? {
        return contentScanningStore.getScanResult(mxcUrl)
    }

    override fun getLiveStatusForFile(mxcUrl: String, fetchIfNeeded: Boolean): LiveData<Optional<ScanStatusInfo>> {
        val data = contentScanningStore.getLiveScanResult(mxcUrl)
        if (fetchIfNeeded && !contentScanningStore.isScanResultKnownOrInProgress(mxcUrl, getContentScannerServer())) {
            getScanResultForAttachment(mxcUrl, NoOpMatrixCallback())
        }
        return data
    }

    override fun getLiveStatusForEncryptedFile(mxcUrl: String, fileInfo: ElementToDecrypt, fetchIfNeeded: Boolean): LiveData<Optional<ScanStatusInfo>> {
        val data = contentScanningStore.getLiveScanResult(mxcUrl)
        if (fetchIfNeeded && !contentScanningStore.isScanResultKnownOrInProgress(mxcUrl, getContentScannerServer())) {
            getScanResultForAttachment(mxcUrl, fileInfo, NoOpMatrixCallback())
        }
        return data
    }
}
