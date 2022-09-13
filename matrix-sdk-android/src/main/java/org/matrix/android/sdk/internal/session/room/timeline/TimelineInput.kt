/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class TimelineInput @Inject constructor() {

    val listeners = mutableSetOf<Listener>()

    fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent) {
        listeners.toSet().forEach { it.onLocalEchoCreated(roomId, timelineEvent) }
    }

    fun onLocalEchoUpdated(roomId: String, eventId: String, sendState: SendState) {
        listeners.toSet().forEach { it.onLocalEchoUpdated(roomId, eventId, sendState) }
    }

    fun onNewTimelineEvents(roomId: String, eventIds: List<String>) {
        listeners.toSet().forEach { it.onNewTimelineEvents(roomId, eventIds) }
    }

    internal interface Listener {
        fun onLocalEchoCreated(roomId: String, timelineEvent: TimelineEvent) = Unit
        fun onLocalEchoUpdated(roomId: String, eventId: String, sendState: SendState) = Unit
        fun onNewTimelineEvents(roomId: String, eventIds: List<String>) = Unit
    }
}
