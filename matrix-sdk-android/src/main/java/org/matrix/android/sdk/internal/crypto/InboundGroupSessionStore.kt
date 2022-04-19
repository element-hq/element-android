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
import kotlinx.coroutines.sync.Mutex
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.internal.crypto.model.OlmInboundGroupSessionWrapper2
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

internal data class InboundGroupSessionHolder(
        val wrapper: OlmInboundGroupSessionWrapper2,
        val mutex: Mutex = Mutex()
)

private val loggerTag = LoggerTag("InboundGroupSessionStore", LoggerTag.CRYPTO)

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

    private val sessionCache = object : LruCache<CacheKey, InboundGroupSessionHolder>(100) {
        override fun entryRemoved(evicted: Boolean, key: CacheKey?, oldValue: InboundGroupSessionHolder?, newValue: InboundGroupSessionHolder?) {
            if (oldValue != null) {
                cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
                    Timber.tag(loggerTag.value).v("## Inbound: entryRemoved ${oldValue.wrapper.roomId}-${oldValue.wrapper.senderKey}")
                    store.storeInboundGroupSessions(listOf(oldValue).map { it.wrapper })
                    oldValue.wrapper.olmInboundGroupSession?.releaseSession()
                }
            }
        }
    }

    private val timer = Timer()
    private var timerTask: TimerTask? = null

    private val dirtySession = mutableListOf<OlmInboundGroupSessionWrapper2>()

    @Synchronized
    fun clear() {
        sessionCache.evictAll()
    }

    @Synchronized
    fun getInboundGroupSession(sessionId: String, senderKey: String): InboundGroupSessionHolder? {
            val known = sessionCache[CacheKey(sessionId, senderKey)]
            Timber.tag(loggerTag.value).v("## Inbound: getInboundGroupSession  $sessionId in cache ${known != null}")
            return known
                    ?: store.getInboundGroupSession(sessionId, senderKey)?.also {
                Timber.tag(loggerTag.value).v("## Inbound: getInboundGroupSession cache populate ${it.roomId}")
                sessionCache.put(CacheKey(sessionId, senderKey), InboundGroupSessionHolder(it))
            }?.let {
                InboundGroupSessionHolder(it)
            }
    }

    @Synchronized
    fun replaceGroupSession(old: InboundGroupSessionHolder, new: InboundGroupSessionHolder, sessionId: String, senderKey: String) {
        Timber.tag(loggerTag.value).v("## Replacing outdated session ${old.wrapper.roomId}-${old.wrapper.senderKey}")
        dirtySession.remove(old.wrapper)
        store.removeInboundGroupSession(sessionId, senderKey)
        sessionCache.remove(CacheKey(sessionId, senderKey))

        // release removed session
        old.wrapper.olmInboundGroupSession?.releaseSession()

        internalStoreGroupSession(new, sessionId, senderKey)
    }

    @Synchronized
    fun storeInBoundGroupSession(holder: InboundGroupSessionHolder, sessionId: String, senderKey: String) {
        internalStoreGroupSession(holder, sessionId, senderKey)
    }

    private fun internalStoreGroupSession(holder: InboundGroupSessionHolder, sessionId: String, senderKey: String) {
        Timber.tag(loggerTag.value).v("## Inbound: getInboundGroupSession mark as dirty ${holder.wrapper.roomId}-${holder.wrapper.senderKey}")
        // We want to batch this a bit for performances
        dirtySession.add(holder.wrapper)

        if (sessionCache[CacheKey(sessionId, senderKey)] == null) {
            // first time seen, put it in memory cache while waiting for batch insert
            // If it's already known, no need to update cache it's already there
            sessionCache.put(CacheKey(sessionId, senderKey), holder)
        }

        timerTask?.cancel()
        timerTask = object : TimerTask() {
            override fun run() {
                batchSave()
            }
        }
        timer.schedule(timerTask!!, 300)
    }

    @Synchronized
    private fun batchSave() {
        val toSave = mutableListOf<OlmInboundGroupSessionWrapper2>().apply { addAll(dirtySession) }
        dirtySession.clear()
        cryptoCoroutineScope.launch(coroutineDispatchers.crypto) {
            Timber.tag(loggerTag.value).v("## Inbound: getInboundGroupSession batching save of ${toSave.size}")
            tryOrNull {
                store.storeInboundGroupSessions(toSave)
            }
        }
    }
}
