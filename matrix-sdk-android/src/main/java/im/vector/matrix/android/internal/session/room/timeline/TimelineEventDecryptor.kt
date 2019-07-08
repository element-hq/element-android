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
package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.internal.crypto.NewSessionListener
import im.vector.matrix.android.internal.crypto.model.event.EncryptedEventContent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import java.util.concurrent.Executors


internal class TimelineEventDecryptor(
        private val realmConfiguration: RealmConfiguration,
        private val timelineId: String,
        private val cryptoService: CryptoService
) {

    private val newSessionListener = object : NewSessionListener {
        override fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
            synchronized(unknownSessionsFailure) {
                unknownSessionsFailure[sessionId]?.let { eventIds ->
                    eventIds.forEach {
                        requestDecryption(it)
                    }
                }
                unknownSessionsFailure[sessionId]?.clear()
            }
        }

    }

    private val executor = Executors.newSingleThreadExecutor()

    private val existingRequests = HashSet<String>()
    private val unknownSessionsFailure = HashMap<String, MutableList<String>>()

    fun start() {
        cryptoService.addNewSessionListener(newSessionListener)
    }

    fun destroy() {
        cryptoService.removeSessionListener(newSessionListener)
        executor.shutdownNow()
        unknownSessionsFailure.clear()
        existingRequests.clear()
    }

    fun requestDecryption(eventId: String) {
        synchronized(existingRequests) {
            if (existingRequests.contains(eventId)) {
                return Unit.also {
                    Timber.d("Skip Decryption request for event ${eventId}, already requested")
                }
            }
            existingRequests.add(eventId)
        }
        synchronized(unknownSessionsFailure) {
            unknownSessionsFailure.values.forEach {
                if (it.contains(eventId)) return@synchronized Unit.also {
                    Timber.d("Skip Decryption request for event ${eventId}, unknown session")
                }
            }
        }
        executor.execute {
            Realm.getInstance(realmConfiguration).use { realm ->
                realm.executeTransaction {
                    processDecryptRequest(eventId, it)
                }
            }
        }
    }

    private fun processDecryptRequest(eventId: String, realm: Realm) {
        Timber.v("Decryption request for event ${eventId}")
        val eventEntity = EventEntity.where(realm, eventId = eventId).findFirst()
                ?: return Unit.also {
                    Timber.d("Decryption request for unknown message")
                }
        val event = eventEntity.asDomain()
        try {
            val result = cryptoService.decryptEvent(event, timelineId)
            Timber.v("Successfully decrypted event ${eventId}")
            eventEntity.setDecryptionResult(result)
        } catch (e: MXCryptoError) {
            Timber.v("Failed to decrypte event ${eventId} ${e}")
            if (e is MXCryptoError.Base && e.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                //Keep track of unknown sessions to automatically try to decrypt on new session
                eventEntity.decryptionErrorCode = e.errorType.name
                event.content?.toModel<EncryptedEventContent>()?.let { content ->
                    content.sessionId?.let { sessionId ->
                        synchronized(unknownSessionsFailure) {
                            val list = unknownSessionsFailure[sessionId]
                                    ?: ArrayList<String>().also {
                                        unknownSessionsFailure[sessionId] = it
                                    }
                            list.add(eventId)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to decrypt event $eventId")
        } finally {
            synchronized(existingRequests) {
                existingRequests.remove(eventId)
            }
        }
    }
}