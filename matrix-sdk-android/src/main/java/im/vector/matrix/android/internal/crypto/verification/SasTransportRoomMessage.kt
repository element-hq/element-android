/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.matrix.android.internal.crypto.verification

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.*
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.R
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.events.model.*
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationStart
import im.vector.matrix.android.internal.session.room.send.LocalEchoEventFactory
import im.vector.matrix.android.internal.worker.WorkManagerUtil
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SasTransportRoomMessage(
        private val context: Context,
        private val userId: String,
        private val userDevice: String,
        private val roomId: String,
        private val monarchy: Monarchy,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val tx: SASVerificationTransaction?
) : SasTransport {

    private val listenerExecutor = Executors.newSingleThreadExecutor()

    override fun sendToOther(type: String,
                             verificationInfo: VerificationInfo,
                             nextState: SasVerificationTxState,
                             onErrorReason: CancelCode,
                             onDone: (() -> Unit)?) {
        Timber.d("## SAS sending msg type $type")
        Timber.v("## SAS sending msg info $verificationInfo")
        val event = createEventAndLocalEcho(
                type = type,
                roomId = roomId,
                content = verificationInfo.toEventContent()!!
        )

        val workerParams = WorkerParamsFactory.toData(SendVerificationMessageWorker.Params(
                userId = userId,
                event = event
        ))
        val enqueueInfo = enqueueSendWork(workerParams)

        // I cannot just listen to the given work request, because when used in a uniqueWork,
        // The callback is called while it is still Running ...

//        Futures.addCallback(enqueueInfo.first.result, object : FutureCallback<Operation.State.SUCCESS> {
//            override fun onSuccess(result: Operation.State.SUCCESS?) {
//                if (onDone != null) {
//                    onDone()
//                } else {
//                    tx?.state = nextState
//                }
//            }
//
//            override fun onFailure(t: Throwable) {
//                Timber.e("## SAS verification [${tx?.transactionId}] failed to send toDevice in state : ${tx?.state}, reason: ${t.localizedMessage}")
//                tx?.cancel(onErrorReason)
//            }
//        }, listenerExecutor)

        val workLiveData = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("${roomId}_VerificationWork")

        val observer = object : Observer<List<WorkInfo>> {
            override fun onChanged(workInfoList: List<WorkInfo>?) {
                workInfoList
                        ?.filter { it.state == WorkInfo.State.SUCCEEDED }
                        ?.firstOrNull { it.id == enqueueInfo.second }
                        ?.let { wInfo ->
                            if (wInfo.outputData.getBoolean("failed", false)) {
                                Timber.e("## SAS verification [${tx?.transactionId}] failed to send verification message in state : ${tx?.state}")
                                tx?.cancel(onErrorReason)
                            } else {
                                if (onDone != null) {
                                    onDone()
                                } else {
                                    tx?.state = nextState
                                }
                            }
                            workLiveData.removeObserver(this)
                        }
            }
        }

        // TODO listen to DB to get synced info
        GlobalScope.launch(Dispatchers.Main) {
            workLiveData.observeForever(observer)
        }

    }

    override fun sendVerificationRequest(localID: String, otherUserId: String, roomId: String, callback: (String?, MessageVerificationRequestContent?) -> Unit) {
        val info = MessageVerificationRequestContent(
                body = context.getString(R.string.key_verification_request_fallback_message, userId),
                fromDevice = userDevice,
                toUserId = otherUserId,
                methods = listOf(KeyVerificationStart.VERIF_METHOD_SAS)
        )
        val content = info.toContent()

        val event = createEventAndLocalEcho(
                localID,
                EventType.MESSAGE,
                roomId,
                content
        )

        val workerParams = WorkerParamsFactory.toData(SendVerificationMessageWorker.Params(
                userId = userId,
                event = event
        ))

        val workRequest = WorkManagerUtil.matrixOneTimeWorkRequestBuilder<SendVerificationMessageWorker>()
                .setConstraints(WorkManagerUtil.workConstraints)
                .setInputData(workerParams)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 2_000L, TimeUnit.MILLISECONDS)
                .build()

        WorkManager.getInstance(context)
                .beginUniqueWork("${roomId}_VerificationWork", ExistingWorkPolicy.APPEND, workRequest)
                .enqueue()

        // I cannot just listen to the given work request, because when used in a uniqueWork,
        // The callback is called while it is still Running ...

        val workLiveData = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("${roomId}_VerificationWork")

        val observer = object : Observer<List<WorkInfo>> {
            override fun onChanged(workInfoList: List<WorkInfo>?) {
                workInfoList
                        ?.filter { it.state == WorkInfo.State.SUCCEEDED }
                        ?.firstOrNull { it.id == workRequest.id }
                        ?.let { wInfo ->
                            if (wInfo.outputData.getBoolean("failed", false)) {
                                callback(null, null)
                            } else if (wInfo.outputData.getString(localID) != null) {
                                callback(wInfo.outputData.getString(localID), info)
                            } else {
                                callback(null, null)
                            }
                            workLiveData.removeObserver(this)
                        }
            }
        }

        // TODO listen to DB to get synced info
        GlobalScope.launch(Dispatchers.Main) {
            workLiveData.observeForever(observer)
        }
    }

    override fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val event = createEventAndLocalEcho(
                type = EventType.KEY_VERIFICATION_CANCEL,
                roomId = roomId,
                content = MessageVerificationCancelContent.create(transactionId, code).toContent()
        )
        val workerParams = WorkerParamsFactory.toData(SendVerificationMessageWorker.Params(
                userId = userId,
                event = event
        ))
        enqueueSendWork(workerParams)
    }

    override fun done(transactionId: String) {
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
        val workerParams = WorkerParamsFactory.toData(SendVerificationMessageWorker.Params(
                userId = userId,
                event = event
        ))
        enqueueSendWork(workerParams)
    }

    private fun enqueueSendWork(workerParams: Data): Pair<Operation, UUID> {
        val workRequest = WorkManagerUtil.matrixOneTimeWorkRequestBuilder<SendVerificationMessageWorker>()
                .setConstraints(WorkManagerUtil.workConstraints)
                .setInputData(workerParams)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 2_000L, TimeUnit.MILLISECONDS)
                .build()
        return WorkManager.getInstance(context)
                .beginUniqueWork("${roomId}_VerificationWork", ExistingWorkPolicy.APPEND, workRequest)
                .enqueue() to workRequest.id
    }

    override fun createAccept(tid: String,
                              keyAgreementProtocol: String,
                              hash: String,
                              commitment: String,
                              messageAuthenticationCode: String,
                              shortAuthenticationStrings: List<String>)
            : VerificationInfoAccept = MessageVerificationAcceptContent.create(
            tid,
            keyAgreementProtocol,
            hash,
            commitment,
            messageAuthenticationCode,
            shortAuthenticationStrings
    )

    override fun createKey(tid: String, pubKey: String): VerificationInfoKey = MessageVerificationKeyContent.create(tid, pubKey)

    override fun createMac(tid: String, mac: Map<String, String>, keys: String) = MessageVerificationMacContent.create(tid, mac, keys)

    override fun createStart(fromDevice: String,
                             method: String,
                             transactionID: String,
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
                method,
                RelationDefaultContent(
                        type = RelationType.REFERENCE,
                        eventId = transactionID
                )
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

    private fun createEventAndLocalEcho(localID: String = LocalEcho.createLocalEchoId(), type: String, roomId: String, content: Content): Event {
        return Event(
                roomId = roomId,
                originServerTs = System.currentTimeMillis(),
                senderId = userId,
                eventId = localID,
                type = type,
                content = content,
                unsignedData = UnsignedData(age = null, transactionId = localID)
        ).also {
            localEchoEventFactory.saveLocalEcho(monarchy, it)
        }
    }
}

internal class SasTransportRoomMessageFactory @Inject constructor(
        private val monarchy: Monarchy,
        private val localEchoEventFactory: LocalEchoEventFactory) {

    fun createTransport(context: Context, userId: String, userDevice: String, roomId: String, tx: SASVerificationTransaction?
    ): SasTransportRoomMessage {
        return SasTransportRoomMessage(context, userId, userDevice, roomId, monarchy, localEchoEventFactory, tx)
    }
}
