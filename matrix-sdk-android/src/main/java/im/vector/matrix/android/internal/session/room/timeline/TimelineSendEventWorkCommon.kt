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

import androidx.work.*
import java.util.concurrent.TimeUnit


private const val SEND_WORK = "SEND_WORK"
private const val BACKOFF_DELAY = 10_000L

private val WORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

/**
 * Helper class for sending event related works.
 * All send event from a room are using the same workchain, in order to ensure order.
 * WorkRequest must always return success (even if server error, in this case marking the event as failed to send)
 * , if not the chain will be doomed in failed state.
 *
 */
internal object TimelineSendEventWorkCommon {

    fun postSequentialWorks(roomId: String, vararg workRequests: OneTimeWorkRequest) {
        when {
            workRequests.isEmpty() -> return
            workRequests.size == 1 -> postWork(roomId, workRequests.first())
            else                   -> {
                val firstWork = workRequests.first()
                var continuation = WorkManager.getInstance()
                        .beginUniqueWork(buildWorkIdentifier(roomId), ExistingWorkPolicy.APPEND, firstWork)
                for (i in 1 until workRequests.size) {
                    val workRequest = workRequests[i]
                    continuation = continuation.then(workRequest)
                }
                continuation.enqueue()
            }
        }
    }

    fun postWork(roomId: String, workRequest: OneTimeWorkRequest) {
        WorkManager.getInstance()
                .beginUniqueWork(buildWorkIdentifier(roomId), ExistingWorkPolicy.APPEND, workRequest)
                .enqueue()
    }

    inline fun <reified W : ListenableWorker> createWork(data: Data): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<W>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun buildWorkIdentifier(roomId: String): String {
        return "${roomId}_$SEND_WORK"
    }
}