/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import com.zhuinden.monarchy.Monarchy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.NewSessionListener
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class RoomSummaryEventDecryptor @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        cryptoCoroutineScope: CoroutineScope,
        private val cryptoService: dagger.Lazy<CryptoService>
) {

    internal sealed class Message {
        data class DecryptEvent(val event: Event) : Message()
        data class NewSessionImported(val sessionId: String) : Message()
    }

    private val scope: CoroutineScope = CoroutineScope(
            cryptoCoroutineScope.coroutineContext +
                    SupervisorJob() +
                    CoroutineName("RoomSummaryDecryptor")
    )

    private val channel = Channel<Message>(capacity = 300)

    private val newSessionListener = object : NewSessionListener {
        override fun onNewSession(roomId: String?, sessionId: String) {
            scope.launch(coroutineDispatchers.computation) {
                channel.send(Message.NewSessionImported(sessionId))
            }
        }
    }

    private val unknownSessionsFailure = mutableMapOf<String, MutableSet<Event>>()

    init {
        scope.launch {
            cryptoService.get().addNewSessionListener(newSessionListener)
            for (request in channel) {
                when (request) {
                    is Message.DecryptEvent -> handleDecryptEvent(request.event)
                    is Message.NewSessionImported -> handleNewSessionImported(request.sessionId)
                }
            }
        }
    }

    private fun handleNewSessionImported(sessionId: String) {
        unknownSessionsFailure[sessionId]
                ?.toList()
                .orEmpty()
                .also {
                    unknownSessionsFailure[sessionId]?.clear()
                }.forEach {
                    // post a retry!
                    requestDecryption(it)
                }
    }

    private suspend fun handleDecryptEvent(event: Event) {
        if (event.getClearType() != EventType.ENCRYPTED) return
        val algorithm = event.content?.get("algorithm") as? String
        if (algorithm != MXCRYPTO_ALGORITHM_MEGOLM) return

        try {
            val result = cryptoService.get().decryptEvent(event, "")
            // now let's persist the result in database
            monarchy.writeAsync { realm ->
                val eventEntity = EventEntity.where(realm, event.eventId.orEmpty()).findFirst()
                eventEntity?.setDecryptionResult(result)
            }
        } catch (failure: Throwable) {
            Timber.v(failure, "Failed to decrypt event ${event.eventId}")
            // We don't need to get more details, just mark this session in failures
            if (failure is MXCryptoError.Base) {
                monarchy.writeAsync { realm ->
                    EventEntity.where(realm, eventId = event.eventId.orEmpty())
                            .findFirst()
                            ?.let {
                                it.decryptionErrorCode = failure.errorType.name
                                it.decryptionErrorReason = failure.technicalMessage.takeIf { it.isNotEmpty() } ?: failure.detailedErrorDescription
                            }
                }

                if (failure.errorType == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID ||
                        failure.errorType == MXCryptoError.ErrorType.UNKNOWN_MESSAGE_INDEX) {
                    (event.content["session_id"] as? String)?.let { sessionId ->
                        unknownSessionsFailure.getOrPut(sessionId) { mutableSetOf() }
                                .add(event)
                    }
                }
            }
        }
    }

    fun requestDecryption(event: Event) {
        channel.trySend(Message.DecryptEvent(event))
    }
}
