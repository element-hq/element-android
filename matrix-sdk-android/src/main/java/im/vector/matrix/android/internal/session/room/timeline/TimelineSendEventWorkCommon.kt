/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.session.room.timeline

import android.content.Context
import androidx.work.*
import im.vector.matrix.android.internal.session.room.send.NoMerger
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import im.vector.matrix.android.internal.worker.WorkManagerUtil.matrixOneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit


private const val SEND_WORK = "SEND_WORK"
private const val BACKOFF_DELAY = 10_000L

/**
 * Helper class for sending event related works.
 * All send event from a room are using the same workchain, in order to ensure order.
 * WorkRequest must always return success (even if server error, in this case marking the event as failed to send)
 * , if not the chain will be doomed in failed state.
 *
 */
internal object TimelineSendEventWorkCommon {

    fun postSequentialWorks(context: Context, roomId: String, vararg workRequests: OneTimeWorkRequest) {
        when {
            workRequests.isEmpty() -> return
            workRequests.size == 1 -> postWork(context, roomId, workRequests.first())
            else                   -> {
                val firstWork = workRequests.first()
                var continuation = WorkManager.getInstance(context)
                        .beginUniqueWork(buildWorkName(roomId), ExistingWorkPolicy.APPEND, firstWork)
                for (i in 1 until workRequests.size) {
                    val workRequest = workRequests[i]
                    continuation = continuation.then(workRequest)
                }
                continuation.enqueue()
            }
        }
    }

    fun postWork(context: Context, roomId: String, workRequest: OneTimeWorkRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND) {
        WorkManager.getInstance(context)
                .beginUniqueWork(buildWorkName(roomId), policy, workRequest)
                .enqueue()
    }

    inline fun <reified W : ListenableWorker> createWork(data: Data, startChain: Boolean): OneTimeWorkRequest {
        return matrixOneTimeWorkRequestBuilder<W>()
                .setConstraints(WorkManagerUtil.workConstraints)
                .apply {
                    if (startChain) {
                        setInputMerger(NoMerger::class.java)
                    }
                }
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun buildWorkName(roomId: String): String {
        return "${roomId}_$SEND_WORK"
    }

    fun cancelAllWorks(context: Context, roomId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(buildWorkName(roomId))
    }
}