/*
 * Copyright 2019 New Vector Ltd
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineService
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.crypto.store.db.doWithRealm
import org.matrix.android.sdk.internal.database.mapper.ReadReceiptsSummaryMapper
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.fetchCopyMap
import io.realm.Sort
import io.realm.kotlin.where
import org.greenrobot.eventbus.EventBus

internal class DefaultTimelineService @AssistedInject constructor(@Assisted private val roomId: String,
                                                                  @SessionDatabase private val monarchy: Monarchy,
                                                                  private val eventBus: EventBus,
                                                                  private val taskExecutor: TaskExecutor,
                                                                  private val contextOfEventTask: GetContextOfEventTask,
                                                                  private val eventDecryptor: TimelineEventDecryptor,
                                                                  private val paginationTask: PaginationTask,
                                                                  private val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
                                                                  private val timelineEventMapper: TimelineEventMapper,
                                                                  private val readReceiptsSummaryMapper: ReadReceiptsSummaryMapper
) : TimelineService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): TimelineService
    }

    override fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline {
        return DefaultTimeline(
                roomId = roomId,
                initialEventId = eventId,
                realmConfiguration = monarchy.realmConfiguration,
                taskExecutor = taskExecutor,
                contextOfEventTask = contextOfEventTask,
                paginationTask = paginationTask,
                timelineEventMapper = timelineEventMapper,
                settings = settings,
                hiddenReadReceipts = TimelineHiddenReadReceipts(readReceiptsSummaryMapper, roomId, settings),
                eventBus = eventBus,
                eventDecryptor = eventDecryptor,
                fetchTokenAndPaginateTask = fetchTokenAndPaginateTask
        )
    }

    override fun getTimeLineEvent(eventId: String): TimelineEvent? {
        return monarchy
                .fetchCopyMap({
                    TimelineEventEntity.where(it, roomId = roomId, eventId = eventId).findFirst()
                }, { entity, _ ->
                    timelineEventMapper.map(entity)
                })
    }

    override fun getTimeLineEventLive(eventId: String): LiveData<Optional<TimelineEvent>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { TimelineEventEntity.where(it, roomId = roomId, eventId = eventId) },
                { timelineEventMapper.map(it) }
        )
        return Transformations.map(liveData) { events ->
            events.firstOrNull().toOptional()
        }
    }

    override fun getAttachmentMessages(): List<TimelineEvent> {
        // TODO pretty bad query.. maybe we should denormalize clear type in base?
        return doWithRealm(monarchy.realmConfiguration) { realm ->
            realm.where<TimelineEventEntity>()
                    .equalTo(TimelineEventEntityFields.ROOM_ID, roomId)
                    .sort(TimelineEventEntityFields.DISPLAY_INDEX, Sort.ASCENDING)
                    .findAll()
                    ?.mapNotNull { timelineEventMapper.map(it).takeIf { it.root.isImageMessage() || it.root.isVideoMessage() } }
                    ?: emptyList()
        }
    }
}
