/*
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
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionComponent
import org.matrix.android.sdk.internal.session.content.UploadContentWorker
import org.matrix.android.sdk.internal.session.room.timeline.TimelineSendEventWorkCommon
import org.matrix.android.sdk.internal.worker.SessionSafeCoroutineWorker
import org.matrix.android.sdk.internal.worker.SessionWorkerParams
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber
import javax.inject.Inject

/**
 * This worker creates a new work for each events passed in parameter
 *
 * Possible previous worker: Always [UploadContentWorker]
 * Possible next worker    : None, but it will post new work to send events, encrypted or not
 */
internal class MultipleEventSendingDispatcherWorker(context: Context, params: WorkerParameters, sessionManager: SessionManager) :
    SessionSafeCoroutineWorker<MultipleEventSendingDispatcherWorker.Params>(context, params, sessionManager, Params::class.java) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val localEchoIds: List<LocalEchoIdentifiers>,
            val isEncrypted: Boolean,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var workManagerProvider: WorkManagerProvider
    @Inject lateinit var timelineSendEventWorkCommon: TimelineSendEventWorkCommon
    @Inject lateinit var localEchoRepository: LocalEchoRepository

    override fun doOnError(params: Params): Result {
        params.localEchoIds.forEach { localEchoIds ->
            localEchoRepository.updateSendState(
                    eventId = localEchoIds.eventId,
                    roomId = localEchoIds.roomId,
                    sendState = SendState.UNDELIVERED,
                    sendStateDetails = params.lastFailureMessage
            )
        }

        return super.doOnError(params)
    }

    override fun injectWith(injector: SessionComponent) {
        injector.inject(this)
    }

    override suspend fun doSafeWork(params: Params): Result {
        Timber.v("## SendEvent: Start dispatch sending multiple event work")
        // Create a work for every event
        params.localEchoIds.forEach { localEchoIds ->
            val roomId = localEchoIds.roomId
            val eventId = localEchoIds.eventId
                localEchoRepository.updateSendState(eventId, roomId, SendState.SENDING)
                Timber.v("## SendEvent: [${System.currentTimeMillis()}] Schedule send event $eventId")
                val sendWork = createSendEventWork(params.sessionId, eventId, true)
                timelineSendEventWorkCommon.postWork(roomId, sendWork)
        }

        return Result.success()
    }

    override fun buildErrorParams(params: Params, message: String): Params {
        return params.copy(lastFailureMessage = params.lastFailureMessage ?: message)
    }

    private fun createSendEventWork(sessionId: String, eventId: String, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId = sessionId, eventId = eventId)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return timelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }
}
