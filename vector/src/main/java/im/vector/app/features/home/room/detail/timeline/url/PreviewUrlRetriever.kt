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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLatestEventId

class PreviewUrlRetriever(session: Session,
                          private val coroutineScope: CoroutineScope) {
    private val mediaService = session.mediaService()

    private data class EventIdPreviewUrlUiState(
            // Id of the latest event in the case of an edited event, or the eventId for an event which has not been edited
            val latestEventId: String,
            val previewUrlUiState: PreviewUrlUiState
    )

    // Keys are the main eventId
    private val data = mutableMapOf<String, EventIdPreviewUrlUiState>()
    private val listeners = mutableMapOf<String, MutableSet<PreviewUrlRetrieverListener>>()

    // In memory list
    private val blockedUrl = mutableSetOf<String>()

    fun getPreviewUrl(event: TimelineEvent) {
        val eventId = event.root.eventId ?: return
        val latestEventId = event.getLatestEventId()

        synchronized(data) {
            val current = data[eventId]
            if (current?.latestEventId != latestEventId) {
                // The event is not known or it has been edited
                // Keep only the first URL for the moment
                val url = mediaService.extractUrls(event)
                        .firstOrNull { canShowUrlPreview(it) }
                        ?.takeIf { it !in blockedUrl }
                if (url == null) {
                    updateState(eventId, latestEventId, PreviewUrlUiState.NoUrl)
                    null
                } else if (url != (current?.previewUrlUiState as? PreviewUrlUiState.Data)?.url) {
                    // There is a not known URL, or the Event has been edited and the URL has changed
                    updateState(eventId, latestEventId, PreviewUrlUiState.Loading)
                    url
                } else {
                    // Already handled
                    null
                }
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
                                    updateState(eventId, latestEventId, PreviewUrlUiState.NoUrl)
                                } else {
                                    updateState(eventId, latestEventId, PreviewUrlUiState.Data(eventId, urlToRetrieve, it))
                                }
                            }
                        },
                        {
                            synchronized(data) {
                                updateState(eventId, latestEventId, PreviewUrlUiState.Error(it))
                            }
                        }
                )
            }
        }
    }

    private fun canShowUrlPreview(url: String): Boolean {
        return blockedDomains.all { !url.startsWith(it) }
    }

    fun doNotShowPreviewUrlFor(eventId: String, url: String) {
        blockedUrl.add(url)

        // Notify the listener
        synchronized(data) {
            data[eventId]
                    ?.takeIf { it.previewUrlUiState is PreviewUrlUiState.Data && it.previewUrlUiState.url == url }
                    ?.let {
                        updateState(eventId, it.latestEventId, PreviewUrlUiState.NoUrl)
                    }
        }
    }

    private fun updateState(eventId: String, latestEventId: String, state: PreviewUrlUiState) {
        data[eventId] = EventIdPreviewUrlUiState(latestEventId, state)
        // Notify the listener
        coroutineScope.launch(Dispatchers.Main) {
            listeners[eventId].orEmpty().forEach {
                it.onStateUpdated(state)
            }
        }
    }

    // Called by the Epoxy item during binding
    fun addListener(key: String, listener: PreviewUrlRetrieverListener) {
        listeners.getOrPut(key) { mutableSetOf() }.add(listener)

        // Give the current state if any
        synchronized(data) {
            listener.onStateUpdated(data[key]?.previewUrlUiState ?: PreviewUrlUiState.Unknown)
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
        private const val CACHE_VALIDITY = 604_800_000L // 7 * 24 * 3_600 * 1_000

        private val blockedDomains = listOf(
                "https://matrix.to",
                "https://app.element.io",
                "https://staging.element.io",
                "https://develop.element.io"
        )
    }
}
