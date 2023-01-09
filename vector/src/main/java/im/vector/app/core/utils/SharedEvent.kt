/*
 * Copyright 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transform
import java.util.concurrent.CopyOnWriteArraySet

interface SharedEvents<out T> {
    fun stream(consumerId: String): Flow<T>
}

class EventQueue<T>(capacity: Int) : SharedEvents<T> {

    private val innerQueue = MutableSharedFlow<OneTimeEvent<T>>(replay = capacity)

    fun post(event: T) {
        innerQueue.tryEmit(OneTimeEvent(event))
    }

    override fun stream(consumerId: String): Flow<T> = innerQueue.filterNotHandledBy(consumerId)
}

/**
 * Event designed to be delivered only once to a concrete entity,
 * but it can also be delivered to multiple different entities.
 *
 * Keeps track of who has already handled its content.
 */
private class OneTimeEvent<out T>(private val content: T) {

    private val handlers = CopyOnWriteArraySet<String>()

    /**
     * @param asker Used to identify, whether this "asker" has already handled this Event.
     * @return Event content or null if it has been already handled by asker
     */
    fun getIfNotHandled(asker: String): T? = if (handlers.add(asker)) content else null
}

private fun <T> Flow<OneTimeEvent<T>>.filterNotHandledBy(consumerId: String): Flow<T> = transform { event ->
    event.getIfNotHandled(consumerId)?.let { emit(it) }
}
