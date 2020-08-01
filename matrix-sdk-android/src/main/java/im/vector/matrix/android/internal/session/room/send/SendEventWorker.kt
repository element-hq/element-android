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

package im.vector.matrix.android.internal.session.room.send

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.shouldBeRetried
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.database.mapper.ContentMapper
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

private const val MAX_NUMBER_OF_RETRY_BEFORE_FAILING = 3

/**
 * Possible previous worker: [EncryptEventWorker] or first worker
 * Possible next worker    : None
 */
internal class SendEventWorker(context: Context,
                               params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            // TODO remove after some time, it's used for compat
            val event: Event? = null,
            val eventId: String? = null,
            val roomId: String? = null,
            val type: String? = null,
            val contentStr: String? = null,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams {

        constructor(sessionId: String, event: Event, lastFailureMessage: String? = null) : this(
                sessionId = sessionId,
                eventId = event.eventId,
                roomId = event.roomId,
                type = event.type,
                contentStr = ContentMapper.map(event.content),
                lastFailureMessage = lastFailureMessage
        )
    }

    @Inject lateinit var localEchoRepository: LocalEchoRepository
    @Inject lateinit var roomAPI: RoomAPI
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()
                        .also { Timber.e("Unable to parse work parameters") }

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)
        if (params.eventId == null || params.roomId == null || params.type == null) {
            // compat with old params, make it fail if any
            if (params.event?.eventId != null) {
                localEchoRepository.updateSendState(params.event.eventId, SendState.UNDELIVERED)
            }
            return Result.success()
        }
        if (params.lastFailureMessage != null) {
            localEchoRepository.updateSendState(params.eventId, SendState.UNDELIVERED)
            // Transmit the error
            return Result.success(inputData)
                    .also { Timber.e("Work cancelled due to input error from parent") }
        }
        return try {
            sendEvent(params.eventId, params.roomId, params.type, params.contentStr)
            Result.success()
        } catch (exception: Throwable) {
            // It does start from 0, we want it to stop if it fails the third time
            val currentAttemptCount = runAttemptCount + 1
            if (currentAttemptCount >= MAX_NUMBER_OF_RETRY_BEFORE_FAILING || !exception.shouldBeRetried()) {
                localEchoRepository.updateSendState(params.eventId, SendState.UNDELIVERED)
                return Result.success()
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun sendEvent(eventId: String, roomId: String, type: String, contentStr: String?) {
        localEchoRepository.updateSendState(eventId, SendState.SENDING)
        executeRequest<SendResponse>(eventBus) {
            apiCall = roomAPI.send(eventId, roomId, type, contentStr)
        }
        localEchoRepository.updateSendState(eventId, SendState.SENT)
    }
}
