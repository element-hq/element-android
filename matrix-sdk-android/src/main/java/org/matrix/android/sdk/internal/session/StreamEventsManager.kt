/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.LiveEventListener
import org.matrix.android.sdk.api.session.crypto.model.MXEventDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class StreamEventsManager @Inject constructor() {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val listeners = mutableListOf<LiveEventListener>()

    fun addLiveEventListener(listener: LiveEventListener) {
        listeners.add(listener)
    }

    fun removeLiveEventListener(listener: LiveEventListener) {
        listeners.remove(listener)
    }

    fun dispatchLiveEventReceived(event: Event, roomId: String, initialSync: Boolean) {
        Timber.v("## dispatchLiveEventReceived ${event.eventId}")
        coroutineScope.launch {
            if (!initialSync) {
                listeners.forEach {
                    tryOrNull {
                        it.onLiveEvent(roomId, event)
                    }
                }
            }
        }
    }

    fun dispatchPaginatedEventReceived(event: Event, roomId: String) {
        Timber.v("## dispatchPaginatedEventReceived ${event.eventId}")
        coroutineScope.launch {
            listeners.forEach {
                tryOrNull {
                    it.onPaginatedEvent(roomId, event)
                }
            }
        }
    }

    fun dispatchLiveEventDecrypted(event: Event, result: MXEventDecryptionResult) {
        Timber.v("## dispatchLiveEventDecrypted ${event.eventId}")
        coroutineScope.launch {
            listeners.forEach {
                tryOrNull {
                    it.onEventDecrypted(event, result.clearEvent)
                }
            }
        }
    }

    fun dispatchLiveEventDecryptionFailed(event: Event, error: Throwable) {
        Timber.v("## dispatchLiveEventDecryptionFailed ${event.eventId}")
        coroutineScope.launch {
            listeners.forEach {
                tryOrNull {
                    it.onEventDecryptionError(event, error)
                }
            }
        }
    }

    fun dispatchOnLiveToDevice(event: Event) {
        Timber.v("## dispatchOnLiveToDevice ${event.eventId}")
        coroutineScope.launch {
            listeners.forEach {
                tryOrNull {
                    it.onLiveToDeviceEvent(event)
                }
            }
        }
    }
}
