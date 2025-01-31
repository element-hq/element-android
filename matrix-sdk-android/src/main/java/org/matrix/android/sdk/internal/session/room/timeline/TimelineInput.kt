/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
