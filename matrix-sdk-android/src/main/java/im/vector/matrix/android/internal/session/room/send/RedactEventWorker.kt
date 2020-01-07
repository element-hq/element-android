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
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal class RedactEventWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val userId: String,
            val txID: String,
            val roomId: String,
            val eventId: String,
            val reason: String?,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject lateinit var roomAPI: RoomAPI
    @Inject lateinit var eventBus: EventBus

    override suspend fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        if (params.lastFailureMessage != null) {
            // Transmit the error
            return Result.success(inputData)
        }

        val sessionComponent = getSessionComponent(params.userId) ?: return Result.success()
        sessionComponent.inject(this)

        val eventId = params.eventId
        return runCatching {
            executeRequest<SendResponse>(eventBus) {
                apiCall = roomAPI.redactEvent(
                        params.txID,
                        params.roomId,
                        eventId,
                        if (params.reason == null) emptyMap() else mapOf("reason" to params.reason)
                )
            }
        }.fold(
                {
                    Result.success()
                },
                {
                    when (it) {
                        is Failure.NetworkConnection -> Result.retry()
                        else                         -> {
                            // TODO mark as failed to send?
                            // always return success, or the chain will be stuck for ever!
                            Result.success(WorkerParamsFactory.toData(params.copy(
                                    lastFailureMessage = it.localizedMessage
                            )))
                        }
                    }
                }
        )
    }
}
