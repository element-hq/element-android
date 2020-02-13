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

package im.vector.matrix.android.internal.session.sync

import dagger.Lazy
import im.vector.matrix.android.api.session.events.model.Event
import javax.inject.Inject

interface RoomEventsProcessor {

    suspend fun process(mode: Mode, roomId: String, events: List<Event>)

    enum class Mode {
        PAGINATING,
        INITIAL_SYNC,
        INCREMENTAL_SYNC,
        LOCAL_ECHO
    }
}

class RoomEventsProcessors @Inject constructor(private val roomEventsProcessors: Lazy<Set<@JvmSuppressWildcards RoomEventsProcessor>>) : RoomEventsProcessor {

    override suspend fun process(mode: RoomEventsProcessor.Mode, roomId: String, events: List<Event>) {
        roomEventsProcessors.get().forEach {
            it.process(mode, roomId, events)
        }
    }
}
