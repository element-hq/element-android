/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventsGroup
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryEvents
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

data class TimelineItemFactoryParams(
        val event: TimelineEvent,
        val prevEvent: TimelineEvent? = null,
        val prevDisplayableEvent: TimelineEvent? = null,
        val nextEvent: TimelineEvent? = null,
        val nextDisplayableEvent: TimelineEvent? = null,
        val partialState: TimelineEventController.PartialState = TimelineEventController.PartialState(),
        val lastSentEventIdWithoutReadReceipts: String? = null,
        val callback: TimelineEventController.Callback? = null,
        val reactionsSummaryEvents: ReactionsSummaryEvents? = null,
        val eventsGroup: TimelineEventsGroup? = null
) {

    val highlightedEventId: String?
        get() = partialState.highlightedEventId

    val rootThreadEventId: String?
        get() = partialState.rootThreadEventId

    val isHighlighted = highlightedEventId == event.eventId

    fun isFromThreadTimeline(): Boolean = rootThreadEventId != null
}
