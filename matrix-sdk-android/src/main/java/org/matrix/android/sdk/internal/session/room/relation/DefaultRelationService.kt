/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.room.relation

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.work.OneTimeWorkRequest
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.model.EventAnnotationsSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.relation.RelationService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.NoOpCancellable
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.EventAnnotationsSummaryEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.session.room.send.EncryptEventWorker
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.RedactEventWorker
import org.matrix.android.sdk.internal.session.room.send.SendEventWorker
import org.matrix.android.sdk.internal.session.room.timeline.TimelineSendEventWorkCommon
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import org.matrix.android.sdk.internal.util.fetchCopyMap
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber

internal class DefaultRelationService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionId private val sessionId: String,
        private val timeLineSendEventWorkCommon: TimelineSendEventWorkCommon,
        private val eventFactory: LocalEchoEventFactory,
        private val cryptoService: CryptoService,
        private val findReactionEventForUndoTask: FindReactionEventForUndoTask,
        private val fetchEditHistoryTask: FetchEditHistoryTask,
        private val timelineEventMapper: TimelineEventMapper,
        @SessionDatabase private val monarchy: Monarchy,
        private val taskExecutor: TaskExecutor)
    : RelationService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): RelationService
    }

    override fun sendReaction(targetEventId: String, reaction: String): Cancelable {
        return if (monarchy
                        .fetchCopyMap(
                                { realm ->
                                    TimelineEventEntity.where(realm, roomId, targetEventId).findFirst()
                                },
                                { entity, _ ->
                                    timelineEventMapper.map(entity)
                                })
                        ?.annotations
                        ?.reactionsSummary
                        .orEmpty()
                        .none { it.addedByMe && it.key == reaction }) {
            val event = eventFactory.createReactionEvent(roomId, targetEventId, reaction)
                    .also { saveLocalEcho(it) }
            val sendRelationWork = createSendEventWork(event, true)
            timeLineSendEventWorkCommon.postWork(roomId, sendRelationWork)
        } else {
            Timber.w("Reaction already added")
            NoOpCancellable
        }
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

                    timeLineSendEventWorkCommon.postWork(roomId, redactWork)
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
                sessionId,
                localEvent.eventId!!,
                roomId,
                eventId,
                reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return timeLineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData, true)
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
            timeLineSendEventWorkCommon.postSequentialWorks(roomId, encryptWork, workRequest)
        } else {
            val workRequest = createSendEventWork(event, true)
            timeLineSendEventWorkCommon.postWork(roomId, workRequest)
        }
    }

    override fun editReply(replyToEdit: TimelineEvent,
                           originalTimelineEvent: TimelineEvent,
                           newBodyText: String,
                           compatibilityBodyText: String): Cancelable {
        val event = eventFactory.createReplaceTextOfReply(
                roomId,
                replyToEdit,
                originalTimelineEvent,
                newBodyText,
                true,
                MessageType.MSGTYPE_TEXT,
                compatibilityBodyText
        )
                .also { saveLocalEcho(it) }
        return if (cryptoService.isRoomEncrypted(roomId)) {
            val encryptWork = createEncryptEventWork(event, listOf("m.relates_to"))
            val workRequest = createSendEventWork(event, false)
            timeLineSendEventWorkCommon.postSequentialWorks(roomId, encryptWork, workRequest)
        } else {
            val workRequest = createSendEventWork(event, true)
            timeLineSendEventWorkCommon.postWork(roomId, workRequest)
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
            timeLineSendEventWorkCommon.postSequentialWorks(roomId, encryptWork, workRequest)
        } else {
            val workRequest = createSendEventWork(event, true)
            timeLineSendEventWorkCommon.postWork(roomId, workRequest)
        }
    }

    private fun createEncryptEventWork(event: Event, keepKeys: List<String>?): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(sessionId, event, keepKeys)
        val sendWorkData = WorkerParamsFactory.toData(params)
        return timeLineSendEventWorkCommon.createWork<EncryptEventWorker>(sendWorkData, true)
    }

    private fun createSendEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return timeLineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }

    override fun getEventAnnotationsSummary(eventId: String): EventAnnotationsSummary? {
        return monarchy.fetchCopyMap(
                { EventAnnotationsSummaryEntity.where(it, eventId).findFirst() },
                { entity, _ ->
                    entity.asDomain()
                }
        )
    }

    override fun getEventAnnotationsSummaryLive(eventId: String): LiveData<Optional<EventAnnotationsSummary>> {
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
        eventFactory.createLocalEcho(event)
    }
}
