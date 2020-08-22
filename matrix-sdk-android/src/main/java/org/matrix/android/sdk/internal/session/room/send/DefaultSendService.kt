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

package org.matrix.android.sdk.internal.session.room.send

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.room.model.message.OptionItem
import org.matrix.android.sdk.api.session.room.send.SendService
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.api.util.CancelableBag
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.content.UploadContentWorker
import org.matrix.android.sdk.internal.session.room.timeline.TimelineSendEventWorkCommon
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.CancelableWork
import org.matrix.android.sdk.internal.worker.AlwaysSuccessfulWorker
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import org.matrix.android.sdk.internal.worker.startChain
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val UPLOAD_WORK = "UPLOAD_WORK"

internal class DefaultSendService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val workManagerProvider: WorkManagerProvider,
        private val timelineSendEventWorkCommon: TimelineSendEventWorkCommon,
        @SessionId private val sessionId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val cryptoService: CryptoService,
        private val taskExecutor: TaskExecutor,
        private val localEchoRepository: LocalEchoRepository,
        private val roomEventSender: RoomEventSender
) : SendService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): SendService
    }

    private val workerFutureListenerExecutor = Executors.newSingleThreadExecutor()

    override fun sendEvent(eventType: String, content: JsonDict?): Cancelable {
        return localEchoEventFactory.createEvent(roomId, eventType, content)
                .also { createLocalEcho(it) }
                .let { sendEvent(it) }
    }

    override fun sendTextMessage(text: CharSequence, msgType: String, autoMarkdown: Boolean): Cancelable {
        return localEchoEventFactory.createTextEvent(roomId, msgType, text, autoMarkdown)
                .also { createLocalEcho(it) }
                .let { sendEvent(it) }
    }

    // For test only
    private fun sendTextMessages(text: CharSequence, msgType: String, autoMarkdown: Boolean, times: Int): Cancelable {
        return CancelableBag().apply {
            // Send the event several times
            repeat(times) { i ->
                localEchoEventFactory.createTextEvent(roomId, msgType, "$text - $i", autoMarkdown)
                        .also { createLocalEcho(it) }
                        .let { sendEvent(it) }
                        .also { add(it) }
            }
        }
    }

    override fun sendFormattedTextMessage(text: String, formattedText: String, msgType: String): Cancelable {
        return localEchoEventFactory.createFormattedTextEvent(roomId, TextContent(text, formattedText), msgType)
                .also { createLocalEcho(it) }
                .let { sendEvent(it) }
    }

    override fun sendPoll(question: String, options: List<OptionItem>): Cancelable {
        return localEchoEventFactory.createPollEvent(roomId, question, options)
                .also { createLocalEcho(it) }
                .let { sendEvent(it) }
    }

    override fun sendOptionsReply(pollEventId: String, optionIndex: Int, optionValue: String): Cancelable {
        return localEchoEventFactory.createOptionsReplyEvent(roomId, pollEventId, optionIndex, optionValue)
                .also { createLocalEcho(it) }
                .let { sendEvent(it) }
    }

    override fun sendMedias(attachments: List<ContentAttachmentData>,
                            compressBeforeSending: Boolean,
                            roomIds: Set<String>): Cancelable {
        return attachments.mapTo(CancelableBag()) {
            sendMedia(it, compressBeforeSending, roomIds)
        }
    }

    override fun redactEvent(event: Event, reason: String?): Cancelable {
        // TODO manage media/attachements?
        return createRedactEventWork(event, reason)
                .let { timelineSendEventWorkCommon.postWork(roomId, it) }
    }

    override fun resendTextMessage(localEcho: TimelineEvent): Cancelable? {
        if (localEcho.root.isTextMessage() && localEcho.root.sendState.hasFailed()) {
            localEchoRepository.updateSendState(localEcho.eventId, SendState.UNSENT)
            return sendEvent(localEcho.root)
        }
        return null
    }

    override fun resendMediaMessage(localEcho: TimelineEvent): Cancelable? {
        if (localEcho.root.isImageMessage() && localEcho.root.sendState.hasFailed()) {
            // TODO this need a refactoring of attachement sending
//        val clearContent = localEcho.root.getClearContent()
//        val messageContent = clearContent?.toModel<MessageContent>() ?: return null
//        when (messageContent.type) {
//            MessageType.MSGTYPE_IMAGE -> {
//                val imageContent = clearContent.toModel<MessageImageContent>() ?: return null
//                val url = imageContent.url ?: return null
//                if (url.startsWith("mxc://")) {
//                    //TODO
//                } else {
//                    //The image has not yet been sent
//                    val attachmentData = ContentAttachmentData(
//                            size = imageContent.info!!.size.toLong(),
//                            mimeType = imageContent.info.mimeType!!,
//                            width = imageContent.info.width.toLong(),
//                            height = imageContent.info.height.toLong(),
//                            name = imageContent.body,
//                            path = imageContent.url,
//                            type = ContentAttachmentData.Type.IMAGE
//                    )
//                    monarchy.runTransactionSync {
//                        EventEntity.where(it,eventId = localEcho.root.eventId ?: "").findFirst()?.let {
//                            it.sendState = SendState.UNSENT
//                        }
//                    }
//                    return internalSendMedia(localEcho.root,attachmentData)
//                }
//            }
//        }
            return null
        }
        return null
    }

    override fun deleteFailedEcho(localEcho: TimelineEvent) {
        taskExecutor.executorScope.launch {
            localEchoRepository.deleteFailedEcho(roomId, localEcho)
        }
    }

    override fun clearSendingQueue() {
        timelineSendEventWorkCommon.cancelAllWorks(roomId)
        workManagerProvider.workManager.cancelUniqueWork(buildWorkName(UPLOAD_WORK))

        // Replace the worker chains with a AlwaysSuccessfulWorker, to ensure the queues are well emptied
        workManagerProvider.matrixOneTimeWorkRequestBuilder<AlwaysSuccessfulWorker>()
                .build().let {
                    timelineSendEventWorkCommon.postWork(roomId, it, ExistingWorkPolicy.REPLACE)

                    // need to clear also image sending queue
                    workManagerProvider.workManager
                            .beginUniqueWork(buildWorkName(UPLOAD_WORK), ExistingWorkPolicy.REPLACE, it)
                            .enqueue()
                }
        taskExecutor.executorScope.launch {
            localEchoRepository.clearSendingQueue(roomId)
        }
    }

    override fun resendAllFailedMessages() {
        taskExecutor.executorScope.launch {
            val eventsToResend = localEchoRepository.getAllFailedEventsToResend(roomId)
            eventsToResend.forEach {
                sendEvent(it)
            }
            localEchoRepository.updateSendState(roomId, eventsToResend.mapNotNull { it.eventId }, SendState.UNSENT)
        }
    }

    override fun sendMedia(attachment: ContentAttachmentData,
                           compressBeforeSending: Boolean,
                           roomIds: Set<String>): Cancelable {
        // Create an event with the media file path
        // Ensure current roomId is included in the set
        val allRoomIds = (roomIds + roomId).toList()

        // Create local echo for each room
        val allLocalEchoes = allRoomIds.map {
            localEchoEventFactory.createMediaEvent(it, attachment).also { event ->
                createLocalEcho(event)
            }
        }
        return internalSendMedia(allLocalEchoes, attachment, compressBeforeSending)
    }

    /**
     * We use the roomId of the local echo event
     */
    private fun internalSendMedia(allLocalEchoes: List<Event>, attachment: ContentAttachmentData, compressBeforeSending: Boolean): Cancelable {
        val cancelableBag = CancelableBag()

        allLocalEchoes.groupBy { cryptoService.isRoomEncrypted(it.roomId!!) }
                .apply {
                    keys.forEach { isRoomEncrypted ->
                        // Should never be empty
                        val localEchoes = get(isRoomEncrypted).orEmpty()
                        val uploadWork = createUploadMediaWork(localEchoes, attachment, isRoomEncrypted, compressBeforeSending)

                        val dispatcherWork = createMultipleEventDispatcherWork(isRoomEncrypted)

                        workManagerProvider.workManager
                                .beginUniqueWork(buildWorkName(UPLOAD_WORK), ExistingWorkPolicy.APPEND, uploadWork)
                                .then(dispatcherWork)
                                .enqueue()
                                .also { operation ->
                                    operation.result.addListener(Runnable {
                                        if (operation.result.isCancelled) {
                                            Timber.e("CHAIN WAS CANCELLED")
                                        } else if (operation.state.value is Operation.State.FAILURE) {
                                            Timber.e("CHAIN DID FAIL")
                                        }
                                    }, workerFutureListenerExecutor)
                                }

                        cancelableBag.add(CancelableWork(workManagerProvider.workManager, dispatcherWork.id))
                    }
                }

        return cancelableBag
    }

    private fun sendEvent(event: Event): Cancelable {
        return roomEventSender.sendEvent(event)
    }

    private fun createLocalEcho(event: Event) {
        localEchoEventFactory.createLocalEcho(event)
    }

    private fun buildWorkName(identifier: String): String {
        return "${roomId}_$identifier"
    }

    private fun createEncryptEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        // Same parameter
        return EncryptEventWorker.Params(sessionId, event)
                .let { WorkerParamsFactory.toData(it) }
                .let {
                    workManagerProvider.matrixOneTimeWorkRequestBuilder<EncryptEventWorker>()
                            .setConstraints(WorkManagerProvider.workConstraints)
                            .setInputData(it)
                            .startChain(startChain)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                            .build()
                }
    }

    private fun createRedactEventWork(event: Event, reason: String?): OneTimeWorkRequest {
        return localEchoEventFactory.createRedactEvent(roomId, event.eventId!!, reason)
                .also { createLocalEcho(it) }
                .let { RedactEventWorker.Params(sessionId, it.eventId!!, roomId, event.eventId, reason) }
                .let { WorkerParamsFactory.toData(it) }
                .let { timelineSendEventWorkCommon.createWork<RedactEventWorker>(it, true) }
    }

    private fun createUploadMediaWork(allLocalEchos: List<Event>,
                                      attachment: ContentAttachmentData,
                                      isRoomEncrypted: Boolean,
                                      compressBeforeSending: Boolean): OneTimeWorkRequest {
        val uploadMediaWorkerParams = UploadContentWorker.Params(sessionId, allLocalEchos, attachment, isRoomEncrypted, compressBeforeSending)
        val uploadWorkData = WorkerParamsFactory.toData(uploadMediaWorkerParams)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<UploadContentWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(true)
                .setInputData(uploadWorkData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createMultipleEventDispatcherWork(isRoomEncrypted: Boolean): OneTimeWorkRequest {
        // the list of events will be replaced by the result of the media upload work
        val params = MultipleEventSendingDispatcherWorker.Params(sessionId, emptyList(), isRoomEncrypted)
        val workData = WorkerParamsFactory.toData(params)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<MultipleEventSendingDispatcherWorker>()
                // No constraint
                // .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(false)
                .setInputData(workData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }
}
