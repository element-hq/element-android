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
import arrow.core.Try
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForDevicesAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.algorithms.MXDecryptionResult
import im.vector.matrix.android.internal.crypto.keysbackup.KeysBackup
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.android.internal.crypto.model.event.RoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.ForwardedRoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

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

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
    private var pendingEvents: MutableMap<String /* senderKey|sessionId */, MutableMap<String /* timelineId */, MutableList<Event>>> = HashMap()

    @Throws(MXDecryptionException::class)
    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult? {
        return decryptEvent(event, timeline, true)
    }

    @Throws(MXDecryptionException::class)
    private fun decryptEvent(event: Event, timeline: String, requestKeysOnFail: Boolean): MXEventDecryptionResult? {
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()!!
        if (TextUtils.isEmpty(encryptedEventContent.senderKey) || TextUtils.isEmpty(encryptedEventContent.sessionId) || TextUtils.isEmpty(encryptedEventContent.ciphertext)) {
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_FIELDS_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_FIELDS_REASON))
        }

        var eventDecryptionResult: MXEventDecryptionResult? = null
        var cryptoError: MXCryptoError? = null
        var decryptGroupMessageResult: MXDecryptionResult? = null

        try {
            decryptGroupMessageResult = olmDevice.decryptGroupMessage(encryptedEventContent.ciphertext!!, event.roomId!!, timeline, encryptedEventContent.sessionId!!, encryptedEventContent.senderKey!!)
        } catch (e: MXDecryptionException) {
            cryptoError = e.cryptoError
        }
        // the decryption succeeds
        if (decryptGroupMessageResult?.payload != null && cryptoError == null) {
            eventDecryptionResult = MXEventDecryptionResult()

            eventDecryptionResult.clearEvent = decryptGroupMessageResult.payload
            eventDecryptionResult.senderCurve25519Key = decryptGroupMessageResult.senderKey

            if (null != decryptGroupMessageResult.keysClaimed) {
                eventDecryptionResult.claimedEd25519Key = decryptGroupMessageResult.keysClaimed!!["ed25519"]
            }

            eventDecryptionResult.forwardingCurve25519KeyChain = decryptGroupMessageResult.forwardingCurve25519KeyChain!!
        } else if (cryptoError != null) {
            if (cryptoError.isOlmError) {
                if (MXCryptoError.UNKNOWN_MESSAGE_INDEX == cryptoError.message) {
                    addEventToPendingList(event, timeline)
                    if (requestKeysOnFail) {
                        requestKeysForEvent(event)
                    }
                }

                val reason = String.format(MXCryptoError.OLM_REASON, cryptoError.message)
                val detailedReason = String.format(MXCryptoError.DETAILLED_OLM_REASON, encryptedEventContent.ciphertext, cryptoError.message)

                throw MXDecryptionException(MXCryptoError(
                        MXCryptoError.OLM_ERROR_CODE,
                        reason,
                        detailedReason))
            } else if (TextUtils.equals(cryptoError.code, MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE)) {
                addEventToPendingList(event, timeline)
                if (requestKeysOnFail) {
                    requestKeysForEvent(event)
                }
            }
            throw MXDecryptionException(cryptoError)
        }

        return eventDecryptionResult
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
        selfMap["userId"] = credentials.userId // TODO Replace this hard coded keys (see OutgoingRoomKeyRequestManager)
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

        if (!pendingEvents[pendingEventsKey]!!.containsKey(timelineId)) {
            pendingEvents[pendingEventsKey]!![timelineId] = ArrayList()
        }

        if (pendingEvents[pendingEventsKey]!![timelineId]!!.indexOf(event) < 0) {
            Timber.v("## addEventToPendingList() : add Event " + event.eventId + " in room id " + event.roomId)
            pendingEvents[pendingEventsKey]!![timelineId]!!.add(event)
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
        var forwardingCurve25519KeyChain: MutableList<String> = ArrayList()

        if (TextUtils.isEmpty(roomKeyContent.roomId) || TextUtils.isEmpty(roomKeyContent.sessionId) || TextUtils.isEmpty(roomKeyContent.sessionKey)) {
            Timber.e("## onRoomKeyEvent() :  Key event is missing fields")
            return
        }
        if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
            Timber.v("## onRoomKeyEvent(), forward adding key : roomId " + roomKeyContent.roomId + " sessionId " + roomKeyContent.sessionId
                    + " sessionKey " + roomKeyContent.sessionKey) // from " + event);
            val forwardedRoomKeyContent = event.getClearContent().toModel<ForwardedRoomKeyContent>() ?: return
            forwardingCurve25519KeyChain = if (forwardedRoomKeyContent.forwardingCurve25519KeyChain == null) {
                ArrayList()
            } else {
                ArrayList(forwardedRoomKeyContent.forwardingCurve25519KeyChain)
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

        val added = olmDevice.addInboundGroupSession(roomKeyContent.sessionId, roomKeyContent.sessionKey, roomKeyContent.roomId, senderKey, forwardingCurve25519KeyChain, keysClaimed, exportFormat)

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
        //TODO see how to handle this
        Timber.v("ON NEW SESSION $sessionId - $senderKey")
        /*val k = "$senderKey|$sessionId"

        val pending = pendingEvents[k]

        if (null != pending) {
            // Have another go at decrypting events sent with this session.
            pendingEvents.remove(k)

            val timelineIds = pending.keys

            for (timelineId in timelineIds) {
                val events = pending[timelineId]

                for (event in events!!) {
                    var result: MXEventDecryptionResult? = null

                    try {
                        result = decryptEvent(event, timelineId)
                    } catch (e: MXDecryptionException) {
                        Timber.e(e, "## onNewSession() : Still can't decrypt " + event.eventId + ". Error")
                        event.setCryptoError(e.cryptoError)
                    }

                    if (null != result) {
                        val fResut = result
                        CryptoAsyncHelper.getUiHandler().post {
                            event.setClearData(fResut)
                            //mSession!!.onEventDecrypted(event)
                        }
                        Timber.v("## onNewSession() : successful re-decryption of " + event.eventId)
                    }
                }
            }
        }
        */
    }

    override fun hasKeysForKeyRequest(request: IncomingRoomKeyRequest): Boolean {
        return (null != request.requestBody
                && olmDevice.hasInboundSessionKeys(request.requestBody!!.roomId!!, request.requestBody!!.senderKey!!, request.requestBody!!.sessionId!!))
    }

    override fun shareKeysWithDevice(request: IncomingRoomKeyRequest) {
        // sanity checks
        if (request.requestBody == null) {
            return
        }
        val userId = request.userId!!
        CoroutineScope(coroutineDispatchers.crypto).launch {
            deviceListManager
                    .downloadKeys(listOf(userId), false)
                    .flatMap {
                        val deviceId = request.deviceId
                        val deviceInfo = cryptoStore.getUserDevice(deviceId!!, userId)
                        if (deviceInfo == null) {
                            throw RuntimeException()
                        } else {
                            val devicesByUser = HashMap<String, List<MXDeviceInfo>>()
                            devicesByUser[userId] = ArrayList(Arrays.asList(deviceInfo))
                            ensureOlmSessionsForDevicesAction
                                    .handle(devicesByUser)
                                    .flatMap {
                                        val body = request.requestBody
                                        val olmSessionResult = it.getObject(deviceId, userId)
                                        if (olmSessionResult?.sessionId == null) {
                                            // no session with this device, probably because there
                                            // were no one-time keys.
                                            Try.just(Unit)
                                        }
                                        Timber.v("## shareKeysWithDevice() : sharing keys for session " + body!!.senderKey + "|" + body.sessionId
                                                + " with device " + userId + ":" + deviceId)
                                        val inboundGroupSession = olmDevice.getInboundGroupSession(body.sessionId, body.senderKey, body.roomId)

                                        val payloadJson = HashMap<String, Any>()
                                        payloadJson["type"] = EventType.FORWARDED_ROOM_KEY
                                        payloadJson["content"] = inboundGroupSession!!.exportKeys()!!

                                        val encodedPayload = messageEncrypter.encryptMessage(payloadJson, Arrays.asList(deviceInfo))
                                        val sendToDeviceMap = MXUsersDevicesMap<Any>()
                                        sendToDeviceMap.setObject(encodedPayload, userId, deviceId)
                                        Timber.v("## shareKeysWithDevice() : sending to $userId:$deviceId")
                                        val sendToDeviceParams = SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap)
                                        sendToDeviceTask.execute(sendToDeviceParams)
                                    }


                        }
                    }
        }
    }

}
