/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.call

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.awaitTransaction
import io.realm.Realm
import timber.log.Timber
import javax.inject.Inject

internal interface CallEventsObserverTask : Task<CallEventsObserverTask.Params, Unit> {

    data class Params(
            val events: List<Event>,
            val userId: String
    )
}

internal class DefaultCallEventsObserverTask @Inject constructor(
        private val monarchy: Monarchy,
        private val cryptoService: CryptoService,
        private val callService: DefaultCallService) : CallEventsObserverTask {

    override suspend fun execute(params: CallEventsObserverTask.Params) {
        val events = params.events
        val userId = params.userId
        monarchy.awaitTransaction { realm ->
            Timber.v(">>> DefaultCallEventsObserverTask[${params.hashCode()}] called with ${events.size} events")
            update(realm, events, userId)
            Timber.v("<<< DefaultCallEventsObserverTask[${params.hashCode()}] finished")
        }
    }

    private fun update(realm: Realm, events: List<Event>, userId: String) {
        events.forEach { event ->
            event.roomId ?: return@forEach Unit.also {
                Timber.w("Event with no room id ${event.eventId}")
            }
            decryptIfNeeded(event)
            if (EventType.isCallEvent(event.getClearType())) {
                callService.onCallEvent(event)
            }
        }
        Timber.v("$realm : $userId")
    }

    private fun decryptIfNeeded(event: Event) {
        if (event.isEncrypted() && event.mxDecryptionResult == null) {
            try {
                val result = cryptoService.decryptEvent(event, event.roomId ?: "")
                event.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderCurve25519Key,
                        keysClaimed = result.claimedEd25519Key?.let { k -> mapOf("ed25519" to k) },
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                )
            } catch (e: MXCryptoError) {
                Timber.v("Call service: Failed to decrypt event")
                // TODO -> we should keep track of this and retry, or aggregation will be broken
            }
        }
    }
}
