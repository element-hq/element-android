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

import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.ValidVerificationInfoRequest
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationAccept
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationCancel
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationDone
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationMac
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationReady
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationRequest
import org.matrix.android.sdk.internal.crypto.model.rest.KeyVerificationStart
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_RECIPROCATE
import org.matrix.android.sdk.internal.crypto.model.rest.VERIFICATION_METHOD_SAS
import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber

internal class VerificationTransportToDevice(
        private var tx: DefaultVerificationTransaction?,
        private var sendToDeviceTask: SendToDeviceTask,
        private val myDeviceId: String?,
        private var taskExecutor: TaskExecutor
) : VerificationTransport {

    override fun sendVerificationRequest(supportedMethods: List<String>,
                                         localId: String,
                                         otherUserId: String,
                                         roomId: String?,
                                         toDevices: List<String>?,
                                         callback: (String?, ValidVerificationInfoRequest?) -> Unit) {
        Timber.d("## SAS sending verification request with supported methods: $supportedMethods")
        val contentMap = MXUsersDevicesMap<Any>()
        val validKeyReq = ValidVerificationInfoRequest(
                transactionId = localId,
                fromDevice = myDeviceId ?: "",
                methods = supportedMethods,
                timestamp = System.currentTimeMillis()
        )
        val keyReq = KeyVerificationRequest(
                fromDevice = validKeyReq.fromDevice,
                methods = validKeyReq.methods,
                timestamp = validKeyReq.timestamp,
                transactionId = validKeyReq.transactionId
        )
        toDevices?.forEach {
            contentMap.setObject(otherUserId, it, keyReq)
        }
        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(MessageType.MSGTYPE_VERIFICATION_REQUEST, contentMap)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.v("## verification [$tx.transactionId] send toDevice request success")
                            callback.invoke(localId, validKeyReq)
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e("## verification [$tx.transactionId] failed to send toDevice request")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun sendVerificationReady(keyReq: VerificationInfoReady,
                                       otherUserId: String,
                                       otherDeviceId: String?,
                                       callback: (() -> Unit)?) {
        Timber.d("## SAS sending verification ready with methods: ${keyReq.methods}")
        val contentMap = MXUsersDevicesMap<Any>()

        contentMap.setObject(otherUserId, otherDeviceId, keyReq)

        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_READY, contentMap)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.v("## verification [$tx.transactionId] send toDevice request success")
                            callback?.invoke()
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e("## verification [$tx.transactionId] failed to send toDevice request")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun <T> sendToOther(type: String,
                                 verificationInfo: VerificationInfo<T>,
                                 nextState: VerificationTxState,
                                 onErrorReason: CancelCode,
                                 onDone: (() -> Unit)?) {
        Timber.d("## SAS sending msg type $type")
        Timber.v("## SAS sending msg info $verificationInfo")
        val stateBeforeCall = tx?.state
        val tx = tx ?: return
        val contentMap = MXUsersDevicesMap<Any>()
        val toSendToDeviceObject = verificationInfo.toSendToDeviceObject()
                ?: return Unit.also { tx.cancel() }

        contentMap.setObject(tx.otherUserId, tx.otherDeviceId, toSendToDeviceObject)

        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(type, contentMap)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.v("## SAS verification [$tx.transactionId] toDevice type '$type' success.")
                            if (onDone != null) {
                                onDone()
                            } else {
                                // we may have received next state (e.g received accept in sending_start)
                                // We only put next state if the state was what is was before we started
                                if (tx.state == stateBeforeCall) {
                                    tx.state = nextState
                                }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e("## SAS verification [$tx.transactionId] failed to send toDevice in state : $tx.state")
                            tx.cancel(onErrorReason)
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun done(transactionId: String, onDone: (() -> Unit)?) {
        val otherUserId = tx?.otherUserId ?: return
        val otherUserDeviceId = tx?.otherDeviceId ?: return
        val cancelMessage = KeyVerificationDone(transactionId)
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(otherUserId, otherUserDeviceId, cancelMessage)
        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_DONE, contentMap)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            onDone?.invoke()
                            Timber.v("## SAS verification [$transactionId] done")
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "## SAS verification [$transactionId] failed to done.")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun cancelTransaction(transactionId: String, otherUserId: String, otherUserDeviceId: String?, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(otherUserId, otherUserDeviceId, cancelMessage)
        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.v("## SAS verification [$transactionId] canceled for reason ${code.value}")
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "## SAS verification [$transactionId] failed to cancel.")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    override fun createAccept(tid: String,
                              keyAgreementProtocol: String,
                              hash: String,
                              commitment: String,
                              messageAuthenticationCode: String,
                              shortAuthenticationStrings: List<String>): VerificationInfoAccept = KeyVerificationAccept.create(
            tid,
            keyAgreementProtocol,
            hash,
            commitment,
            messageAuthenticationCode,
            shortAuthenticationStrings)

    override fun createKey(tid: String, pubKey: String): VerificationInfoKey = KeyVerificationKey.create(tid, pubKey)

    override fun createMac(tid: String, mac: Map<String, String>, keys: String) = KeyVerificationMac.create(tid, mac, keys)

    override fun createStartForSas(fromDevice: String,
                                   transactionId: String,
                                   keyAgreementProtocols: List<String>,
                                   hashes: List<String>,
                                   messageAuthenticationCodes: List<String>,
                                   shortAuthenticationStrings: List<String>): VerificationInfoStart {
        return KeyVerificationStart(
                fromDevice,
                VERIFICATION_METHOD_SAS,
                transactionId,
                keyAgreementProtocols,
                hashes,
                messageAuthenticationCodes,
                shortAuthenticationStrings,
                null)
    }

    override fun createStartForQrCode(fromDevice: String,
                                      transactionId: String,
                                      sharedSecret: String): VerificationInfoStart {
        return KeyVerificationStart(
                fromDevice,
                VERIFICATION_METHOD_RECIPROCATE,
                transactionId,
                null,
                null,
                null,
                null,
                sharedSecret)
    }

    override fun createReady(tid: String, fromDevice: String, methods: List<String>): VerificationInfoReady {
        return KeyVerificationReady(
                transactionId = tid,
                fromDevice = fromDevice,
                methods = methods
        )
    }
}
