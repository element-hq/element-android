/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room

import org.matrix.android.sdk.api.query.QueryStateEventValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * Get a TimelineEvent using the TimelineService of a Room.
 * @param eventId The id of the event to retrieve
 */
fun Room.getTimelineEvent(eventId: String): TimelineEvent? =
        timelineService().getTimelineEvent(eventId)

/**
 * Get a StateEvent using the StateService of a Room.
 * @param eventType The type of the event, see [org.matrix.android.sdk.api.session.events.model.EventType].
 * @param stateKey the query which will be done on the stateKey.
 */
fun Room.getStateEvent(eventType: String, stateKey: QueryStateEventValue): Event? =
        stateService().getStateEvent(eventType, stateKey)
