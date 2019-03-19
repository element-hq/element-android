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

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.PagingRequestHelper
import im.vector.matrix.android.internal.util.tryTransactionAsync

private const val EVENT_NOT_FOUND_INDEX = -1

internal class DefaultTimelineService(private val roomId: String,
                                      private val monarchy: Monarchy,
                                      private val taskExecutor: TaskExecutor,
                                      private val contextOfEventTask: GetContextOfEventTask,
                                      private val timelineEventFactory: TimelineEventFactory,
                                      private val paginationTask: PaginationTask,
                                      private val helper: PagingRequestHelper
) : TimelineService {

    override fun createTimeline(eventId: String?): Timeline {
        return DefaultTimeline(roomId, eventId, monarchy.realmConfiguration, taskExecutor, contextOfEventTask, timelineEventFactory, paginationTask, helper)
    }

    // PRIVATE FUNCTIONS ***************************************************************************

    private fun getInitialLoadKey(eventId: String?): Int {
        var initialLoadKey = 0
        if (eventId != null) {
            val indexOfEvent = indexOfEvent(eventId)
            if (indexOfEvent == EVENT_NOT_FOUND_INDEX) {
                fetchEvent(eventId)
            } else {
                initialLoadKey = indexOfEvent
            }
        }
        return initialLoadKey
    }


    private fun fetchEvent(eventId: String) {
        val params = GetContextOfEventTask.Params(roomId, eventId)
        contextOfEventTask.configureWith(params).executeBy(taskExecutor)
    }

    private fun clearUnlinkedEvents() {
        monarchy.tryTransactionAsync { realm ->
            val unlinkedEvents = EventEntity
                    .where(realm, roomId = roomId)
                    .equalTo(EventEntityFields.IS_UNLINKED, true)
                    .findAll()
            unlinkedEvents.deleteAllFromRealm()
        }
    }

    private fun indexOfEvent(eventId: String): Int {
        var displayIndex = EVENT_NOT_FOUND_INDEX
        monarchy.doWithRealm {
            displayIndex = EventEntity.where(it, eventId = eventId).findFirst()?.displayIndex
                           ?: EVENT_NOT_FOUND_INDEX
        }
        return displayIndex
    }


}