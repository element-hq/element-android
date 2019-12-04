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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.RelationType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.api.session.room.model.relation.RelationDefaultContent
import im.vector.matrix.android.internal.crypto.tasks.DefaultSendVerificationMessageTask
import im.vector.matrix.android.internal.crypto.tasks.SendVerificationMessageTask
import im.vector.matrix.android.internal.session.room.send.SendResponse
import im.vector.matrix.android.internal.task.TaskConstraints
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

internal class SasTransportRoomMessage(
        private val roomId: String,
        private val cryptoService: CryptoService,
//        private val tx: SASVerificationTransaction?,
        private val sendVerificationMessageTask: SendVerificationMessageTask,
        private val taskExecutor: TaskExecutor
) : SasTransport {

    override fun sendToOther(type: String,
                             verificationInfo: VerificationInfo,
                             nextState: SasVerificationTxState,
                             onErrorReason: CancelCode,
                             onDone: (() -> Unit)?) {
        Timber.d("## SAS sending msg type $type")
        Timber.v("## SAS sending msg info $verificationInfo")
        sendVerificationMessageTask.configureWith(
                SendVerificationMessageTask.Params(
                        type,
                        roomId,
                        verificationInfo.toEventContent()!!,
                        cryptoService
                )
        ) {
            constraints = TaskConstraints(true)
            retryCount = 3
        }
                .executeBy(taskExecutor)
    }

    override fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        sendVerificationMessageTask.configureWith(
                SendVerificationMessageTask.Params(
                        EventType.KEY_VERIFICATION_CANCEL,
                        roomId,
                        MessageVerificationCancelContent.create(transactionId, code).toContent(),
                        cryptoService
                )
        ) {
            constraints = TaskConstraints(true)
            retryCount = 3
            callback = object : MatrixCallback<SendResponse> {
                override fun onSuccess(data: SendResponse) {
                    Timber.v("## SAS verification [$transactionId] canceled for reason ${code.value}")
                }

                override fun onFailure(failure: Throwable) {
                    Timber.e(failure, "## SAS verification [$transactionId] failed to cancel.")
                }
            }
        }
                .executeBy(taskExecutor)
    }

    override fun done(transactionId: String) {
        sendVerificationMessageTask.configureWith(
                SendVerificationMessageTask.Params(
                        EventType.KEY_VERIFICATION_DONE,
                        roomId,
                        MessageVerificationDoneContent(
                                relatesTo = RelationDefaultContent(
                                        RelationType.REFERENCE,
                                        transactionId
                                )
                        ).toContent(),
                        cryptoService
                )
        ) {
            constraints = TaskConstraints(true)
            retryCount = 3
        }
                .executeBy(taskExecutor)
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
}

internal class SasTransportRoomMessageFactory @Inject constructor(
        private val sendVerificationMessageTask: DefaultSendVerificationMessageTask,
        private val taskExecutor: TaskExecutor) {

    fun createTransport(roomId: String,
                        cryptoService: CryptoService
//                        tx: SASVerificationTransaction?
    ): SasTransportRoomMessage {
        return SasTransportRoomMessage(roomId, cryptoService, /*tx,*/ sendVerificationMessageTask, taskExecutor)
    }
}
