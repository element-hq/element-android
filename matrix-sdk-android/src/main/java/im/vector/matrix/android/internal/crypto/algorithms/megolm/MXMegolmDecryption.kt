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
import androidx.annotation.Keep
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.*
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.algorithms.MXDecryptionResult
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.crypto.model.MXDeviceInfo
import im.vector.matrix.android.internal.crypto.model.MXOlmSessionResult
import im.vector.matrix.android.internal.crypto.model.MXUsersDevicesMap
import im.vector.matrix.android.internal.crypto.CryptoManager
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.android.internal.crypto.model.rest.ForwardedRoomKeyContent
import im.vector.matrix.android.internal.crypto.model.event.RoomKeyContent
import im.vector.matrix.android.internal.crypto.model.rest.RoomKeyRequestBody
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.util.*

@Keep
internal class MXMegolmDecryption : IMXDecrypting {
    /**
     * The olm device interface
     */

    // the matrix credentials
    private lateinit var mCredentials: Credentials

    private lateinit var mCrypto: CryptoManager
    private lateinit var mOlmDevice: MXOlmDevice
    private lateinit var mDeviceListManager: DeviceListManager
    private lateinit var mCryptoStore: IMXCryptoStore
    private lateinit var mSendToDeviceTask: SendToDeviceTask
    private lateinit var mTaskExecutor: TaskExecutor

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
    private var mPendingEvents: MutableMap<String, MutableMap<String /* timelineId */, MutableList<Event>>>? = null/* senderKey|sessionId */

    /**
     * Init the object fields
     */
    override fun initWithMatrixSession(credentials: Credentials,
                                       crypto: CryptoManager,
                                       olmDevice: MXOlmDevice,
                                       deviceListManager: DeviceListManager,
                                       sendToDeviceTask: SendToDeviceTask,
                                       taskExecutor: TaskExecutor) {
        mCredentials = credentials
        mDeviceListManager = deviceListManager
        mSendToDeviceTask = sendToDeviceTask
        mTaskExecutor = taskExecutor
        mOlmDevice = olmDevice
        mPendingEvents = HashMap()
    }

    @Throws(MXDecryptionException::class)
    override fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult? {
        return decryptEvent(event, timeline, true)
    }

    @Throws(MXDecryptionException::class)
    private fun decryptEvent(event: Event?, timeline: String, requestKeysOnFail: Boolean): MXEventDecryptionResult? {
        // sanity check
        if (null == event) {
            Timber.e("## decryptEvent() : null event")
            return null
        }

        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()!!

        if (TextUtils.isEmpty(encryptedEventContent.senderKey) || TextUtils.isEmpty(encryptedEventContent.sessionId) || TextUtils.isEmpty(encryptedEventContent.ciphertext)) {
            throw MXDecryptionException(MXCryptoError(MXCryptoError.MISSING_FIELDS_ERROR_CODE,
                    MXCryptoError.UNABLE_TO_DECRYPT, MXCryptoError.MISSING_FIELDS_REASON))
        }

        var eventDecryptionResult: MXEventDecryptionResult? = null
        var cryptoError: MXCryptoError? = null
        var decryptGroupMessageResult: MXDecryptionResult? = null

        try {
            decryptGroupMessageResult = mOlmDevice.decryptGroupMessage(encryptedEventContent.ciphertext!!, event.roomId!!, timeline, encryptedEventContent.sessionId!!, encryptedEventContent.senderKey!!)
        } catch (e: MXDecryptionException) {
            cryptoError = e.cryptoError
        }

        // the decryption succeeds
        if (null != decryptGroupMessageResult && null != decryptGroupMessageResult.mPayload && null == cryptoError) {
            eventDecryptionResult = MXEventDecryptionResult()

            eventDecryptionResult.mClearEvent = decryptGroupMessageResult.mPayload
            eventDecryptionResult.mSenderCurve25519Key = decryptGroupMessageResult.mSenderKey

            if (null != decryptGroupMessageResult.mKeysClaimed) {
                eventDecryptionResult.mClaimedEd25519Key = decryptGroupMessageResult.mKeysClaimed!!["ed25519"]
            }

            eventDecryptionResult.mForwardingCurve25519KeyChain = decryptGroupMessageResult.mForwardingCurve25519KeyChain!!
        } else if (null != cryptoError) {
            if (cryptoError.isOlmError) {
                if (TextUtils.equals("UNKNOWN_MESSAGE_INDEX", cryptoError.message)) {
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
        val sender = event.sender!!
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()!!

        val recipients = ArrayList<Map<String, String>>()

        val selfMap = HashMap<String, String>()
        selfMap["userId"] = mCredentials.userId
        selfMap["deviceId"] = "*"
        recipients.add(selfMap)

        if (!TextUtils.equals(sender, mCredentials.userId)) {
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

        mCrypto.requestRoomKey(requestBody, recipients)
    }

    /**
     * Add an event to the list of those we couldn't decrypt the first time we
     * saw them.
     *
     * @param event      the event to try to decrypt later
     * @param timelineId the timeline identifier
     */
    private fun addEventToPendingList(event: Event, timelineId: String) {
        var timelineId = timelineId
        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()!!

        val k = "${encryptedEventContent.senderKey}|${encryptedEventContent.sessionId}"

        // avoid undefined timelineId
        if (TextUtils.isEmpty(timelineId)) {
            timelineId = ""
        }

        if (!mPendingEvents!!.containsKey(k)) {
            mPendingEvents!![k] = HashMap()
        }

        if (!mPendingEvents!![k]!!.containsKey(timelineId)) {
            mPendingEvents!![k]!!.put(timelineId, ArrayList<Event>())
        }

        if (mPendingEvents!![k]!![timelineId]!!.indexOf(event) < 0) {
            Timber.d("## addEventToPendingList() : add Event " + event.eventId + " in room id " + event.roomId)
            mPendingEvents!![k]!![timelineId]!!.add(event)
        }
    }

    /**
     * Handle a key event.
     *
     * @param roomKeyEvent the key event.
     */
    override fun onRoomKeyEvent(event: Event) {
        var exportFormat = false
        val roomKeyContent = event.content.toModel<RoomKeyContent>()!!

        var senderKey: String? = event.getSenderKey()
        var keysClaimed: MutableMap<String, String> = HashMap()
        var forwarding_curve25519_key_chain: MutableList<String>? = null

        if (TextUtils.isEmpty(roomKeyContent.roomId) || TextUtils.isEmpty(roomKeyContent.sessionId) || TextUtils.isEmpty(roomKeyContent.sessionKey)) {
            Timber.e("## onRoomKeyEvent() :  Key event is missing fields")
            return
        }

        if (TextUtils.equals(event.type, EventType.FORWARDED_ROOM_KEY)) {
            Timber.d("## onRoomKeyEvent(), forward adding key : roomId " + roomKeyContent.roomId + " sessionId " + roomKeyContent.sessionId
                    + " sessionKey " + roomKeyContent.sessionKey) // from " + event);
            val forwardedRoomKeyContent = event.content.toModel<ForwardedRoomKeyContent>()!!

            if (null == forwardedRoomKeyContent.forwardingCurve25519KeyChain) {
                forwarding_curve25519_key_chain = ArrayList()
            } else {
                forwarding_curve25519_key_chain = ArrayList(forwardedRoomKeyContent.forwardingCurve25519KeyChain!!)
            }

            forwarding_curve25519_key_chain.add(senderKey!!)

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

            keysClaimed["ed25519"] = forwardedRoomKeyContent.senderClaimedEd25519Key!!
        } else {
            Timber.d("## onRoomKeyEvent(), Adding key : roomId " + roomKeyContent.roomId + " sessionId " + roomKeyContent.sessionId
                    + " sessionKey " + roomKeyContent.sessionKey) // from " + event);

            if (null == senderKey) {
                Timber.e("## onRoomKeyEvent() : key event has no sender key (not encrypted?)")
                return
            }

            // inherit the claimed ed25519 key from the setup message
            keysClaimed = event.getKeysClaimed().toMutableMap()
        }

        val added = mOlmDevice.addInboundGroupSession(roomKeyContent.sessionId!!, roomKeyContent.sessionKey!!, roomKeyContent.roomId!!, senderKey, forwarding_curve25519_key_chain!!, keysClaimed, exportFormat)

        if (added) {
            mCrypto.getKeysBackupService().maybeBackupKeys()

            val content = RoomKeyRequestBody()

            content.algorithm = roomKeyContent.algorithm
            content.roomId = roomKeyContent.roomId
            content.sessionId = roomKeyContent.sessionId
            content.senderKey = senderKey

            mCrypto.cancelRoomKeyRequest(content)

            onNewSession(senderKey, roomKeyContent.sessionId!!)
        }
    }

    /**
     * Check if the some messages can be decrypted with a new session
     *
     * @param senderKey the session sender key
     * @param sessionId the session id
     */
    override fun onNewSession(senderKey: String, sessionId: String) {
        val k = "$senderKey|$sessionId"

        val pending = mPendingEvents!![k]

        if (null != pending) {
            // Have another go at decrypting events sent with this session.
            mPendingEvents!!.remove(k)

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
                            TODO()
                            //mSession!!.onEventDecrypted(event)
                        }
                        Timber.d("## onNewSession() : successful re-decryption of " + event.eventId)
                    }
                }
            }
        }
    }

    override fun hasKeysForKeyRequest(request: IncomingRoomKeyRequest): Boolean {
        return (null != request
                && null != request.mRequestBody
                && mOlmDevice.hasInboundSessionKeys(request.mRequestBody!!.roomId!!, request.mRequestBody!!.senderKey!!, request.mRequestBody!!.sessionId!!))
    }

    override fun shareKeysWithDevice(request: IncomingRoomKeyRequest) {
        // sanity checks
        if (null == request || null == request.mRequestBody) {
            return
        }

        val userId = request.mUserId!!

        mDeviceListManager.downloadKeys(listOf(userId), false, object : MatrixCallback<MXUsersDevicesMap<MXDeviceInfo>> {
            override fun onSuccess(data: MXUsersDevicesMap<MXDeviceInfo>) {
                val deviceId = request.mDeviceId
                val deviceInfo = mCryptoStore.getUserDevice(deviceId!!, userId)

                if (null != deviceInfo) {
                    val body = request.mRequestBody

                    val devicesByUser = HashMap<String, List<MXDeviceInfo>>()
                    devicesByUser[userId] = ArrayList(Arrays.asList(deviceInfo))

                    mCrypto.ensureOlmSessionsForDevices(devicesByUser, object : MatrixCallback<MXUsersDevicesMap<MXOlmSessionResult>> {
                        override fun onSuccess(map: MXUsersDevicesMap<MXOlmSessionResult>) {
                            val olmSessionResult = map.getObject(deviceId, userId)

                            if (null == olmSessionResult || null == olmSessionResult.mSessionId) {
                                // no session with this device, probably because there
                                // were no one-time keys.
                                //
                                // ensureOlmSessionsForUsers has already done the logging,
                                // so just skip it.
                                return
                            }

                            Timber.d("## shareKeysWithDevice() : sharing keys for session " + body!!.senderKey + "|" + body.sessionId
                                    + " with device " + userId + ":" + deviceId)

                            val inboundGroupSession = mOlmDevice.getInboundGroupSession(body.sessionId, body.senderKey, body.roomId)

                            val payloadJson = HashMap<String, Any>()
                            payloadJson["type"] = EventType.FORWARDED_ROOM_KEY
                            payloadJson["content"] = inboundGroupSession!!.exportKeys()!!

                            val encodedPayload = mCrypto.encryptMessage(payloadJson, Arrays.asList(deviceInfo))
                            val sendToDeviceMap = MXUsersDevicesMap<Any>()
                            sendToDeviceMap.setObject(encodedPayload, userId, deviceId)

                            Timber.d("## shareKeysWithDevice() : sending to $userId:$deviceId")
                            mSendToDeviceTask.configureWith(SendToDeviceTask.Params(EventType.ENCRYPTED, sendToDeviceMap))
                                    .dispatchTo(object : MatrixCallback<Unit> {
                                        override fun onSuccess(data: Unit) {
                                            Timber.d("## shareKeysWithDevice() : sent to $userId:$deviceId")
                                        }

                                        override fun onFailure(failure: Throwable) {
                                            Timber.e(failure, "## shareKeysWithDevice() : sendToDevice $userId:$deviceId failed")
                                        }
                                    })
                                    .executeBy(mTaskExecutor)
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "## shareKeysWithDevice() : ensureOlmSessionsForDevices $userId:$deviceId failed")
                        }
                    })
                } else {
                    Timber.e("## shareKeysWithDevice() : ensureOlmSessionsForDevices $userId:$deviceId not found")
                }
            }

            override fun onFailure(failure: Throwable) {
                Timber.e(failure, "## shareKeysWithDevice() : downloadKeys $userId failed")
            }
        })
    }
}
