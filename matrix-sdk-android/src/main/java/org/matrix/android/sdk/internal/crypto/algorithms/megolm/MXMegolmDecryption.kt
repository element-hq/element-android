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
import org.matrix.android.sdk.internal.crypto.algorithms.IMXDecrypting
import org.matrix.android.sdk.internal.crypto.keysbackup.DefaultKeysBackupService
import org.matrix.android.sdk.internal.crypto.OutgoingKeyRequestManager
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.session.StreamEventsManager
import timber.log.Timber

private val loggerTag = LoggerTag("MXMegolmDecryption", LoggerTag.CRYPTO)

internal class MXMegolmDecryption(
        private val olmDevice: MXOlmDevice,
        private val outgoingKeyRequestManager: OutgoingKeyRequestManager,
        private val cryptoStore: IMXCryptoStore,
        private val liveEventManager: Lazy<StreamEventsManager>
) : IMXDecrypting {

    var newSessionListener: NewSessionListener? = null

    /**
     * Events which we couldn't decrypt due to unknown sessions / indexes: map from
     * senderKey|sessionId to timelines to list of MatrixEvents.
     */
//    private var pendingEvents: MutableMap<String /* senderKey|sessionId */, MutableMap<String /* timelineId */, MutableList<Event>>> = HashMap()

    @Throws(MXCryptoError::class)
    override suspend fun decryptEvent(event: Event, timeline: String): MXEventDecryptionResult {
        // If cross signing is enabled, we don't send request until the keys are trusted
        // There could be a race effect here when xsigning is enabled, we should ensure that keys was downloaded once
        val requestOnFail = cryptoStore.getMyCrossSigningInfo()?.isTrusted() == true
        return decryptEvent(event, timeline, requestOnFail)
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
                                                .orEmpty()
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
                                    if (requestKeysOnFail) {
                                        requestKeysForEvent(event)
                                    }
                                    throw MXCryptoError.Base(
                                            MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX,
                                            "UNKNOWN_MESSAGE_INDEX",
                                            null)
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
     */
    override fun onRoomKeyEvent(event: Event, defaultKeysBackupService: DefaultKeysBackupService) {
        Timber.tag(loggerTag.value).v("onRoomKeyEvent()")
        var exportFormat = false
        val roomKeyContent = event.getClearContent().toModel<RoomKeyContent>() ?: return

        var senderKey: String? = event.getSenderKey()
        var keysClaimed: MutableMap<String, String> = HashMap()
        val forwardingCurve25519KeyChain: MutableList<String> = ArrayList()

        if (roomKeyContent.roomId.isNullOrEmpty() || roomKeyContent.sessionId.isNullOrEmpty() || roomKeyContent.sessionKey.isNullOrEmpty()) {
            Timber.tag(loggerTag.value).e("onRoomKeyEvent() :  Key event is missing fields")
            return
        }
        if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
            Timber.tag(loggerTag.value).i("onRoomKeyEvent(), forward adding key : ${roomKeyContent.roomId}|${roomKeyContent.sessionId}")
            val forwardedRoomKeyContent = event.getClearContent().toModel<ForwardedRoomKeyContent>()
                    ?: return

            forwardedRoomKeyContent.forwardingCurve25519KeyChain?.let {
                forwardingCurve25519KeyChain.addAll(it)
            }

            if (senderKey == null) {
                Timber.tag(loggerTag.value).e("onRoomKeyEvent() : event is missing sender_key field")
                return
            }

            forwardingCurve25519KeyChain.add(senderKey)

            exportFormat = true
            senderKey = forwardedRoomKeyContent.senderKey
            if (null == senderKey) {
                Timber.tag(loggerTag.value).e("onRoomKeyEvent() : forwarded_room_key event is missing sender_key field")
                return
            }

            if (null == forwardedRoomKeyContent.senderClaimedEd25519Key) {
                Timber.tag(loggerTag.value).e("forwarded_room_key_event is missing sender_claimed_ed25519_key field")
                return
            }

            keysClaimed["ed25519"] = forwardedRoomKeyContent.senderClaimedEd25519Key
        } else {
            Timber.tag(loggerTag.value).i("onRoomKeyEvent(), Adding key : ${roomKeyContent.roomId}|${roomKeyContent.sessionId}")
            if (null == senderKey) {
                Timber.tag(loggerTag.value).e("## onRoomKeyEvent() : key event has no sender key (not encrypted?)")
                return
            }

            // inherit the claimed ed25519 key from the setup message
            keysClaimed = event.getKeysClaimed().toMutableMap()
        }

        Timber.tag(loggerTag.value).i("onRoomKeyEvent addInboundGroupSession ${roomKeyContent.sessionId}")
        val added = olmDevice.addInboundGroupSession(roomKeyContent.sessionId,
                roomKeyContent.sessionKey,
                roomKeyContent.roomId,
                senderKey,
                forwardingCurve25519KeyChain,
                keysClaimed,
                exportFormat)

        when (added) {
            is MXOlmDevice.AddSessionResult.Imported               -> added.ratchetIndex
            is MXOlmDevice.AddSessionResult.NotImportedHigherIndex -> added.newIndex
            else                                                   -> null
        }?.let { index ->
            if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
                val fromDevice = (event.content?.get("sender_key") as? String)?.let { senderDeviceIdentityKey ->
                    cryptoStore.getUserDeviceList(event.senderId ?: "")
                            ?.firstOrNull {
                                it.identityKey() == senderDeviceIdentityKey
                            }
                }?.deviceId

                outgoingKeyRequestManager.onRoomKeyForwarded(
                        sessionId = roomKeyContent.sessionId,
                        algorithm = roomKeyContent.algorithm ?: "",
                        roomId = roomKeyContent.roomId,
                        senderKey = senderKey,
                        fromIndex = index,
                        fromDevice = fromDevice,
                        event = event)

                cryptoStore.saveIncomingForwardKeyAuditTrail(
                        roomId = roomKeyContent.roomId,
                        sessionId = roomKeyContent.sessionId,
                        senderKey = senderKey,
                        algorithm = roomKeyContent.algorithm ?: "",
                        userId = event.senderId ?: "",
                        deviceId = fromDevice ?: "",
                        chainIndex = index.toLong())

                // The index is used to decide if we cancel sent request or if we wait for a better key
                outgoingKeyRequestManager.postCancelRequestForSessionIfNeeded(roomKeyContent.sessionId, roomKeyContent.roomId, senderKey, index)
            }
        }

        if (added is MXOlmDevice.AddSessionResult.Imported) {
            Timber.tag(loggerTag.value)
                    .d("onRoomKeyEvent(${event.getClearType()}) : Added megolm session ${roomKeyContent.sessionId} in ${roomKeyContent.roomId}")
            defaultKeysBackupService.maybeBackupKeys()

            onNewSession(roomKeyContent.roomId, senderKey, roomKeyContent.sessionId)
        }
    }

    /**
     * Check if the some messages can be decrypted with a new session
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
