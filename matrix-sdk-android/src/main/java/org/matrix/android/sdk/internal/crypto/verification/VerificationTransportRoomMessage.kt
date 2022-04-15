/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.crypto.verification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.events.model.UnsignedData
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationAcceptContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationCancelContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationDoneContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationKeyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationMacContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationReadyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationStartContent
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import timber.log.Timber
import java.util.concurrent.Executors

internal class VerificationTransportRoomMessage(
        private val sendVerificationMessageTask: SendVerificationMessageTask,
        private val userId: String,
        private val userDeviceId: String?,
        private val roomId: String,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val tx: DefaultVerificationTransaction?
) : VerificationTransport {

    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val verificationSenderScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sequencer = SemaphoreCoroutineSequencer()

    override fun <T> sendToOther(type: String,
                                 verificationInfo: VerificationInfo<T>,
                                 nextState: VerificationTxState,
                                 onErrorReason: CancelCode,
                                 onDone: (() -> Unit)?) {
        Timber.d("## SAS sending msg type $type")
        Timber.v("## SAS sending msg info $verificationInfo")
        val event = createEventAndLocalEcho(
                type = type,
                roomId = roomId,
                content = verificationInfo.toEventContent()!!
        )

        verificationSenderScope.launch {
            sequencer.post {
                try {
                    val params = SendVerificationMessageTask.Params(event)
                    sendVerificationMessageTask.executeRetry(params, 5)
                    // Do I need to update local echo state to sent?
                    if (onDone != null) {
                        onDone()
                    } else {
                        tx?.state = nextState
                    }
                } catch (failure: Throwable) {
                    tx?.cancel(onErrorReason)
                }
            }
        }
    }

    override fun sendVerificationRequest(supportedMethods: List<String>,
                                         localId: String,
                                         otherUserId: String,
                                         roomId: String?,
                                         toDevices: List<String>?,
                                         callback: (String?, ValidVerificationInfoRequest?) -> Unit) {
        Timber.d("## SAS sending verification request with supported methods: $supportedMethods")
        // This transport requires a room
        requireNotNull(roomId)

        val validInfo = ValidVerificationInfoRequest(
                transactionId = "",
                fromDevice = userDeviceId ?: "",
                methods = supportedMethods,
                timestamp = System.currentTimeMillis()
        )

        val info = MessageVerificationRequestContent(
                body = "$userId is requesting to verify your key, but your client does not support in-chat key verification." +
                        " You will need to use legacy key verification to verify keys.",
                fromDevice = validInfo.fromDevice,
                toUserId = otherUserId,
                timestamp = validInfo.timestamp,
                methods = validInfo.methods
        )
        val content = info.toContent()

        val event = createEventAndLocalEcho(
                localId = localId,
                type = EventType.MESSAGE,
                roomId = roomId,
                content = content
        )

        verificationSenderScope.launch {
            val params = SendVerificationMessageTask.Params(event)
            sequencer.post {
                try {
                    val eventId = sendVerificationMessageTask.executeRetry(params, 5)
                    // Do I need to update local echo state to sent?
                    callback(eventId, validInfo)
                } catch (failure: Throwable) {
                    callback(null, null)
                }
            }
        }
    }

    override fun cancelTransaction(transactionId: String, otherUserId: String, otherUserDeviceId: String?, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val event = createEventAndLocalEcho(
                type = EventType.KEY_VERIFICATION_CANCEL,
                roomId = roomId,
                content = MessageVerificationCancelContent.create(transactionId, code).toContent()
        )

        verificationSenderScope.launch {
            sequencer.post {
                try {
                    val params = SendVerificationMessageTask.Params(event)
                    sendVerificationMessageTask.executeRetry(params, 5)
                } catch (failure: Throwable) {
                    Timber.w("")
                }
            }
        }
    }

    override fun done(transactionId: String,
                      onDone: (() -> Unit)?) {
        Timber.d("## SAS sending done for $transactionId")
        val event = createEventAndLocalEcho(
                type = EventType.KEY_VERIFICATION_DONE,
                roomId = roomId,
                content = MessageVerificationDoneContent(
                        relatesTo = RelationDefaultContent(
                                RelationType.REFERENCE,
                                transactionId
                        )
                ).toContent()
        )
        verificationSenderScope.launch {
            sequencer.post {
                try {
                    val params = SendVerificationMessageTask.Params(event)
                    sendVerificationMessageTask.executeRetry(params, 5)
                } catch (failure: Throwable) {
                    Timber.w("")
                } finally {
                    onDone?.invoke()
                }
            }
        }
//        val workerParams = WorkerParamsFactory.toData(SendVerificationMessageWorker.Params(
//                sessionId = sessionId,
//                eventId = event.eventId ?: ""
//        ))
//        val enqueueInfo = enqueueSendWork(workerParams)
//
//        val workLiveData = workManagerProvider.workManager
//                .getWorkInfosForUniqueWorkLiveData(uniqueQueueName())
//        val observer = object : Observer<List<WorkInfo>> {
//            override fun onChanged(workInfoList: List<WorkInfo>?) {
//                workInfoList
//                        ?.filter { it.state == WorkInfo.State.SUCCEEDED }
//                        ?.firstOrNull { it.id == enqueueInfo.second }
//                        ?.let { _ ->
//                            onDone?.invoke()
//                            workLiveData.removeObserver(this)
//                        }
//            }
//        }
//
//        // TODO listen to DB to get synced info
//        coroutineScope.launch(Dispatchers.Main) {
//            workLiveData.observeForever(observer)
//        }
    }

//    private fun enqueueSendWork(workerParams: Data): Pair<Operation, UUID> {
//        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<SendVerificationMessageWorker>()
//                .setConstraints(WorkManagerProvider.workConstraints)
//                .setInputData(workerParams)
//                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
//                .build()
//        return workManagerProvider.workManager
//                .beginUniqueWork(uniqueQueueName(), ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
//                .enqueue() to workRequest.id
//    }

//    private fun uniqueQueueName() = "${roomId}_VerificationWork"

    override fun createAccept(tid: String,
                              keyAgreementProtocol: String,
                              hash: String,
                              commitment: String,
                              messageAuthenticationCode: String,
                              shortAuthenticationStrings: List<String>): VerificationInfoAccept =
            MessageVerificationAcceptContent.create(
                    tid,
                    keyAgreementProtocol,
                    hash,
                    commitment,
                    messageAuthenticationCode,
                    shortAuthenticationStrings
            )

    override fun createKey(tid: String, pubKey: String): VerificationInfoKey = MessageVerificationKeyContent.create(tid, pubKey)

    override fun createMac(tid: String, mac: Map<String, String>, keys: String) = MessageVerificationMacContent.create(tid, mac, keys)

    override fun createStartForSas(fromDevice: String,
                                   transactionId: String,
                                   keyAgreementProtocols: List<String>,
                                   hashes: List<String>,
                                   messageAuthenticationCodes: List<String>,
                                   shortAuthenticationStrings: List<String>): VerificationInfoStart {
        return MessageVerificationStartContent(
                fromDevice,
                hashes,
                keyAgreementProtocols,
                messageAuthenticationCodes,
                shortAuthenticationStrings,
                VERIFICATION_METHOD_SAS,
                RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = transactionId
                ),
                null
        )
    }

    override fun createStartForQrCode(fromDevice: String,
                                      transactionId: String,
                                      sharedSecret: String): VerificationInfoStart {
        return MessageVerificationStartContent(
                fromDevice,
                null,
                null,
                null,
                null,
                VERIFICATION_METHOD_RECIPROCATE,
                RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = transactionId
                ),
                sharedSecret
        )
    }

    override fun createReady(tid: String, fromDevice: String, methods: List<String>): VerificationInfoReady {
        return MessageVerificationReadyContent(
                fromDevice = fromDevice,
                relatesTo = RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = tid
                ),
                methods = methods
        )
    }

    private fun createEventAndLocalEcho(localId: String = LocalEcho.createLocalEchoId(), type: String, roomId: String, content: Content): Event {
        return Event(
                roomId = roomId,
                originServerTs = System.currentTimeMillis(),
                senderId = userId,
                eventId = localId,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localId)
        ).also {
            localEchoEventFactory.createLocalEcho(it)
        }
    }

    override fun sendVerificationReady(keyReq: VerificationInfoReady,
                                       otherUserId: String,
                                       otherDeviceId: String?,
                                       callback: (() -> Unit)?) {
        // Not applicable (send event is called directly)
        Timber.w("## SAS ignored verification ready with methods: ${keyReq.methods}")
    }
}
