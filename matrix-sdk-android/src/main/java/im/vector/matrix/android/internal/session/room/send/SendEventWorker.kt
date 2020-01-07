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
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.session.events.model.Content
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal class SendEventWorker constructor(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val roomId: String,
            val event: Event,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var localEchoUpdater: LocalEchoUpdater
    @Inject lateinit var roomAPI: RoomAPI
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success()

        val sessionComponent = getSessionComponent(params.sessionId) ?: return Result.success()
        sessionComponent.inject(this)

        val event = params.event
        if (event.eventId == null) {
            return Result.success()
        }

        if (params.lastFailureMessage != null) {
            localEchoUpdater.updateSendState(event.eventId, SendState.UNDELIVERED)
            // Transmit the error
            return Result.success(inputData)
        }
        return try {
            sendEvent(event.eventId, event.type, event.content, params.roomId)
            Result.success()
        } catch (exception: Throwable) {
            if (exception.shouldBeRetried()) {
                Result.retry()
            } else {
                localEchoUpdater.updateSendState(event.eventId, SendState.UNDELIVERED)
                // always return success, or the chain will be stuck for ever!
                Result.success()
            }
        }
    }

    private fun Throwable.shouldBeRetried(): Boolean {
        return this is Failure.NetworkConnection
                || (this is Failure.ServerError && error.code == MatrixError.M_LIMIT_EXCEEDED)
    }

    private suspend fun sendEvent(eventId: String, eventType: String, content: Content?, roomId: String) {
        localEchoUpdater.updateSendState(eventId, SendState.SENDING)
        executeRequest<SendResponse>(eventBus) {
            apiCall = roomAPI.send(
                    eventId,
                    roomId,
                    eventType,
                    content
            )
        }
        localEchoUpdater.updateSendState(eventId, SendState.SENT)
    }
}
