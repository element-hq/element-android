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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.api.session.room.timeline.TimelineSettings
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.mapper.ReadReceiptsSummaryMapper
import im.vector.matrix.android.internal.database.mapper.TimelineEventMapper
import im.vector.matrix.android.internal.database.model.TimelineEventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.fetchCopyMap

internal class DefaultTimelineService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                  private val monarchy: Monarchy,
                                                                  private val taskExecutor: TaskExecutor,
                                                                  private val contextOfEventTask: GetContextOfEventTask,
                                                                  private val cryptoService: CryptoService,
                                                                  private val paginationTask: PaginationTask,
                                                                  private val timelineEventMapper: TimelineEventMapper,
                                                                  private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper
) : TimelineService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): TimelineService
    }

    override fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline {
        return DefaultTimeline(roomId,
                               eventId,
                               monarchy.realmConfiguration,
                               taskExecutor,
                               contextOfEventTask,
                               paginationTask,
                               cryptoService,
                               timelineEventMapper,
                               settings,
                               TimelineHiddenReadReceipts(readReceiptsSummaryMapper, roomId, settings)
        )
    }

    override fun getTimeLineEvent(eventId: String): TimelineEvent? {
        return monarchy
                .fetchCopyMap({
                                  TimelineEventEntity.where(it, eventId = eventId).findFirst()
                              }, { entity, realm ->
                                  timelineEventMapper.map(entity)
                              })
    }

    override fun liveTimeLineEvent(eventId: String): LiveData<TimelineEvent> {
        val liveData = RealmLiveData(monarchy.realmConfiguration) {
            TimelineEventEntity.where(it, eventId = eventId)
        }
        return Transformations.map(liveData) { events ->
            events.firstOrNull()?.let { timelineEventMapper.map(it) }
        }
    }

}