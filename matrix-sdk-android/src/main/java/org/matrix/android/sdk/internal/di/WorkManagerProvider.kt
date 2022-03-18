/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.di

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class WorkManagerProvider @Inject constructor(
        context: Context,
        @SessionId private val sessionId: String,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val sessionScope: CoroutineScope
) {
    private val tag = MATRIX_SDK_TAG_PREFIX + sessionId

    val workManager = WorkManager.getInstance(context)

    init {
        checkIfWorkerFactoryIsSetup()
    }

    /**
     * Create a OneTimeWorkRequestBuilder, with the Matrix SDK tag
     */
    inline fun <reified W : ListenableWorker> matrixOneTimeWorkRequestBuilder() =
            OneTimeWorkRequestBuilder<W>()
                    .addTag(tag)

    /**
     * Create a PeriodicWorkRequestBuilder, with the Matrix SDK tag
     */
    inline fun <reified W : ListenableWorker> matrixPeriodicWorkRequestBuilder(repeatInterval: Long,
                                                                               repeatIntervalTimeUnit: TimeUnit) =
            PeriodicWorkRequestBuilder<W>(repeatInterval, repeatIntervalTimeUnit)
                    .addTag(tag)

    /**
     * Cancel all works instantiated by the Matrix SDK for the current session, and not those from the SDK client, or for other sessions
     */
    fun cancelAllWorks() {
        workManager.let {
            it.cancelAllWorkByTag(tag)
            it.pruneWork()
        }
    }

    private fun checkIfWorkerFactoryIsSetup() {
        sessionScope.launch(coroutineDispatchers.main) {
            val checkWorkerRequest = OneTimeWorkRequestBuilder<MatrixWorkerFactory.CheckFactoryWorker>().build()
            workManager.enqueue(checkWorkerRequest)
            val checkWorkerLiveState = workManager.getWorkInfoByIdLiveData(checkWorkerRequest.id)
            val observer = object : Observer<WorkInfo> {
                override fun onChanged(workInfo: WorkInfo?) {
                    if (workInfo?.state?.isFinished == true) {
                        checkWorkerLiveState.removeObserver(this)
                        if (workInfo.state == WorkInfo.State.FAILED) {
                            throw RuntimeException("MatrixWorkerFactory is not being set on your worker configuration.\n" +
                                    "Makes sure to add it to a DelegatingWorkerFactory if you have your own factory or use it directly.\n" +
                                    "You can grab the instance through the Matrix class.")
                        }
                    }
                }
            }
            checkWorkerLiveState.observeForever(observer)
        }
    }

    companion object {
        private const val MATRIX_SDK_TAG_PREFIX = "MatrixSDK-"

        /**
         * Default constraints: connected network
         */
        val workConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        // Use min value, smaller value will be ignored
        const val BACKOFF_DELAY_MILLIS = WorkRequest.MIN_BACKOFF_MILLIS
    }
}
