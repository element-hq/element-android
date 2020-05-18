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

package im.vector.matrix.android.internal.session.room.timeline

import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class TimelineInput @Inject constructor() {
    fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent) {
        listeners.toSet().forEach { it.onLocalEchoCreated(roomId, timelineEvent) }
    }

    fun onNewTimelineEvents(roomId: String, eventIds: List<String>) {
        listeners.toSet().forEach { it.onNewTimelineEvents(roomId, eventIds) }
    }

    val listeners = mutableSetOf<Listener>()

    internal interface Listener {
        fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent)
        fun onNewTimelineEvents(roomId: String, eventIds: List<String>)
    }
}
