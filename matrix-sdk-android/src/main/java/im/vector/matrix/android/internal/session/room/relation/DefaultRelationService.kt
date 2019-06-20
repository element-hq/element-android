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
package im.vector.matrix.android.internal.session.room.relation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequest
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.database.helper.addSendingEvent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.RoomEntity
import im.vector.matrix.android.internal.database.query.findLastLiveChunkFromRoom
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.RedactEventWorker
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.util.tryTransactionAsync
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import timber.log.Timber
import javax.inject.Inject

internal class DefaultRelationService @Inject constructor(private val context: Context,
                                                          private val credentials: Credentials,
                                                          private val roomId: String,
                                                          private val eventFactory: LocalEchoEventFactory,
                                                          private val findReactionEventForUndoTask: FindReactionEventForUndoTask,
                                                          private val updateQuickReactionTask: UpdateQuickReactionTask,
                                                          private val monarchy: Monarchy,
                                                          private val taskExecutor: TaskExecutor)
    : RelationService {


    override fun sendReaction(reaction: String, targetEventId: String): Cancelable {
        val event = eventFactory.createReactionEvent(roomId, targetEventId, reaction)
                .also {
                    saveLocalEcho(it)
                }
        val sendRelationWork = createSendRelationWork(event)
        TimelineSendEventWorkCommon.postWork(context, roomId, sendRelationWork)
        return CancelableWork(context, sendRelationWork.id)
    }


    private fun createSendRelationWork(event: Event): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(credentials.userId,roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData)

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
                        if (data.redactEventId == null) {
                            Timber.w("Cannot find reaction to undo (not yet synced?)")
                            //TODO?
                        }
                        data.redactEventId?.let { toRedact ->

                            val redactEvent = eventFactory.createRedactEvent(roomId, toRedact, null).also {
                                saveLocalEcho(it)
                            }
                            val redactWork = createRedactEventWork(redactEvent, toRedact, null)

                            TimelineSendEventWorkCommon.postWork(context, roomId, redactWork)

                        }
                    }
                })
                .executeBy(taskExecutor)

    }


    override fun updateQuickReaction(reaction: String, oppositeReaction: String, targetEventId: String, myUserId: String) {

        val params = UpdateQuickReactionTask.Params(
                roomId,
                targetEventId,
                reaction,
                oppositeReaction,
                myUserId
        )

        updateQuickReactionTask.configureWith(params)
                .dispatchTo(object : MatrixCallback<UpdateQuickReactionTask.Result> {
                    override fun onSuccess(data: UpdateQuickReactionTask.Result) {
                        data.reactionToAdd?.also { sendReaction(it, targetEventId) }
                        data.reactionToRedact.forEach {
                            val redactEvent = eventFactory.createRedactEvent(roomId, it, null).also {
                                saveLocalEcho(it)
                            }
                            val redactWork = createRedactEventWork(redactEvent, it, null)
                            TimelineSendEventWorkCommon.postWork(context, roomId, redactWork)
                        }
                    }
                })
                .executeBy(taskExecutor)
    }

    private fun buildWorkIdentifier(identifier: String): String {
        return "${roomId}_$identifier"
    }

    //TODO duplicate with send service?
    private fun createRedactEventWork(localEvent: Event, eventId: String, reason: String?): OneTimeWorkRequest {

        val sendContentWorkerParams = RedactEventWorker.Params(credentials.userId ,localEvent.eventId!!,
                                                               roomId, eventId, reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return TimelineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData)
    }

    override fun editTextMessage(targetEventId: String, newBodyText: String, newBodyAutoMarkdown: Boolean, compatibilityBodyText: String): Cancelable {
        val event = eventFactory.createReplaceTextEvent(roomId, targetEventId, newBodyText, newBodyAutoMarkdown, MessageType.MSGTYPE_TEXT, compatibilityBodyText).also {
            saveLocalEcho(it)
        }
        val sendContentWorkerParams = SendEventWorker.Params(credentials.userId ,roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        //TODO use relation API?

        val workRequest = TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData)
        TimelineSendEventWorkCommon.postWork(context, roomId, workRequest)
        return CancelableWork(context, workRequest.id)

    }


    override fun replyToMessage(eventReplied: Event, replyText: String): Cancelable? {
        val event = eventFactory.createReplyTextEvent(roomId, eventReplied, replyText)?.also {
            saveLocalEcho(it)
        } ?: return null
        val sendContentWorkerParams = SendEventWorker.Params(credentials.userId ,roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)


        val workRequest = TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData)
        TimelineSendEventWorkCommon.postWork(context, roomId, workRequest)
        return CancelableWork(context, workRequest.id)
    }


    override fun getEventSummaryLive(eventId: String): LiveData<List<EventAnnotationsSummary>> {
        return monarchy.findAllMappedWithChanges(
                { realm ->
                    EventAnnotationsSummaryEntity.where(realm, eventId)
                },
                {
                    it.asDomain()
                }
        )
    }

    /**
     * Saves the event in database as a local echo.
     * SendState is set to UNSENT and it's added to a the sendingTimelineEvents list of the room.
     * The sendingTimelineEvents is checked on new sync and will remove the local echo if an event with
     * the same transaction id is received (in unsigned data)
     */
    private fun saveLocalEcho(event: Event) {
        monarchy.tryTransactionAsync { realm ->
            val roomEntity = RoomEntity.where(realm, roomId = roomId).findFirst()
                             ?: return@tryTransactionAsync
            val liveChunk = ChunkEntity.findLastLiveChunkFromRoom(realm, roomId = roomId)
                            ?: return@tryTransactionAsync

            roomEntity.addSendingEvent(event, liveChunk.forwardsStateIndex ?: 0)
        }
    }
}
