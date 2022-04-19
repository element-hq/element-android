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
package org.matrix.android.sdk.internal.session.room.timeline

import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.sync.handler.room.ThreadsAwarenessHandler
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

internal class TimelineEventDecryptor @Inject constructor(
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration,
        private val cryptoService: CryptoService,
        private val threadsAwarenessHandler: ThreadsAwarenessHandler,
) {

    private val newSessionListener = object : NewSessionListener {
        override fun onNewSession(roomId: String?, senderKey: String, sessionId: String) {
            synchronized(unknownSessionsFailure) {
                unknownSessionsFailure[sessionId]
                        ?.toList()
                        .orEmpty()
                        .also {
                            unknownSessionsFailure[sessionId]?.clear()
                        }
            }.forEach {
                requestDecryption(it)
            }
        }
    }

    private var executor: ExecutorService? = null

    // Set of eventIds which are currently decrypting
    private val existingRequests = mutableSetOf<DecryptionRequest>()

    // sessionId -> list of eventIds
    private val unknownSessionsFailure = mutableMapOf<String, MutableSet<DecryptionRequest>>()

    fun start() {
        executor = Executors.newSingleThreadExecutor()
        cryptoService.addNewSessionListener(newSessionListener)
    }

    fun destroy() {
        cryptoService.removeSessionListener(newSessionListener)
        executor?.shutdownNow()
        executor = null
        synchronized(unknownSessionsFailure) {
            unknownSessionsFailure.clear()
        }
        synchronized(existingRequests) {
            existingRequests.clear()
        }
    }

    fun requestDecryption(request: DecryptionRequest) {
        synchronized(unknownSessionsFailure) {
            for (requests in unknownSessionsFailure.values) {
                if (request in requests) {
                    Timber.d("Skip Decryption request for event ${request.event.eventId}, unknown session")
                    return
                }
            }
        }
        synchronized(existingRequests) {
            if (!existingRequests.add(request)) {
                Timber.d("Skip Decryption request for event ${request.event.eventId}, already requested")
                return
            }
        }
        executor?.execute {
            Realm.getInstance(realmConfiguration).use { realm ->
                try {
                    runBlocking {
                        processDecryptRequest(request, realm)
                    }
                } catch (e: InterruptedException) {
                    Timber.i("Decryption got interrupted")
                }
            }
        }
    }

    private fun threadAwareNonEncryptedEvents(request: DecryptionRequest, realm: Realm) {
        val event = request.event
        realm.executeTransaction {
            val eventId = event.eventId ?: return@executeTransaction
            val eventEntity = EventEntity
                    .where(it, eventId = eventId)
                    .findFirst()
            val decryptedEvent = eventEntity?.asDomain()
            threadsAwarenessHandler.makeEventThreadAware(realm, event.roomId, decryptedEvent, eventEntity)
        }
    }

    private suspend fun processDecryptRequest(request: DecryptionRequest, realm: Realm) {
        val event = request.event
        val timelineId = request.timelineId

        if (!request.event.isEncrypted()) {
            // Here we have requested a decryption to an event that is not encrypted
            // We will simply make this event thread aware
            threadAwareNonEncryptedEvents(request, realm)
            return
        }
        try {
            val result = cryptoService.decryptEvent(request.event, timelineId)
            Timber.v("Successfully decrypted event ${event.eventId}")
            realm.executeTransaction {
                val eventId = event.eventId ?: return@executeTransaction
                val eventEntity = EventEntity
                        .where(it, eventId = eventId)
                        .findFirst()
                eventEntity?.setDecryptionResult(result)
                val decryptedEvent = eventEntity?.asDomain()
                threadsAwarenessHandler.makeEventThreadAware(realm, event.roomId, decryptedEvent, eventEntity)
            }
        } catch (e: MXCryptoError) {
            Timber.v("Failed to decrypt event ${event.eventId} : ${e.localizedMessage}")
            if (e is MXCryptoError.Base /*&& e.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID*/) {
                // Keep track of unknown sessions to automatically try to decrypt on new session
                realm.executeTransaction {
                    EventEntity.where(it, eventId = event.eventId ?: "")
                            .findFirst()
                            ?.let {
                                it.decryptionErrorCode = e.errorType.name
                                it.decryptionErrorReason = e.technicalMessage.takeIf { it.isNotEmpty() } ?: e.detailedErrorDescription
                            }
                }
                event.content?.toModel<EncryptedEventContent>()?.let { content ->
                    content.sessionId?.let { sessionId ->
                        synchronized(unknownSessionsFailure) {
                            val list = unknownSessionsFailure.getOrPut(sessionId) { mutableSetOf() }
                            list.add(request)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e("Failed to decrypt event ${event.eventId}, ${t.localizedMessage}")
        } finally {
            synchronized(existingRequests) {
                existingRequests.remove(request)
            }
        }
    }

    data class DecryptionRequest(
            val event: Event,
            val timelineId: String
    )
}
