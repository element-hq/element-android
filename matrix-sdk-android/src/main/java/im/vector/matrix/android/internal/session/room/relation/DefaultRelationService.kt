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
import androidx.lifecycle.Transformations
import androidx.work.OneTimeWorkRequest
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.room.model.EventAnnotationsSummary
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.relation.RelationService
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.api.util.toOptional
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.session.room.send.EncryptEventWorker
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.session.room.send.RedactEventWorker
import im.vector.matrix.android.internal.session.room.send.SendEventWorker
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import timber.log.Timber

internal class DefaultRelationService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                  private val context: Context,
                                                                  @UserId private val userId: String,
                                                                  private val eventFactory: LocalEchoEventFactory,
                                                                  private val cryptoService: CryptoService,
                                                                  private val findReactionEventForUndoTask: FindReactionEventForUndoTask,
                                                                  private val fetchEditHistoryTask: FetchEditHistoryTask,
                                                                  private val monarchy: Monarchy,
                                                                  private val taskExecutor: TaskExecutor)
    : RelationService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): RelationService
    }

    override fun sendReaction(targetEventId: String, reaction: String): Cancelable {
        val event = eventFactory.createReactionEvent(roomId, targetEventId, reaction)
                .also { saveLocalEcho(it) }
        val sendRelationWork = createSendEventWork(event, true)
        TimelineSendEventWorkCommon.postWork(context, roomId, sendRelationWork)
        return CancelableWork(context, sendRelationWork.id)
    }

    override fun undoReaction(targetEventId: String, reaction: String): Cancelable {
        val params = FindReactionEventForUndoTask.Params(
                roomId,
                targetEventId,
                reaction
        )
        // TODO We should avoid using MatrixCallback internally
        val callback = object : MatrixCallback<FindReactionEventForUndoTask.Result> {
            override fun onSuccess(data: FindReactionEventForUndoTask.Result) {
                if (data.redactEventId == null) {
                    Timber.w("Cannot find reaction to undo (not yet synced?)")
                    // TODO?
                }
                data.redactEventId?.let { toRedact ->
                    val redactEvent = eventFactory.createRedactEvent(roomId, toRedact, null)
                            .also { saveLocalEcho(it) }
                    val redactWork = createRedactEventWork(redactEvent, toRedact, null)

                    TimelineSendEventWorkCommon.postWork(context, roomId, redactWork)
                }
            }
        }
        return findReactionEventForUndoTask
                .configureWith(params) {
                    this.retryCount = Int.MAX_VALUE
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    // TODO duplicate with send service?
    private fun createRedactEventWork(localEvent: Event, eventId: String, reason: String?): OneTimeWorkRequest {
        val sendContentWorkerParams = RedactEventWorker.Params(
                userId,
                localEvent.eventId!!,
                roomId,
                eventId,
                reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return TimelineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData, true)
    }

    override fun editTextMessage(targetEventId: String,
                                 msgType: String,
                                 newBodyText: CharSequence,
                                 newBodyAutoMarkdown: Boolean,
                                 compatibilityBodyText: String): Cancelable {
        val event = eventFactory
                .createReplaceTextEvent(roomId, targetEventId, newBodyText, newBodyAutoMarkdown, msgType, compatibilityBodyText)
                .also { saveLocalEcho(it) }
        return if (cryptoService.isRoomEncrypted(roomId)) {
            val encryptWork = createEncryptEventWork(event, listOf("m.relates_to"))
            val workRequest = createSendEventWork(event, false)
            TimelineSendEventWorkCommon.postSequentialWorks(context, roomId, encryptWork, workRequest)
            CancelableWork(context, encryptWork.id)
        } else {
            val workRequest = createSendEventWork(event, true)
            TimelineSendEventWorkCommon.postWork(context, roomId, workRequest)
            CancelableWork(context, workRequest.id)
        }
    }

    override fun editReply(replyToEdit: TimelineEvent,
                           originalTimelineEvent: TimelineEvent,
                           newBodyText: String,
                           compatibilityBodyText: String): Cancelable {
        val event = eventFactory
                .createReplaceTextOfReply(roomId,
                        replyToEdit,
                        originalTimelineEvent,
                        newBodyText, true, MessageType.MSGTYPE_TEXT, compatibilityBodyText)
                .also { saveLocalEcho(it) }
        return if (cryptoService.isRoomEncrypted(roomId)) {
            val encryptWork = createEncryptEventWork(event, listOf("m.relates_to"))
            val workRequest = createSendEventWork(event, false)
            TimelineSendEventWorkCommon.postSequentialWorks(context, roomId, encryptWork, workRequest)
            CancelableWork(context, encryptWork.id)
        } else {
            val workRequest = createSendEventWork(event, true)
            TimelineSendEventWorkCommon.postWork(context, roomId, workRequest)
            CancelableWork(context, workRequest.id)
        }
    }

    override fun fetchEditHistory(eventId: String, callback: MatrixCallback<List<Event>>) {
        val params = FetchEditHistoryTask.Params(roomId, cryptoService.isRoomEncrypted(roomId), eventId)
        fetchEditHistoryTask
                .configureWith(params) {
                    this.callback = callback
                }
                .executeBy(taskExecutor)
    }

    override fun replyToMessage(eventReplied: TimelineEvent, replyText: CharSequence, autoMarkdown: Boolean): Cancelable? {
        val event = eventFactory.createReplyTextEvent(roomId, eventReplied, replyText, autoMarkdown)
                ?.also { saveLocalEcho(it) }
                ?: return null

        return if (cryptoService.isRoomEncrypted(roomId)) {
            val encryptWork = createEncryptEventWork(event, listOf("m.relates_to"))
            val workRequest = createSendEventWork(event, false)
            TimelineSendEventWorkCommon.postSequentialWorks(context, roomId, encryptWork, workRequest)
            CancelableWork(context, encryptWork.id)
        } else {
            val workRequest = createSendEventWork(event, true)
            TimelineSendEventWorkCommon.postWork(context, roomId, workRequest)
            CancelableWork(context, workRequest.id)
        }
    }

    private fun createEncryptEventWork(event: Event, keepKeys: List<String>?): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(userId, roomId, event, keepKeys)
        val sendWorkData = WorkerParamsFactory.toData(params)
        return TimelineSendEventWorkCommon.createWork<EncryptEventWorker>(sendWorkData, true)
    }

    private fun createSendEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(userId, roomId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return TimelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }

    override fun getEventSummaryLive(eventId: String): LiveData<Optional<EventAnnotationsSummary>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { EventAnnotationsSummaryEntity.where(it, eventId) },
                { it.asDomain() }
        )
        return Transformations.map(liveData) { results ->
            results.firstOrNull().toOptional()
        }
    }

    /**
     * Saves the event in database as a local echo.
     * SendState is set to UNSENT and it's added to a the sendingTimelineEvents list of the room.
     * The sendingTimelineEvents is checked on new sync and will remove the local echo if an event with
     * the same transaction id is received (in unsigned data)
     */
    private fun saveLocalEcho(event: Event) {
        eventFactory.saveLocalEcho(monarchy, event)
    }
}
