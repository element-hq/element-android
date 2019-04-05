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

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionAsync
import java.util.concurrent.TimeUnit

private const val SEND_WORK = "SEND_WORK"

internal class DefaultSendService(private val roomId: String,
                                  private val eventFactory: EventFactory,
                                  private val monarchy: Monarchy) : SendService {

    private val sendConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    // TODO callback is not used
    override fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable {
        val event = eventFactory.createTextEvent(roomId, text)

        monarchy.tryTransactionAsync { realm ->
            val chunkEntity = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
                    ?: return@tryTransactionAsync
            chunkEntity.add(roomId, event, PaginationDirection.FORWARDS)
        }

        val sendContentWorkerParams = SendEventWorker.Params(roomId, event)
        val workData = WorkerParamsFactory.toData(sendContentWorkerParams)

        val sendWork = OneTimeWorkRequestBuilder<SendEventWorker>()
                .setConstraints(sendConstraints)
                .setInputData(workData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10_000, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()

        return CancelableWork(sendWork.id)

    }

}
