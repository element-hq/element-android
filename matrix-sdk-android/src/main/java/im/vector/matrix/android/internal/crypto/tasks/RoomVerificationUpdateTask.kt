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

package im.vector.matrix.android.internal.crypto.tasks

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.*
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.crypto.verification.DefaultSasVerificationService
import im.vector.matrix.android.internal.di.DeviceId
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.task.Task
import timber.log.Timber
import java.util.*
import javax.inject.Inject

internal interface RoomVerificationUpdateTask : Task<RoomVerificationUpdateTask.Params, Unit> {
    data class Params(
            val events: List<Event>,
            val sasVerificationService: DefaultSasVerificationService,
            val cryptoService: CryptoService
    )
}

internal class DefaultRoomVerificationUpdateTask @Inject constructor(
        @UserId private val userId: String,
        @DeviceId private val deviceId: String?,
        private val cryptoService: CryptoService) : RoomVerificationUpdateTask {

    companion object {
        // XXX what about multi-account?
        private val transactionsHandledByOtherDevice = ArrayList<String>()
    }

    override suspend fun execute(params: RoomVerificationUpdateTask.Params) {
        // TODO ignore initial sync or back pagination?

        params.events.forEach { event ->
            Timber.d("## SAS Verification live observer: received msgId: ${event.eventId} msgtype: ${event.type} from ${event.senderId}")
            Timber.v("## SAS Verification live observer: received msgId: $event")

            // If the request is in the future by more than 5 minutes or more than 10 minutes in the past,
            // the message should be ignored by the receiver.

            if (!SasVerificationService.isValidRequest(event.ageLocalTs
                            ?: event.originServerTs)) return@forEach Unit.also {
                Timber.d("## SAS Verification live observer: msgId: ${event.eventId} is outdated")
            }

            // decrypt if needed?
            if (event.isEncrypted() && event.mxDecryptionResult == null) {
                // TODO use a global event decryptor? attache to session and that listen to new sessionId?
                // for now decrypt sync
                try {
                    val result = cryptoService.decryptEvent(event, event.roomId + UUID.randomUUID().toString())
                    event.mxDecryptionResult = OlmDecryptionResult(
                            payload = result.clearEvent,
                            senderKey = result.senderCurve25519Key,
                            keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                            forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                    )
                } catch (e: MXCryptoError) {
                    Timber.e("## SAS Failed to decrypt event: ${event.eventId}")
                }
            }
            Timber.v("## SAS Verification live observer: received msgId: ${event.eventId} type: ${event.getClearType()}")

            if (event.senderId == userId) {
                // If it's send from me, we need to keep track of Requests or Start
                // done from another device of mine

                if (EventType.MESSAGE == event.type) {
                    val msgType = event.getClearContent().toModel<MessageContent>()?.type
                    if (MessageType.MSGTYPE_VERIFICATION_REQUEST == msgType) {
                        event.getClearContent().toModel<MessageVerificationRequestContent>()?.let {
                            if (it.fromDevice != deviceId) {
                                // The verification is requested from another device
                                Timber.v("## SAS Verification live observer: Transaction requested from other device  tid:${event.eventId} ")
                                event.eventId?.let { txId -> transactionsHandledByOtherDevice.add(txId) }
                            }
                        }
                    }
                } else if (EventType.KEY_VERIFICATION_START == event.type) {
                    event.getClearContent().toModel<MessageVerificationStartContent>()?.let {
                        if (it.fromDevice != deviceId) {
                            // The verification is started from another device
                            Timber.v("## SAS Verification live observer: Transaction started by other device  tid:${it.transactionID} ")
                            it.transactionID?.let { txId -> transactionsHandledByOtherDevice.add(txId) }
                        }
                    }
                } else if (EventType.KEY_VERIFICATION_READY == event.type) {
                    event.getClearContent().toModel<MessageVerificationReadyContent>()?.let {
                        if (it.fromDevice != deviceId) {
                            // The verification is started from another device
                            Timber.v("## SAS Verification live observer: Transaction started by other device  tid:${it.transactionID} ")
                            it.transactionID?.let { txId -> transactionsHandledByOtherDevice.add(txId) }
                        }
                    }
                } else if (EventType.KEY_VERIFICATION_CANCEL == event.type || EventType.KEY_VERIFICATION_DONE == event.type) {
                    event.getClearContent().toModel<MessageRelationContent>()?.relatesTo?.eventId?.let {
                        transactionsHandledByOtherDevice.remove(it)
                    }
                }

                Timber.v("## SAS Verification ignoring message sent by me: ${event.eventId} type: ${event.getClearType()}")
                return@forEach
            }

            val relatesTo = event.getClearContent().toModel<MessageRelationContent>()?.relatesTo?.eventId
            if (relatesTo != null && transactionsHandledByOtherDevice.contains(relatesTo)) {
                // Ignore this event, it is directed to another of my devices
                Timber.v("## SAS Verification live observer: Ignore Transaction handled by other device  tid:$relatesTo ")
                return@forEach
            }
            when (event.getClearType()) {
                EventType.KEY_VERIFICATION_START,
                EventType.KEY_VERIFICATION_ACCEPT,
                EventType.KEY_VERIFICATION_KEY,
                EventType.KEY_VERIFICATION_MAC,
                EventType.KEY_VERIFICATION_CANCEL,
                EventType.KEY_VERIFICATION_READY,
                EventType.KEY_VERIFICATION_DONE -> {
                    params.sasVerificationService.onRoomEvent(event)
                }
                EventType.MESSAGE -> {
                    if (MessageType.MSGTYPE_VERIFICATION_REQUEST == event.getClearContent().toModel<MessageContent>()?.type) {
                        params.sasVerificationService.onRoomRequestReceived(event)
                    }
                }
            }
        }
    }
}
