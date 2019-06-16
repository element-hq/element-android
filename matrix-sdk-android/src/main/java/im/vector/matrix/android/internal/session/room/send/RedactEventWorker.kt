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
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.DelegateWorkerFactory

internal class RedactEventWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val roomAPI: RoomAPI)
    : Worker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            val txID: String,
            val roomId: String,
            val eventId: String,
            val reason: String?
    )

    override fun doWork(): Result {
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.failure()

        val eventId = params.eventId
        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.redactEvent(
                    params.txID,
                    params.roomId,
                    eventId,
                    if (params.reason == null) emptyMap() else mapOf("reason" to params.reason)
            )
        }
        return result.fold({
            when (it) {
                is Failure.NetworkConnection -> Result.retry()
                else -> {
                    //TODO mark as failed to send?
                    //always return success, or the chain will be stuck for ever!
                    Result.success()
                }
            }
        }, {
            Result.success()
        })
    }

    @AssistedInject.Factory
    interface Factory : DelegateWorkerFactory

}