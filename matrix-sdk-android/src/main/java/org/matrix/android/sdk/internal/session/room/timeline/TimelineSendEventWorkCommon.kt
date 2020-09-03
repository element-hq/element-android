/*
 * Copyright 2019 New Vector Ltd
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
package org.matrix.android.sdk.internal.session.room.timeline

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.util.CancelableWork
import org.matrix.android.sdk.internal.worker.startChain
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Helper class for sending event related works.
 * All send event from a room are using the same workchain, in order to ensure order.
 * WorkRequest must always return success (even if server error, in this case marking the event as failed to send),
 * if not the chain will be doomed in failed state.
 */
internal class TimelineSendEventWorkCommon @Inject constructor(
        private val workManagerProvider: WorkManagerProvider
) {

    fun postSequentialWorks(roomId: String, vararg workRequests: OneTimeWorkRequest): Cancelable {
        return when {
            workRequests.isEmpty() -> NoOpCancellable
            workRequests.size == 1 -> postWork(roomId, workRequests.first())
            else                   -> {
                val firstWork = workRequests.first()
                var continuation = workManagerProvider.workManager
                        .beginUniqueWork(buildWorkName(roomId), ExistingWorkPolicy.APPEND, firstWork)
                for (i in 1 until workRequests.size) {
                    val workRequest = workRequests[i]
                    continuation = continuation.then(workRequest)
                }
                continuation.enqueue()
                CancelableWork(workManagerProvider.workManager, firstWork.id)
            }
        }
    }

    fun postWork(roomId: String, workRequest: OneTimeWorkRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE): Cancelable {
        workManagerProvider.workManager
                .beginUniqueWork(buildWorkName(roomId), policy, workRequest)
                .enqueue()

        return CancelableWork(workManagerProvider.workManager, workRequest.id)
    }

    inline fun <reified W : ListenableWorker> createWork(data: Data, startChain: Boolean): OneTimeWorkRequest {
        return workManagerProvider.matrixOneTimeWorkRequestBuilder<W>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(startChain)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun buildWorkName(roomId: String): String {
        return "${roomId}_$SEND_WORK"
    }

    fun cancelAllWorks(roomId: String) {
        workManagerProvider.workManager
                .cancelUniqueWork(buildWorkName(roomId))
    }

    companion object {
        private const val SEND_WORK = "SEND_WORK"
        private const val BACKOFF_DELAY = 10_000L
    }
}
