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
import im.vector.matrix.android.api.session.crypto.sas.CancelCode
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationAccept
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationCancel
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationKey
import im.vector.matrix.android.internal.crypto.model.rest.KeyVerificationMac
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

internal class SasTransportToDevice(
        private var tx: SASVerificationTransaction?,
        private var sendToDeviceTask: SendToDeviceTask,
        private var taskExecutor: TaskExecutor
) : SasTransport {

    override fun sendToOther(type: String,
                             verificationInfo: VerificationInfo,
                             nextState: SasVerificationTxState,
                             onErrorReason: CancelCode,
                             onDone: (() -> Unit)?) {
        Timber.d("## SAS sending msg type $type")
        Timber.v("## SAS sending msg info $verificationInfo")
        val tx = tx ?: return
        val contentMap = MXUsersDevicesMap<Any>()
        val toSendToDeviceObject = verificationInfo.toSendToDeviceObject()
                ?: return Unit.also { tx.cancel() }

        contentMap.setObject(tx.otherUserId, tx.otherDeviceId, toSendToDeviceObject)

        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(type, contentMap, tx.transactionId)) {
                    this.callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.v("## SAS verification [$tx.transactionId] toDevice type '$type' success.")
                            if (onDone != null) {
                                onDone()
                            } else {
                                tx.state = nextState
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

    override fun done(transactionId: String) {
        // To device do not do anything here
    }

    override fun cancelTransaction(transactionId: String, userId: String, userDevice: String, code: CancelCode) {
        Timber.d("## SAS canceling transaction $transactionId for reason $code")
        val cancelMessage = KeyVerificationCancel.create(transactionId, code)
        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(userId, userDevice, cancelMessage)
        sendToDeviceTask
                .configureWith(SendToDeviceTask.Params(EventType.KEY_VERIFICATION_CANCEL, contentMap, transactionId)) {
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
                              shortAuthenticationStrings: List<String>)
            : VerificationInfoAccept = KeyVerificationAccept.create(
            tid,
            keyAgreementProtocol,
            hash,
            commitment,
            messageAuthenticationCode,
            shortAuthenticationStrings)

    override fun createKey(tid: String, pubKey: String): VerificationInfoKey = KeyVerificationKey.create(tid, pubKey)

    override fun createMac(tid: String, mac: Map<String, String>, keys: String) = KeyVerificationMac.create(tid, mac, keys)
}

internal class SasTransportToDeviceFactory @Inject constructor(
        private val sendToDeviceTask: SendToDeviceTask,
        private val taskExecutor: TaskExecutor) {

    fun createTransport(tx: SASVerificationTransaction?): SasTransportToDevice {
        return SasTransportToDevice(tx, sendToDeviceTask, taskExecutor)
    }
}
