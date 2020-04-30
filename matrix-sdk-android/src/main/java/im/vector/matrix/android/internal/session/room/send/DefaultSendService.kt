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

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.isImageMessage
import im.vector.matrix.android.api.session.events.model.isTextMessage
import im.vector.matrix.android.api.session.room.model.message.OptionItem
import im.vector.matrix.android.api.session.room.send.SendService
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.api.util.CancelableBag
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.WorkManagerProvider
import im.vector.matrix.android.internal.session.content.UploadContentWorker
import im.vector.matrix.android.internal.session.room.timeline.TimelineSendEventWorkCommon
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.CancelableWork
import im.vector.matrix.android.internal.worker.AlwaysSuccessfulWorker
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.startChain
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
        private val localEchoRepository: LocalEchoRepository
) : SendService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): SendService
    }

    private val workerFutureListenerExecutor = Executors.newSingleThreadExecutor()

    override fun sendTextMessage(text: CharSequence, msgType: String, autoMarkdown: Boolean): Cancelable {
        val event = localEchoEventFactory.createTextEvent(roomId, msgType, text, autoMarkdown).also {
            createLocalEcho(it)
        }
        return sendEvent(event)
    }

    // For test only
    private fun sendTextMessages(text: CharSequence, msgType: String, autoMarkdown: Boolean, times: Int): Cancelable {
        return CancelableBag().apply {
            // Send the event several times
            repeat(times) { i ->
                val event = localEchoEventFactory.createTextEvent(roomId, msgType, "$text - $i", autoMarkdown).also {
                    createLocalEcho(it)
                }
                add(sendEvent(event))
            }
        }
    }

    override fun sendFormattedTextMessage(text: String, formattedText: String, msgType: String): Cancelable {
        val event = localEchoEventFactory.createFormattedTextEvent(roomId, TextContent(text, formattedText), msgType).also {
            createLocalEcho(it)
        }
        return sendEvent(event)
    }

    override fun sendPoll(question: String, options: List<OptionItem>): Cancelable {
        val event = localEchoEventFactory.createPollEvent(roomId, question, options).also {
            createLocalEcho(it)
        }
        return sendEvent(event)
    }

    override fun sendOptionsReply(pollEventId: String, optionIndex: Int, optionValue: String): Cancelable {
        val event = localEchoEventFactory.createOptionsReplyEvent(roomId, pollEventId, optionIndex, optionValue).also {
            createLocalEcho(it)
        }
        return sendEvent(event)
    }

    private fun sendEvent(event: Event): Cancelable {
        // Encrypted room handling
        return if (cryptoService.isRoomEncrypted(roomId)) {
            Timber.v("Send event in encrypted room")
            val encryptWork = createEncryptEventWork(event, true)
            // Note that event will be replaced by the result of the previous work
            val sendWork = createSendEventWork(event, false)
            timelineSendEventWorkCommon.postSequentialWorks(roomId, encryptWork, sendWork)
        } else {
            val sendWork = createSendEventWork(event, true)
            timelineSendEventWorkCommon.postWork(roomId, sendWork)
        }
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
        val redactWork = createRedactEventWork(event, reason)
        return timelineSendEventWorkCommon.postWork(roomId, redactWork)
    }

    override fun resendTextMessage(localEcho: TimelineEvent): Cancelable? {
        if (localEcho.root.isTextMessage() && localEcho.root.sendState.hasFailed()) {
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

    private fun createLocalEcho(event: Event) {
        localEchoEventFactory.createLocalEcho(event)
    }

    private fun buildWorkName(identifier: String): String {
        return "${roomId}_$identifier"
    }

    private fun createEncryptEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        // Same parameter
        val params = EncryptEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(params)

        return workManagerProvider.matrixOneTimeWorkRequestBuilder<EncryptEventWorker>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .setInputData(sendWorkData)
                .startChain(startChain)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun createSendEventWork(event: Event, startChain: Boolean): OneTimeWorkRequest {
        val sendContentWorkerParams = SendEventWorker.Params(sessionId, event)
        val sendWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)

        return timelineSendEventWorkCommon.createWork<SendEventWorker>(sendWorkData, startChain)
    }

    private fun createRedactEventWork(event: Event, reason: String?): OneTimeWorkRequest {
        val redactEvent = localEchoEventFactory.createRedactEvent(roomId, event.eventId!!, reason).also {
            createLocalEcho(it)
        }
        val sendContentWorkerParams = RedactEventWorker.Params(sessionId, redactEvent.eventId!!, roomId, event.eventId, reason)
        val redactWorkData = WorkerParamsFactory.toData(sendContentWorkerParams)
        return timelineSendEventWorkCommon.createWork<RedactEventWorker>(redactWorkData, true)
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
