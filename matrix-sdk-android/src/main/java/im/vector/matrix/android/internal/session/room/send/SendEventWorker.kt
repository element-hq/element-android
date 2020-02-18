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
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

internal class SendEventWorker(context: Context,
                               params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val event: Event,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var localEchoUpdater: LocalEchoUpdater
    @Inject lateinit var roomAPI: RoomAPI
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success().also {
                    Timber.e("Work cancelled due to input error from parent")
                }

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
            sendEvent(event)
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

    private suspend fun sendEvent(event: Event) {
        localEchoUpdater.updateSendState(event.eventId!!, SendState.SENDING)
        executeRequest<SendResponse>(eventBus) {
            apiCall = roomAPI.send(
                    event.eventId,
                    event.roomId!!,
                    event.type,
                    event.content
            )
        }
        localEchoUpdater.updateSendState(event.eventId, SendState.SENT)
    }
}
