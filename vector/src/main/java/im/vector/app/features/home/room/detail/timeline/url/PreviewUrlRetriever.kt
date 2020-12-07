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

package im.vector.app.features.home.room.detail.timeline.url

import im.vector.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.Event

class PreviewUrlRetriever(session: Session) {
    private val mediaService = session.mediaService()

    private val data = mutableMapOf<String, PreviewUrlUiState>()
    private val listeners = mutableMapOf<String, MutableSet<PreviewUrlRetrieverListener>>()

    // In memory list
    private val blockedUrl = mutableSetOf<String>()

    fun getPreviewUrl(event: Event, coroutineScope: CoroutineScope) {
        val eventId = event.eventId ?: return

        synchronized(data) {
            if (data[eventId] == null) {
                // Keep only the first URL for the moment
                val url = mediaService.extractUrls(event)
                        .firstOrNull()
                        ?.takeIf { it !in blockedUrl }
                if (url == null) {
                    updateState(eventId, PreviewUrlUiState.NoUrl)
                } else {
                    updateState(eventId, PreviewUrlUiState.Loading)
                }
                url
            } else {
                // Already handled
                null
            }
        }?.let { urlToRetrieve ->
            coroutineScope.launch {
                runCatching {
                    mediaService.getPreviewUrl(
                            url = urlToRetrieve,
                            timestamp = null,
                            cacheStrategy = if (BuildConfig.DEBUG) CacheStrategy.NoCache else CacheStrategy.TtlCache(CACHE_VALIDITY, false)
                    )
                }.fold(
                        {
                            synchronized(data) {
                                // Blocked after the request has been sent?
                                if (urlToRetrieve in blockedUrl) {
                                    updateState(eventId, PreviewUrlUiState.NoUrl)
                                } else {
                                    updateState(eventId, PreviewUrlUiState.Data(eventId, urlToRetrieve, it))
                                }
                            }
                        },
                        {
                            synchronized(data) {
                                updateState(eventId, PreviewUrlUiState.Error(it))
                            }
                        }
                )
            }
        }
    }

    fun doNotShowPreviewUrlFor(eventId: String, url: String) {
        blockedUrl.add(url)

        // Notify the listener
        synchronized(data) {
            data[eventId]
                    ?.takeIf { it is PreviewUrlUiState.Data && it.url == url }
                    ?.let {
                        updateState(eventId, PreviewUrlUiState.NoUrl)
                    }
        }
    }

    private fun updateState(eventId: String, state: PreviewUrlUiState) {
        data[eventId] = state
        // Notify the listener
        listeners[eventId].orEmpty().forEach {
            it.onStateUpdated(state)
        }
    }

    // Called by the Epoxy item during binding
    fun addListener(key: String, listener: PreviewUrlRetrieverListener) {
        listeners.getOrPut(key) { mutableSetOf() }.add(listener)

        // Give the current state if any
        synchronized(data) {
            listener.onStateUpdated(data[key] ?: PreviewUrlUiState.Unknown)
        }
    }

    // Called by the Epoxy item during unbinding
    fun removeListener(key: String, listener: PreviewUrlRetrieverListener) {
        listeners[key]?.remove(listener)
    }

    interface PreviewUrlRetrieverListener {
        fun onStateUpdated(state: PreviewUrlUiState)
    }

    companion object {
        // One week in millis
        private const val CACHE_VALIDITY: Long = 7 * 24 * 3_600 * 1_000
    }
}
