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

import androidx.paging.PagedList
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEventInterceptor
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.internal.database.model.ChunkEntityFields
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.PagingRequestHelper
import im.vector.matrix.android.internal.util.tryTransactionAsync
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.Sort

private const val PAGE_SIZE = 100
private const val PREFETCH_DISTANCE = 30
private const val EVENT_NOT_FOUND_INDEX = -1

internal class DefaultTimelineService(private val roomId: String,
                                      private val monarchy: Monarchy,
                                      private val taskExecutor: TaskExecutor,
                                      private val contextOfEventTask: GetContextOfEventTask,
                                      private val timelineEventFactory: TimelineEventFactory,
                                      private val paginationTask: PaginationTask,
                                      private val helper: PagingRequestHelper
) : TimelineService {

    private val eventInterceptors = ArrayList<TimelineEventInterceptor>()

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

    private fun buildPagedListConfig(): PagedList.Config {
        return PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(PAGE_SIZE)
                .setInitialLoadSizeHint(2 * PAGE_SIZE)
                .setPrefetchDistance(PREFETCH_DISTANCE)
                .build()
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

    private fun buildDataSourceFactoryQuery(realm: Realm, eventId: String?): RealmQuery<EventEntity> {
        val query = if (eventId == null) {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.LINKED_ONLY)
                    .equalTo("${EventEntityFields.CHUNK}.${ChunkEntityFields.IS_LAST}", true)
        } else {
            EventEntity
                    .where(realm, roomId = roomId, linkFilterMode = EventEntity.LinkFilterMode.BOTH)
                    .`in`("${EventEntityFields.CHUNK}.${ChunkEntityFields.EVENTS.EVENT_ID}", arrayOf(eventId))
        }
        return query.sort(EventEntityFields.DISPLAY_INDEX, Sort.DESCENDING)
    }


}