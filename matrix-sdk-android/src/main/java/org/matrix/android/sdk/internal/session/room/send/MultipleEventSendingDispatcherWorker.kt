/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.session.room.send

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.content.UploadContentWorker
import org.matrix.android.sdk.internal.session.room.timeline.TimelineSendEventWorkCommon
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.getSessionComponent
import org.matrix.android.sdk.internal.worker.startChain
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * This worker creates a new work for each events passed in parameter
 *
 * Possible previous worker: Always [UploadContentWorker]
 * Possible next worker    : None, but it will post new work to send events, encrypted or not
 */
internal class MultipleEventSendingDispatcherWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val events: List<Event>,
            val isEncrypted: Boolean,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var workManagerProvider: WorkManagerProvider
    @Inject lateinit var timelineSendEventWorkCommon: TimelineSendEventWorkCommon
    @Inject lateinit var localEchoRepository: LocalEchoRepository

    override suspend fun doWork(): Result {
        Timber.v("Start dispatch sending multiple event work")
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        if (params.lastFailureMessage != null) {
            params.events.forEach { event ->
                event.eventId?.let { localEchoRepository.updateSendState(it, SendState.UNDELIVERED) }
            }
            // Transmit the error if needed?
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }

        // Create a work for every event
        params.events.forEach { event ->
            if (params.isEncrypted) {
                Timber.v("Send event in encrypted room")
                val encryptWork = createEncryptEventWork(params.sessionId, event, true)
                // Note that event will be replaced by the result of the previous work
                val sendWork = createSendEventWork(params.sessionId, event, false)
                timelineSendEventWorkCommon.postSequentialWorks(event.roomId!!, encryptWork, sendWork)
            } else {
                val sendWork = createSendEventWork(params.sessionId, event, true)
                timelineSendEventWorkCommon.postWork(event.roomId!!, sendWork)
            }
        }

        return Result.success()
    }

    private fun createEncryptEventWork(sessionId: String, event: Event, startChain: Boolean): OneTimeWorkRequest {
        val params = EncryptEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(params)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<EncryptEventWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(sendWorkData)
                .startChain(startChain)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createSendEventWork(sessionId: String, event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return timelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }
}
