/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms.megolm

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.android.internal.crypto.model.event.RoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.ForwardedRoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class MXMegolmDecryption(private val credentials: Credentials,
                                  private val olmDevice: MXOlmDevice,
                                  private val deviceListManager: DeviceListManager,
                                  private val outgoingRoomKeyRequestManager: OutgoingRoomKeyRequestManager,
                                  private val messageEncrypter: MessageEncrypter,
                                  private val ensureOlmSessionsForDevicesAction: EnsureOlmSessionsForDevicesAction,
                                  private val cryptoStore: IMXCryptoStore,
                                  private val sendToDeviceTask: SendToDeviceTask,
                                  private val coroutineDispatchers: MatrixCoroutineDispatchers)
    : IMXDecrypting {

    var newSessionListener: NewSessionListener? = null

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
    private var pendingEvents: MutableMap<String /* senderKey|sessionId */, MutableMap<String /* timelineId */, MutableList<Event>>> = HashMap()

    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        return decryptEvent(event, timeline, true)
    }

    private suspend fun decryptEvent(event: Event, timeline: String, requestKeysOnFail: Boolean): MXEventDecryptionResult {
        if (event.roomId.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)

        if (encryptedEventContent.senderKey.isNullOrBlank()
                || encryptedEventContent.sessionId.isNullOrBlank()
                || encryptedEventContent.ciphertext.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        return runCatching {
            olmDevice.decryptGroupMessage(encryptedEventContent.ciphertext,
                    event.roomId,
                    timeline,
                    encryptedEventContent.sessionId,
                    encryptedEventContent.senderKey)
        }
                .fold(
                        { olmDecryptionResult ->
                            // the decryption succeeds
                            if (olmDecryptionResult.payload != null) {
                                MXEventDecryptionResult(
                                        clearEvent = olmDecryptionResult.payload,
                                        senderCurve25519Key = olmDecryptionResult.senderKey,
                                        claimedEd25519Key = olmDecryptionResult.keysClaimed?.get("ed25519"),
                                        forwardingCurve25519KeyChain = olmDecryptionResult.forwardingCurve25519KeyChain
                                                ?: emptyList()
                                )
                            } else {
                                throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
                            }
                        },
                        { throwable ->
                            if (throwable is MXCryptoError.OlmError) {
                                // TODO Check the value of .message
                                if (throwable.olmException.message == "UNKNOWN_MESSAGE_INDEX") {
                                    addEventToPendingList(event, timeline)
                                    if (requestKeysOnFail) {
                                        requestKeysForEvent(event)
                                    }
                                }

                                val reason = String.format(MXCryptoError.OLM_REASON, throwable.olmException.message)
                                val detailedReason = String.format(MXCryptoError.DETAILED_OLM_REASON, encryptedEventContent.ciphertext, reason)

                                throw MXCryptoError.Base(
                                        MXCryptoError.ErrorType.OLM,
                                        reason,
                                        detailedReason)
                            }
                            if (throwable is MXCryptoError.Base) {
                                if (throwable.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                                    addEventToPendingList(event, timeline)
                                    if (requestKeysOnFail) {
                                        requestKeysForEvent(event)
                                    }
                                }
                            }
                            throw throwable
                        }
                )
    }


    /**
     * Helper for the real decryptEvent and for _retryDecryption. If
     * requestKeysOnFail is true, we'll send an m.room_key_request when we fail
     * to decrypt the event due to missing megolm keys.
     *
     * @param event the event
     */
    private fun requestKeysForEvent(event: Event) {
        val sender = event.senderId!!
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()!!

        val recipients = ArrayList<Map<String, String>>()

        val selfMap = HashMap<String, String>()
        // TODO Replace this hard coded keys (see OutgoingRoomKeyRequestManager)
        selfMap["userId"] = credentials.userId
        selfMap["deviceId"] = "*"
        recipients.add(selfMap)

        if (!TextUtils.equals(sender, credentials.userId)) {
            val senderMap = HashMap<String, String>()
            senderMap["userId"] = sender
            senderMap["deviceId"] = encryptedEventContent.deviceId!!
            recipients.add(senderMap)
        }

        val requestBody = RoomKeyRequestBody()

        requestBody.roomId = event.roomId
        requestBody.algorithm = encryptedEventContent.algorithm
        requestBody.senderKey = encryptedEventContent.senderKey
        requestBody.sessionId = encryptedEventContent.sessionId

        outgoingRoomKeyRequestManager.sendRoomKeyRequest(requestBody, recipients)
    }

    /**
     * Add an event to the list of those we couldn't decrypt the first time we
     * saw them.
     *
     * @param event      the event to try to decrypt later
     * @param timelineId the timeline identifier
     */
    private fun addEventToPendingList(event: Event, timelineId: String) {
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>() ?: return
        val pendingEventsKey = "${encryptedEventContent.senderKey}|${encryptedEventContent.sessionId}"


        if (!pendingEvents.containsKey(pendingEventsKey)) {
            pendingEvents[pendingEventsKey] = HashMap()
        }

        if (pendingEvents[pendingEventsKey]?.containsKey(timelineId) == false) {
            pendingEvents[pendingEventsKey]?.put(timelineId, ArrayList())
        }

        if (pendingEvents[pendingEventsKey]?.get(timelineId)?.contains(event) == false) {
            Timber.v("## addEventToPendingList() : add Event " + event.eventId + " in room id " + event.roomId)
            pendingEvents[pendingEventsKey]?.get(timelineId)?.add(event)
        }
    }

    /**
     * Handle a key event.
     *
     * @param event the key event.
     */
    override fun onRoomKeyEvent(event: Event, keysBackup: KeysBackup) {
        var exportFormat = false
        val roomKeyContent = event.getClearContent().toModel<RoomKeyContent>() ?: return

        var senderKey: String? = event.getSenderKey()
        var keysClaimed: MutableMap<String, String> = HashMap()
        val forwardingCurve25519KeyChain: MutableList<String> = ArrayList()

        if (TextUtils.isEmpty(roomKeyContent.roomId) || TextUtils.isEmpty(roomKeyContent.sessionId) || TextUtils.isEmpty(roomKeyContent.sessionKey)) {
            Timber.e("## onRoomKeyEvent() :  Key event is missing fields")
            return
        }
        if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
            Timber.v("## onRoomKeyEvent(), forward adding key : roomId ${roomKeyContent.roomId}" +
                    " sessionId ${roomKeyContent.sessionId} sessionKey ${roomKeyContent.sessionKey}")
            val forwardedRoomKeyContent = event.getClearContent().toModel<ForwardedRoomKeyContent>()
                    ?: return

            forwardedRoomKeyContent.forwardingCurve25519KeyChain?.let {
                forwardingCurve25519KeyChain.addAll(it)
            }

            if (senderKey == null) {
                Timber.e("## onRoomKeyEvent() : event is missing sender_key field")
                return
            }

            forwardingCurve25519KeyChain.add(senderKey)

            exportFormat = true
            senderKey = forwardedRoomKeyContent.senderKey
            if (null == senderKey) {
                Timber.e("## onRoomKeyEvent() : forwarded_room_key event is missing sender_key field")
                return
            }

            if (null == forwardedRoomKeyContent.senderClaimedEd25519Key) {
                Timber.e("## forwarded_room_key_event is missing sender_claimed_ed25519_key field")
                return
            }

            keysClaimed["ed25519"] = forwardedRoomKeyContent.senderClaimedEd25519Key
        } else {
            Timber.v("## onRoomKeyEvent(), Adding key : roomId " + roomKeyContent.roomId + " sessionId " + roomKeyContent.sessionId
                    + " sessionKey " + roomKeyContent.sessionKey) // from " + event);

            if (null == senderKey) {
                Timber.e("## onRoomKeyEvent() : key event has no sender key (not encrypted?)")
                return
            }

            // inherit the claimed ed25519 key from the setup message
            keysClaimed = event.getKeysClaimed().toMutableMap()
        }

        if (roomKeyContent.sessionId == null
                || roomKeyContent.sessionKey == null
                || roomKeyContent.roomId == null) {
            Timber.e("## invalid roomKeyContent")
            return
        }

        val added = olmDevice.addInboundGroupSession(roomKeyContent.sessionId,
                roomKeyContent.sessionKey,
                roomKeyContent.roomId,
                senderKey,
                forwardingCurve25519KeyChain,
                keysClaimed,
                exportFormat)

        if (added) {
            keysBackup.maybeBackupKeys()

            val content = RoomKeyRequestBody()

            content.algorithm = roomKeyContent.algorithm
            content.roomId = roomKeyContent.roomId
            content.sessionId = roomKeyContent.sessionId
            content.senderKey = senderKey

            outgoingRoomKeyRequestManager.cancelRoomKeyRequest(content)

            onNewSession(senderKey, roomKeyContent.sessionId)
        }
    }

    /**
     * Check if the some messages can be decrypted with a new session
     *
     * @param senderKey the session sender key
     * @param sessionId the session id
     */
    override fun onNewSession(senderKey: String, sessionId: String) {
        Timber.v("ON NEW SESSION $sessionId - $senderKey")
        newSessionListener?.onNewSession(null, senderKey, sessionId)
    }

    override fun hasKeysForKeyRequest(request: IncomingRoomKeyRequest): Boolean {
        val roomId = request.requestBody?.roomId ?: return false
        val senderKey = request.requestBody?.senderKey ?: return false
        val sessionId = request.requestBody?.sessionId ?: return false
        return olmDevice.hasInboundSessionKeys(roomId, senderKey, sessionId)
    }

    override fun shareKeysWithDevice(request: IncomingRoomKeyRequest) {
        // sanity checks
        if (request.requestBody == null) {
            return
        }
        val userId = request.userId ?: return
        GlobalScope.launch(coroutineDispatchers.crypto) {
            runCatching { deviceListManager.downloadKeys(listOf(userId), false) }
                    .mapCatching {
                        val deviceId = request.deviceId
                        val deviceInfo = cryptoStore.getUserDevice(deviceId ?: "", userId)
                        if (deviceInfo == null) {
                            throw RuntimeException()
                        } else {
                            val devicesByUser = mapOf(userId to listOf(deviceInfo))
                            val usersDeviceMap = ensureOlmSessionsForDevicesAction.handle(devicesByUser)
                            val body = request.requestBody
                            val olmSessionResult = usersDeviceMap.getObject(userId, deviceId)
                            if (olmSessionResult?.sessionId == null) {
                                // no session with this device, probably because there
                                // were no one-time keys.
                                return@mapCatching
                            }
                            Timber.v("## shareKeysWithDevice() : sharing keys for session" +
                                    " ${body?.senderKey}|${body?.sessionId} with device $userId:$deviceId")

                            val payloadJson = mutableMapOf<String, Any>("type" to EventType.FORWARDED_ROOM_KEY)
                            runCatching { olmDevice.getInboundGroupSession(body?.sessionId, body?.senderKey, body?.roomId) }
                                    .fold(
                                            {
                                                // TODO
                                                payloadJson["content"] = it.exportKeys()
                                                        ?: ""
                                            },
                                            {
                                                // TODO
                                            }

                                    )

                            val encodedPayload = messageEncrypter.encryptMessage(payloadJson, listOf(deviceInfo))
                            val sendToDeviceMap = MXUsersDevicesMap<Any>()
                            sendToDeviceMap.setObject(userId, deviceId, encodedPayload)
                            Timber.v("## shareKeysWithDevice() : sending to $userId:$deviceId")
                            val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
                            sendToDeviceTask.execute(sendToDeviceParams)
                        }
                    }
        }
    }
}

