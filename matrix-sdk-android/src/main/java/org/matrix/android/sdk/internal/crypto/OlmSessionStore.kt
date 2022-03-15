/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.internal.crypto.model.OlmSessionWrapper
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.olm.OlmSession
import timber.log.Timber
import javax.inject.Inject

private val loggerTag = LoggerTag("OlmSessionStore", LoggerTag.CRYPTO)

/**
 * Keep the used olm session in memory and load them from the data layer when needed
 * Access is synchronized for thread safety
 */
internal class OlmSessionStore @Inject constructor(private val store: IMXCryptoStore) {
    /**
     * map of device key to list of olm sessions (it is possible to have several active sessions with a device)
     */
    private val olmSessions = HashMap<String, MutableList<OlmSessionWrapper>>()

    /**
     * Store a session between our own device and another device.
     * This will be called after the session has been created but also every time it has been used
     * in order to persist the correct state for next run
     * @param olmSessionWrapper   the end-to-end session.
     * @param deviceKey the public key of the other device.
     */
    @Synchronized
    fun storeSession(olmSessionWrapper: OlmSessionWrapper, deviceKey: String) {
        // This could be a newly created session or one that was just created
        // Anyhow we should persist ratchet state for future app lifecycle
        addNewSessionInCache(olmSessionWrapper, deviceKey)
        store.storeSession(olmSessionWrapper, deviceKey)
    }

    /**
     * Get all the Olm Sessions we are sharing with the given device.
     *
     * @param deviceKey the public key of the other device.
     * @return A set of sessionId, or empty if device is not known
     */
    @Synchronized
    fun getDeviceSessionIds(deviceKey: String): List<String> {
        // we need to get the persisted ids first
        val persistedKnownSessions = store.getDeviceSessionIds(deviceKey)
                .orEmpty()
                .toMutableList()
        // Do we have some in cache not yet persisted?
        olmSessions.getOrPut(deviceKey) { mutableListOf() }.forEach { cached ->
            getSafeSessionIdentifier(cached.olmSession)?.let { cachedSessionId ->
                if (!persistedKnownSessions.contains(cachedSessionId)) {
                    persistedKnownSessions.add(cachedSessionId)
                }
            }
        }
        return persistedKnownSessions
    }

    /**
     * Retrieve an end-to-end session between our own device and another
     * device.
     *
     * @param sessionId the session Id.
     * @param deviceKey the public key of the other device.
     * @return the session wrapper if found
     */
    @Synchronized
    fun getDeviceSession(sessionId: String, deviceKey: String): OlmSessionWrapper? {
        // get from cache or load and add to cache
        return internalGetSession(sessionId, deviceKey)
    }

    /**
     * Retrieve the last used sessionId, regarding `lastReceivedMessageTs`, or null if no session exist
     *
     * @param deviceKey the public key of the other device.
     * @return last used sessionId, or null if not found
     */
    @Synchronized
    fun getLastUsedSessionId(deviceKey: String): String? {
        // We want to avoid to load in memory old session if possible
        val lastPersistedUsedSession = store.getLastUsedSessionId(deviceKey)
        var candidate = lastPersistedUsedSession?.let { internalGetSession(it, deviceKey) }
        // we should check if we have one in cache with a higher last message received?
        olmSessions[deviceKey].orEmpty().forEach { inCache ->
            if (inCache.lastReceivedMessageTs > (candidate?.lastReceivedMessageTs ?: 0L)) {
                candidate = inCache
            }
        }

        return candidate?.olmSession?.sessionIdentifier()
    }

    /**
     * Release all sessions and clear cache
     */
    @Synchronized
    fun clear() {
        olmSessions.entries.onEach { entry ->
            entry.value.onEach { it.olmSession.releaseSession() }
        }
        olmSessions.clear()
    }

    private fun internalGetSession(sessionId: String, deviceKey: String): OlmSessionWrapper? {
        return getSessionInCache(sessionId, deviceKey)
                ?: // deserialize from store
                return store.getDeviceSession(sessionId, deviceKey)?.also {
                    addNewSessionInCache(it, deviceKey)
                }
    }

    private fun getSessionInCache(sessionId: String, deviceKey: String): OlmSessionWrapper? {
        return olmSessions[deviceKey]?.firstOrNull {
            getSafeSessionIdentifier(it.olmSession) == sessionId
        }
    }

    private fun getSafeSessionIdentifier(session: OlmSession): String? {
        return try {
            session.sessionIdentifier()
        } catch (throwable: Throwable) {
            Timber.tag(loggerTag.value).w("Failed to load sessionId from loaded olm session")
            null
        }
    }

    private fun addNewSessionInCache(session: OlmSessionWrapper, deviceKey: String) {
        val sessionId = getSafeSessionIdentifier(session.olmSession) ?: return
        olmSessions.getOrPut(deviceKey) { mutableListOf() }.let {
            val existing = it.firstOrNull { getSafeSessionIdentifier(it.olmSession) == sessionId }
            it.add(session)
            // remove and release if was there but with different instance
            if (existing != null && existing.olmSession != session.olmSession) {
                // mm not sure when this could happen
                // anyhow we should remove and release the one known
                it.remove(existing)
                existing.olmSession.releaseSession()
            }
        }
    }
}
