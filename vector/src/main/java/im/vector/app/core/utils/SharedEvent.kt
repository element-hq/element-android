/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import im.vector.app.core.platform.VectorViewEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import java.util.concurrent.CopyOnWriteArraySet

interface SharedEvents<out T : VectorViewEvents> {
    fun stream(consumerId: String): Flow<T>
}

class EventQueue<T : VectorViewEvents>(capacity: Int) : SharedEvents<T> {

    private val innerQueue = MutableSharedFlow<OneTimeEvent<T>>(replay = capacity)

    fun post(event: T) {
        innerQueue.tryEmit(OneTimeEvent(event))
    }

    override fun stream(consumerId: String): Flow<T> = innerQueue
            .onEach {
                // Ensure that buffered Events will not be sent again to new subscribers.
                innerQueue.resetReplayCache()
            }
            .filterNotHandledBy(consumerId)
}

/**
 * Event designed to be delivered only once to a concrete entity,
 * but it can also be delivered to multiple different entities.
 *
 * Keeps track of who has already handled its content.
 */
private class OneTimeEvent<out T : VectorViewEvents>(private val content: T) {

    private val handlers = CopyOnWriteArraySet<String>()

    /**
     * @param asker Used to identify, whether this "asker" has already handled this Event.
     * @return Event content or null if it has been already handled by asker
     */
    fun getIfNotHandled(asker: String): T? = if (handlers.add(asker)) content else null
}

private fun <T : VectorViewEvents> Flow<OneTimeEvent<T>>.filterNotHandledBy(consumerId: String): Flow<T> = transform { event ->
    event.getIfNotHandled(consumerId)?.let { emit(it) }
}
