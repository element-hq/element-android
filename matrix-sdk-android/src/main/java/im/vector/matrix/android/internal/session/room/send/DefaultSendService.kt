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
import im.vector.matrix.android.internal.session.room.media.MediaAttachment
import im.vector.matrix.android.internal.session.room.media.UploadMediaWorker
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import im.vector.matrix.android.internal.util.tryTransactionAsync
import java.util.concurrent.TimeUnit

private const val SEND_WORK = "SEND_WORK"
private const val BACKOFF_DELAY = 10_000L
private val WORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal class DefaultSendService(private val roomId: String,
                                  private val eventFactory: EventFactory,
                                  private val monarchy: Monarchy) : SendService {


    override fun sendTextMessage(text: String, callback: MatrixCallback<Event>): Cancelable {
        val event = eventFactory.createTextEvent(roomId, text)
        saveLiveEvent(event)
        val sendWork = createSendEventWork(event)
        WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK, ExistingWorkPolicy.APPEND, sendWork)
                .enqueue()

        return CancelableWork(sendWork.id)
    }

    override fun sendMedia(attachment: MediaAttachment, callback: MatrixCallback<Event>): Cancelable {
        // Create an event with the media file path
        val event = eventFactory.createImageEvent(roomId, attachment)
        saveLiveEvent(event)

        val uploadWork = createUploadMediaWork(attachment)
        val sendWork = createSendEventWork(event)

        WorkManager.getInstance()
                .beginUniqueWork(SEND_WORK, ExistingWorkPolicy.APPEND, uploadWork)
                .then(sendWork)
                .enqueue()
        return CancelableWork(sendWork.id)
    }

    private fun saveLiveEvent(event: Event) {
        monarchy.tryTransactionAsync { realm ->
            val chunkEntity = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId)
                    ?: return@tryTransactionAsync
            chunkEntity.add(roomId, event, PaginationDirection.FORWARDS)
        }
    }

    private fun createSendEventWork(event: Event): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return OneTimeWorkRequestBuilder<SendEventWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(sendWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createUploadMediaWork(attachment: MediaAttachment): OneTimeWorkRequest {
        val uploadMediaWorkerParams = UploadMediaWorker.Params(attachment)
        val uploadWorkData = WorkerParamsFactory.toData(uploadMediaWorkerParams)

        return OneTimeWorkRequestBuilder<UploadMediaWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(uploadWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

}
