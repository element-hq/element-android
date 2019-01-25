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
import com.squareup.moshi.JsonClass
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionSync
import org.koin.standalone.inject

internal class SendEventWorker(context: Context, params: WorkerParameters)
    : Worker(context, params), MatrixKoinComponent {


    @JsonClass(generateAdapter = true)
    internal data class Params(
            val roomId: String,
            val event: Event
    )

    private val roomAPI by inject<RoomAPI>()
    private val monarchy by inject<Monarchy>()

    override fun doWork(): Result {

        val params = WorkerParamsFactory.fromData<Params>(inputData)
                     ?: return Result.failure()

        if (params.event.eventId == null) {
            return Result.failure()
        }

        val result = executeRequest<SendResponse> {
            apiCall = roomAPI.send(
                    params.event.eventId,
                    params.roomId,
                    params.event.type,
                    params.event.content
            )
        }
        result.flatMap { sendResponse ->
            monarchy.tryTransactionSync { realm ->
                val dummyEventEntity = EventEntity.where(realm, params.event.eventId).findFirst()
                dummyEventEntity?.eventId = sendResponse.eventId
            }
        }
        return result.fold({ Result.retry() }, { Result.success() })
    }


}
