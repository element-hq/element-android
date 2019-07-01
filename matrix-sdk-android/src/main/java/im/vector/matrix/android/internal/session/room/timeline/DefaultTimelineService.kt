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
import androidx.lifecycle.MediatorLiveData
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.room.timeline.Timeline
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.TimelineService
import im.vector.matrix.android.internal.database.RealmLiveData
import im.vector.matrix.android.internal.database.model.EventAnnotationsSummaryEntity
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.fetchCopyMap
import javax.inject.Inject

internal class DefaultTimelineService @Inject constructor(private val roomId: String,
                                                          private val monarchy: Monarchy,
                                                          private val taskExecutor: TaskExecutor,
                                                          private val timelineEventFactory: CacheableTimelineEventFactory,
                                                          private val contextOfEventTask: GetContextOfEventTask,
                                                          private val paginationTask: PaginationTask
) : TimelineService {

    override fun createTimeline(eventId: String?, allowedTypes: List<String>?): Timeline {
        return DefaultTimeline(roomId, eventId, monarchy.realmConfiguration, taskExecutor, contextOfEventTask, timelineEventFactory, paginationTask, allowedTypes)
    }

    override fun getTimeLineEvent(eventId: String): TimelineEvent? {
        return monarchy
                .fetchCopyMap({
                    EventEntity.where(it, eventId = eventId).findFirst()
                }, { entity, realm ->
                    timelineEventFactory.create(entity, realm)
                })
    }

    override fun liveTimeLineEvent(eventId: String): LiveData<TimelineEvent> {
        val liveEventEntity = RealmLiveData(monarchy.realmConfiguration) { realm ->
            EventEntity.where(realm, eventId = eventId)
        }
        val liveAnnotationsEntity = RealmLiveData(monarchy.realmConfiguration) { realm ->
            EventAnnotationsSummaryEntity.where(realm, eventId = eventId)
        }
        val result = MediatorLiveData<TimelineEvent>()
        result.addSource(liveEventEntity) { realmResults ->
            result.value = realmResults.firstOrNull()?.let { timelineEventFactory.create(it, it.realm) }
        }

        result.addSource(liveAnnotationsEntity) {
            liveEventEntity.value?.let {
                result.value = liveEventEntity.value?.let { realmResults ->
                    //recreate the timeline event
                    realmResults.firstOrNull()?.let { timelineEventFactory.create(it, it.realm) }
                }
            }
        }
        return result
    }

}