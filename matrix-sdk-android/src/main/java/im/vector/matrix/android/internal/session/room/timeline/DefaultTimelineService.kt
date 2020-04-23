/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.timeline

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.api.util.Optional
import kotlinx.coroutines.flow.Flow

internal class DefaultTimelineService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                  private val timelineEventDataSource: TimelineEventDataSource,
                                                                  private val sqlTimelineFactory: SQLTimeline.Factory
) : TimelineService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): TimelineService
    }

    override fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline {
        return sqlTimelineFactory.create(roomId, eventId, settings)
    }

    override fun getTimeLineEvent(eventId: String): TimelineEvent? {
        return timelineEventDataSource.getTimeLineEvent(eventId)
    }

    override fun getTimeLineEventLive(eventId: String): Flow<Optional<TimelineEvent>> {
        return timelineEventDataSource.getTimeLineEventLive(eventId)
    }
}
