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

package org.matrix.android.sdk.internal.crypto.algorithms.megolm

import dagger.Lazy
import org.matrix.android.sdk.api.crypto.MXCryptoConfig
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.crypto.model.ForwardedRoomKeyContent
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.RoomKeyContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.MXOlmDevice
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.matrix.android.sdk.internal.crypto.algorithms.IMXDecrypting
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.StreamEventsManager
import org.matrix.android.sdk.internal.util.time.Clock
import timber.log.Timber

private val loggerTag = LoggerTag("MXMegolmDecryption", LoggerTag.CRYPTO)

internal class MXMegolmDecryption(
        private val olmDevice: MXOlmDevice,
        private val myUserId: String,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        private val cryptoStore: IMXCryptoStore,
        private val liveEventManager: Lazy<StreamEventsManager>,
        private val unrequestedForwardManager: UnRequestedForwardManager,
        private val cryptoConfig: MXCryptoConfig,
        private val clock: Clock,
) : IMXDecrypting {

    var newSessionListener: NewSessionListener? = null

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
//    private var pendingEvents: MutableMap<String /* senderKey|sessionId */, MutableMap<String /* timelineId */, MutableList<Event>>> = HashMap()

    @Throws(MXCryptoError::class)
    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        return decryptEvent(event, timeline, true)
    }

    @Throws(MXCryptoError::class)
    private suspend fun decryptEvent(event: Event, timeline: String, requestKeysOnFail: Boolean): MXEventDecryptionResult {
        Timber.tag(loggerTag.value).v("decryptEvent ${event.eventId}, requestKeysOnFail:$requestKeysOnFail")
        if (event.roomId.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        val encryptedEventContent = event.content.toModel<EncryptedEventContent>()
                ?: throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)

        if (encryptedEventContent.senderKey.isNullOrBlank() ||
                encryptedEventContent.sessionId.isNullOrBlank() ||
                encryptedEventContent.ciphertext.isNullOrBlank()) {
            throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
        }

        return runCatching {
            olmDevice.decryptGroupMessage(
                    encryptedEventContent.ciphertext,
                    event.roomId,
                    timeline,
                    eventId = event.eventId.orEmpty(),
                    encryptedEventContent.sessionId,
                    encryptedEventContent.senderKey
            )
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
                                                .orEmpty(),
                                        isSafe = olmDecryptionResult.isSafe.orFalse()
                                ).also {
                                    liveEventManager.get().dispatchLiveEventDecrypted(event, it)
                                }
                            } else {
                                throw MXCryptoError.Base(MXCryptoError.ErrorType.MISSING_FIELDS, MXCryptoError.MISSING_FIELDS_REASON)
                            }
                        },
                        { throwable ->
                            liveEventManager.get().dispatchLiveEventDecryptionFailed(event, throwable)
                            if (throwable is MXCryptoError.OlmError) {
                                // TODO Check the value of .message
                                if (throwable.olmException.message == "UNKNOWN_MESSAGE_INDEX") {
                                    // So we know that session, but it's ratcheted and we can't decrypt at that index

                                    if (requestKeysOnFail) {
                                        requestKeysForEvent(event)
                                    }
                                    // Check if partially withheld
                                    val withHeldInfo = cryptoStore.getWithHeldMegolmSession(event.roomId, encryptedEventContent.sessionId)
                                    if (withHeldInfo != null) {
                                        // Encapsulate as withHeld exception
                                        throw MXCryptoError.Base(
                                                MXCryptoError.ErrorType.KEYS_WITHHELD,
                                                withHeldInfo.code?.value ?: "",
                                                withHeldInfo.reason
                                        )
                                    }

                                    throw MXCryptoError.Base(
                                            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX,
                                            "UNKNOWN_MESSAGE_INDEX",
                                            null
                                    )
                                }

                                val reason = String.format(MXCryptoError.OLM_REASON, throwable.olmException.message)
                                val detailedReason = String.format(MXCryptoError.DETAILED_OLM_REASON, encryptedEventContent.ciphertext, reason)

                                throw MXCryptoError.Base(
                                        MXCryptoError.ErrorType.OLM,
                                        reason,
                                        detailedReason
                                )
                            }
                            if (throwable is MXCryptoError.Base) {
                                if (throwable.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                                    // Check if it was withheld by sender to enrich error code
                                    val withHeldInfo = cryptoStore.getWithHeldMegolmSession(event.roomId, encryptedEventContent.sessionId)
                                    if (withHeldInfo != null) {
                                        if (requestKeysOnFail) {
                                            requestKeysForEvent(event)
                                        }
                                        // Encapsulate as withHeld exception
                                        throw MXCryptoError.Base(
                                                MXCryptoError.ErrorType.KEYS_WITHHELD,
                                                withHeldInfo.code?.value ?: "",
                                                withHeldInfo.reason
                                        )
                                    }

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
        outgoingKeyRequestManager.requestKeyForEvent(event, false)
    }

    /**
     * Handle a key event.
     *
     * @param event the key event.
     * @param defaultKeysBackupService the keys backup service
     * @param forceAccept if true will force to accept the forwarded key
     */
    override fun onRoomKeyEvent(event: Event, defaultKeysBackupService: DefaultKeysBackupService, forceAccept: Boolean) {
        Timber.tag(loggerTag.value).v("onRoomKeyEvent(${event.getSenderKey()})")
        var exportFormat = false
        val roomKeyContent = event.getDecryptedContent()?.toModel<RoomKeyContent>() ?: return

        val eventSenderKey: String = event.getSenderKey() ?: return Unit.also {
            Timber.tag(loggerTag.value).e("onRoom Key/Forward Event() : event is missing sender_key field")
        }

        // this device might not been downloaded now?
        val fromDevice = cryptoStore.deviceWithIdentityKey(eventSenderKey)

        lateinit var sessionInitiatorSenderKey: String
        val trusted: Boolean

        var keysClaimed: MutableMap<String, String> = HashMap()
        val forwardingCurve25519KeyChain: MutableList<String> = ArrayList()

        if (roomKeyContent.roomId.isNullOrEmpty() || roomKeyContent.sessionId.isNullOrEmpty() || roomKeyContent.sessionKey.isNullOrEmpty()) {
            Timber.tag(loggerTag.value).e("onRoomKeyEvent() :  Key event is missing fields")
            return
        }
        if (event.getDecryptedType() == EventType.FORWARDED_ROOM_KEY) {
            if (!cryptoStore.isKeyGossipingEnabled()) {
                Timber.tag(loggerTag.value)
                        .i("onRoomKeyEvent(), ignore forward adding as per crypto config : ${roomKeyContent.roomId}|${roomKeyContent.sessionId}")
                return
            }
            Timber.tag(loggerTag.value).i("onRoomKeyEvent(), forward adding key : ${roomKeyContent.roomId}|${roomKeyContent.sessionId}")
            val forwardedRoomKeyContent = event.getDecryptedContent()?.toModel<ForwardedRoomKeyContent>()
                    ?: return

            forwardedRoomKeyContent.forwardingCurve25519KeyChain?.let {
                forwardingCurve25519KeyChain.addAll(it)
            }

            forwardingCurve25519KeyChain.add(eventSenderKey)

            exportFormat = true
            sessionInitiatorSenderKey = forwardedRoomKeyContent.senderKey ?: return Unit.also {
                Timber.tag(loggerTag.value).e("onRoomKeyEvent() : forwarded_room_key event is missing sender_key field")
            }

            if (null == forwardedRoomKeyContent.senderClaimedEd25519Key) {
                Timber.tag(loggerTag.value).e("forwarded_room_key_event is missing sender_claimed_ed25519_key field")
                return
            }

            keysClaimed["ed25519"] = forwardedRoomKeyContent.senderClaimedEd25519Key

            // checking if was requested once.
            // should we check if the request is sort of active?
            val wasNotRequested = cryptoStore.getOutgoingRoomKeyRequest(
                    roomId = forwardedRoomKeyContent.roomId.orEmpty(),
                    sessionId = forwardedRoomKeyContent.sessionId.orEmpty(),
                    algorithm = forwardedRoomKeyContent.algorithm.orEmpty(),
                    senderKey = forwardedRoomKeyContent.senderKey.orEmpty(),
            ).isEmpty()

            trusted = false

            if (!forceAccept && wasNotRequested) {
//                val senderId = cryptoStore.deviceWithIdentityKey(event.getSenderKey().orEmpty())?.userId.orEmpty()
                unrequestedForwardManager.onUnRequestedKeyForward(roomKeyContent.roomId, event, clock.epochMillis())
                // Ignore unsolicited
                Timber.tag(loggerTag.value).w("Ignoring forwarded_room_key_event for ${roomKeyContent.sessionId} that was not requested")
                return
            }

            // Check who sent the request, as we requested we have the device keys (no need to download)
            val sessionThatIsSharing = cryptoStore.deviceWithIdentityKey(eventSenderKey)
            if (sessionThatIsSharing == null) {
                Timber.tag(loggerTag.value).w("Ignoring forwarded_room_key from unknown device with identity $eventSenderKey")
                return
            }
            val isOwnDevice = myUserId == sessionThatIsSharing.userId
            val isDeviceVerified = sessionThatIsSharing.isVerified
            val isFromSessionInitiator = sessionThatIsSharing.identityKey() == sessionInitiatorSenderKey

            val isLegitForward = (isOwnDevice && isDeviceVerified) ||
                    (!cryptoConfig.limitRoomKeyRequestsToMyDevices && isFromSessionInitiator)

            val shouldAcceptForward = forceAccept || isLegitForward

            if (!shouldAcceptForward) {
                Timber.tag(loggerTag.value)
                        .w("Ignoring forwarded_room_key device:$eventSenderKey, ownVerified:{$isOwnDevice&&$isDeviceVerified}," +
                                " fromInitiator:$isFromSessionInitiator")
                return
            }
        } else {
            // It's a m.room_key so safe
            trusted = true
            sessionInitiatorSenderKey = eventSenderKey
            Timber.tag(loggerTag.value).i("onRoomKeyEvent(), Adding key : ${roomKeyContent.roomId}|${roomKeyContent.sessionId}")
            // inherit the claimed ed25519 key from the setup message
            keysClaimed = event.getKeysClaimed().toMutableMap()
        }

        Timber.tag(loggerTag.value).i("onRoomKeyEvent addInboundGroupSession ${roomKeyContent.sessionId}")
        val addSessionResult = olmDevice.addInboundGroupSession(
                sessionId = roomKeyContent.sessionId,
                sessionKey = roomKeyContent.sessionKey,
                roomId = roomKeyContent.roomId,
                senderKey = sessionInitiatorSenderKey,
                forwardingCurve25519KeyChain = forwardingCurve25519KeyChain,
                keysClaimed = keysClaimed,
                exportFormat = exportFormat,
                sharedHistory = roomKeyContent.getSharedKey(),
                trusted = trusted
        ).also {
            Timber.tag(loggerTag.value).v(".. onRoomKeyEvent addInboundGroupSession ${roomKeyContent.sessionId} result: $it")
        }

        when (addSessionResult) {
            is MXOlmDevice.AddSessionResult.Imported -> addSessionResult.ratchetIndex
            is MXOlmDevice.AddSessionResult.NotImportedHigherIndex -> addSessionResult.newIndex
            else -> null
        }?.let { index ->
            if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
                outgoingKeyRequestManager.onRoomKeyForwarded(
                        sessionId = roomKeyContent.sessionId,
                        algorithm = roomKeyContent.algorithm ?: "",
                        roomId = roomKeyContent.roomId,
                        senderKey = sessionInitiatorSenderKey,
                        fromIndex = index,
                        fromDevice = fromDevice?.deviceId,
                        event = event
                )

                cryptoStore.saveIncomingForwardKeyAuditTrail(
                        roomId = roomKeyContent.roomId,
                        sessionId = roomKeyContent.sessionId,
                        senderKey = sessionInitiatorSenderKey,
                        algorithm = roomKeyContent.algorithm ?: "",
                        userId = event.senderId.orEmpty(),
                        deviceId = fromDevice?.deviceId.orEmpty(),
                        chainIndex = index.toLong()
                )

                // The index is used to decide if we cancel sent request or if we wait for a better key
                outgoingKeyRequestManager.postCancelRequestForSessionIfNeeded(roomKeyContent.sessionId, roomKeyContent.roomId, sessionInitiatorSenderKey, index)
            }
        }

        if (addSessionResult is MXOlmDevice.AddSessionResult.Imported) {
            Timber.tag(loggerTag.value)
                    .d("onRoomKeyEvent(${event.getClearType()}) : Added megolm session ${roomKeyContent.sessionId} in ${roomKeyContent.roomId}")
            defaultKeysBackupService.maybeBackupKeys()

            onNewSession(roomKeyContent.roomId, sessionInitiatorSenderKey, roomKeyContent.sessionId)
        }
    }

    /**
     * Returns boolean shared key flag, if enabled with respect to matrix configuration.
     */
    private fun RoomKeyContent.getSharedKey(): Boolean {
        if (!cryptoStore.isShareKeysOnInviteEnabled()) return false
        return sharedHistory ?: false
    }

    /**
     * Check if the some messages can be decrypted with a new session.
     *
     * @param roomId the room id where the new Megolm session has been created for, may be null when importing from external sessions
     * @param senderKey the session sender key
     * @param sessionId the session id
     */
    fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
        Timber.tag(loggerTag.value).v("ON NEW SESSION $sessionId - $senderKey")
        newSessionListener?.onNewSession(roomId, senderKey, sessionId)
    }
}
