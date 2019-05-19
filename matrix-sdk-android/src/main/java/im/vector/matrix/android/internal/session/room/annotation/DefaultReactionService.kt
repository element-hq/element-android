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
package im.vector.matrix.android.internal.session.room.annotation

import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.annotation.ReactionService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.RedactEventWorker
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.WorkerParamsFactory
import java.util.concurrent.TimeUnit

private const val REACTION_WORK = "REACTION_WORK"
private const val BACKOFF_DELAY = 10_000L

private val WORK_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

internal class DefaultReactionService(private val roomId: String,
                                      private val eventFactory: LocalEchoEventFactory,
                                      private val monarchy: Monarchy,
                                      private val findReactionEventForUndoTask: FindReactionEventForUndoTask,
                                      private val taskExecutor: TaskExecutor)
    : ReactionService {


    override fun sendReaction(reaction: String, targetEventId: String): Cancelable {
        val event = eventFactory.createReactionEvent(roomId, targetEventId, reaction)
//                .also {
//            //saveLocalEcho(it)
//        }
        val sendRelationWork = createSendRelationWork(event)
        WorkManager.getInstance()
                .beginUniqueWork(buildWorkIdentifier(REACTION_WORK), ExistingWorkPolicy.APPEND, sendRelationWork)
                .enqueue()
        return CancelableWork(sendRelationWork.id)
    }


    private fun createSendRelationWork(event: Event): OneTimeWorkRequest {
        //TODO use the new API to send relation (for now use regular send)
        val sendContentWorkerParams = SendEventWorker.Params(
                roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return OneTimeWorkRequestBuilder<SendEventWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(sendWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    override fun undoReaction(reaction: String, targetEventId: String, myUserId: String)/*: Cancelable*/ {

        val params = FindReactionEventForUndoTask.Params(
                roomId,
                targetEventId,
                reaction,
                myUserId
        )
        findReactionEventForUndoTask.configureWith(params)
                .enableRetry()
                .dispatchTo(object : MatrixCallback<FindReactionEventForUndoTask.Result> {
                    override fun onSuccess(data: FindReactionEventForUndoTask.Result) {
                        data.redactEventId?.let { toRedact ->
                            val redactWork = createRedactEventWork(toRedact, null)
                            WorkManager.getInstance()
                                    .beginUniqueWork(buildWorkIdentifier(REACTION_WORK), ExistingWorkPolicy.APPEND, redactWork)
                                    .enqueue()
                        }
                    }
                })
                .executeBy(taskExecutor)

    }

    private fun buildWorkIdentifier(identifier: String): String {
        return "${roomId}_$identifier"
    }

//    private fun saveLocalEcho(event: Event) {
//        monarchy.tryTransactionAsync { realm ->
//            val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()
//                    ?: return@tryTransactionAsync
//            val liveChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId = roomId)
//                    ?: return@tryTransactionAsync
//
//            roomEntity.addSendingEvent(event, liveChunk.forwardsStateIndex ?: 0)
//        }
//    }

    //TODO duplicate with send service?
    private fun createRedactEventWork(eventId: String, reason: String?): OneTimeWorkRequest {

        //TODO create local echo of m.room.redaction event?

        val sendContentWorkerParams = RedactEventWorker.Params(
                roomId, eventId, reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return OneTimeWorkRequestBuilder<RedactEventWorker>()
                .setConstraints(WORK_CONSTRAINTS)
                .setInputData(redactWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }
}
