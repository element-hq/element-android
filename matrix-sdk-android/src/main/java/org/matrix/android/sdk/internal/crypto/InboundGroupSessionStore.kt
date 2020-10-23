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

package org.matrix.android.sdk.internal.crypto

import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

/**
 * Allows to cache and batch store operations on inbound group session store.
 * Because it is used in the decrypt flow, that can be called quite rapidly
 */
internal class InboundGroupSessionStore @Inject constructor(
        private val store: IMXCryptoStore,
        private val cryptoCoroutineScope: CoroutineScope,
        private val coroutineDispatchers: MatrixCoroutineDispatchers) {

    private data class CacheKey(
            val sessionId: String,
            val senderKey: String
    )

    private val sessionCache = object : LruCache<CacheKey, OlmInboundGroupSessionWrapper2>(30) {
        override fun entryRemoved(evicted: Boolean, key: CacheKey?, oldValue: OlmInboundGroupSessionWrapper2?, newValue: OlmInboundGroupSessionWrapper2?) {
            if (evicted && oldValue != null) {
                cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                    Timber.v("## Inbound: entryRemoved ${oldValue.roomId}-${oldValue.senderKey}")
                    store.storeInboundGroupSessions(listOf(oldValue))
                }
            }
        }
    }

    private val timer = Timer()
    private var timerTask: TimerTask? = null

    private val dirtySession = mutableListOf<OlmInboundGroupSessionWrapper2>()

    @Synchronized
    fun getInboundGroupSession(sessionId: String, senderKey: String): OlmInboundGroupSessionWrapper2? {
        synchronized(sessionCache) {
            val known = sessionCache[CacheKey(sessionId, senderKey)]
            Timber.v("## Inbound: getInboundGroupSession in cache ${known != null}")
            return known ?: store.getInboundGroupSession(sessionId, senderKey)?.also {
                Timber.v("## Inbound: getInboundGroupSession cache populate ${it.roomId}")
                sessionCache.put(CacheKey(sessionId, senderKey), it)
            }
        }
    }

    @Synchronized
    fun storeInBoundGroupSession(wrapper: OlmInboundGroupSessionWrapper2) {
        Timber.v("## Inbound: getInboundGroupSession mark as dirty ${wrapper.roomId}-${wrapper.senderKey}")
        // We want to batch this a bit for performances
        dirtySession.add(wrapper)

        timerTask?.cancel()
        timerTask = object : TimerTask() {
            override fun run() {
                batchSave()
            }
        }
        timer.schedule(timerTask!!, 2_000)
    }

    @Synchronized
    private fun batchSave() {
        val toSave = mutableListOf<OlmInboundGroupSessionWrapper2>().apply { addAll(dirtySession) }
        dirtySession.clear()
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.v("## Inbound: getInboundGroupSession batching save of ${dirtySession.size}")
            tryOrNull {
                store.storeInboundGroupSessions(toSave)
            }
        }
    }
}
