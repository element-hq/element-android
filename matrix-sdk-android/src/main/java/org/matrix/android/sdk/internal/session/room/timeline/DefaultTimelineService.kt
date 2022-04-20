/*
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
import com.zhuinden.monarchy.Monarchy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineService
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.matrix.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask
import org.matrix.android.sdk.internal.session.sync.handler.room.ReadReceiptHandler
import org.matrix.android.sdk.internal.session.sync.handler.room.ThreadsAwarenessHandler

internal class DefaultTimelineService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val timelineInput: TimelineInput,
        private val contextOfEventTask: GetContextOfEventTask,
        private val eventDecryptor: TimelineEventDecryptor,
        private val paginationTask: PaginationTask,
        private val fetchTokenAndPaginateTask: FetchTokenAndPaginateTask,
        private val fetchThreadTimelineTask: FetchThreadTimelineTask,
        private val timelineEventMapper: TimelineEventMapper,
        private val loadRoomMembersTask: LoadRoomMembersTask,
        private val threadsAwarenessHandler: ThreadsAwarenessHandler,
        private val lightweightSettingsStorage: LightweightSettingsStorage,
        private val readReceiptHandler: ReadReceiptHandler,
        private val coroutineDispatchers: MatrixCoroutineDispatchers,
        private val timelineEventDataSource: TimelineEventDataSource
) : TimelineService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultTimelineService
    }

    override fun createTimeline(eventId: String?, settings: TimelineSettings): Timeline {
        return DefaultTimeline(
                roomId = roomId,
                initialEventId = eventId,
                settings = settings,
                realmConfiguration = monarchy.realmConfiguration,
                coroutineDispatchers = coroutineDispatchers,
                paginationTask = paginationTask,
                fetchTokenAndPaginateTask = fetchTokenAndPaginateTask,
                timelineEventMapper = timelineEventMapper,
                timelineInput = timelineInput,
                eventDecryptor = eventDecryptor,
                fetchThreadTimelineTask = fetchThreadTimelineTask,
                loadRoomMembersTask = loadRoomMembersTask,
                readReceiptHandler = readReceiptHandler,
                getEventTask = contextOfEventTask,
                threadsAwarenessHandler = threadsAwarenessHandler,
                lightweightSettingsStorage = lightweightSettingsStorage
        )
    }

    override fun getTimelineEvent(eventId: String): TimelineEvent? {
        return timelineEventDataSource.getTimelineEvent(roomId, eventId)
    }

    override fun getTimelineEventLive(eventId: String): LiveData<Optional<TimelineEvent>> {
        return timelineEventDataSource.getTimelineEventLive(roomId, eventId)
    }

    override fun getAttachmentMessages(): List<TimelineEvent> {
        return timelineEventDataSource.getAttachmentMessages(roomId)
    }
}
