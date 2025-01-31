/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.events

import org.matrix.android.sdk.api.session.events.EventService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.internal.session.call.CallEventProcessor
import org.matrix.android.sdk.internal.session.room.timeline.GetEventTask
import javax.inject.Inject

internal class DefaultEventService @Inject constructor(
        private val getEventTask: GetEventTask,
        private val callEventProcessor: CallEventProcessor
) : EventService {

    override suspend fun getEvent(roomId: String, eventId: String): Event {
        val event = getEventTask.execute(GetEventTask.Params(roomId, eventId))
        // Fast lane to the call event processors: try to make the incoming call ring faster
        if (callEventProcessor.shouldProcessFastLane(event.getClearType())) {
            callEventProcessor.processFastLane(event)
        }

        return event
    }
}
