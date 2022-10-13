/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.threads.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.matrix.android.sdk.api.session.room.threads.local.ThreadsLocalService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.helper.findAllLocalThreadNotificationsForRoomId
import org.matrix.android.sdk.internal.database.helper.findAllThreadsForRoomId
import org.matrix.android.sdk.internal.database.helper.isUserParticipatingInThread
import org.matrix.android.sdk.internal.database.helper.mapEventsWithEdition
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId

internal class DefaultThreadsLocalService @AssistedInject constructor(
        @Assisted private val roomId: String,
        @UserId private val userId: String,
        @SessionDatabase private val realmInstance: RealmInstance,
        private val timelineEventMapper: TimelineEventMapper,
) : ThreadsLocalService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultThreadsLocalService
    }

    override fun getMarkedThreadNotificationsLive(): LiveData<List<TimelineEvent>> {
        return realmInstance.queryList(timelineEventMapper::map) {
            TimelineEventEntity.findAllLocalThreadNotificationsForRoomId(it, roomId = roomId)
        }.asLiveData()
    }

    override fun getMarkedThreadNotifications(): List<TimelineEvent> {
        val realm = realmInstance.getBlockingRealm()
        return TimelineEventEntity.findAllLocalThreadNotificationsForRoomId(realm, roomId = roomId)
                .find()
                .map(timelineEventMapper::map)
    }

    override fun getAllThreadsLive(): LiveData<List<TimelineEvent>> {
        return realmInstance.queryList(timelineEventMapper::map) {
            TimelineEventEntity.findAllThreadsForRoomId(it, roomId = roomId)
        }.asLiveData()
    }

    override fun getAllThreads(): List<TimelineEvent> {
        val realm = realmInstance.getBlockingRealm()
        return TimelineEventEntity.findAllThreadsForRoomId(realm, roomId = roomId)
                .find()
                .map(timelineEventMapper::map)
    }

    override fun isUserParticipatingInThread(rootThreadEventId: String): Boolean {
        val realm = realmInstance.getBlockingRealm()
        return TimelineEventEntity.isUserParticipatingInThread(
                realm = realm,
                roomId = roomId,
                rootThreadEventId = rootThreadEventId,
                senderId = userId
        )
    }

    override fun mapEventsWithEdition(threads: List<TimelineEvent>): List<TimelineEvent> {
        val realm = realmInstance.getBlockingRealm()
        return threads.mapEventsWithEdition(realm, roomId)
    }

    override suspend fun markThreadAsRead(rootThreadEventId: String) {
        realmInstance.write {
            EventEntity.where(
                    realm = this,
                    eventId = rootThreadEventId
            ).first().find()?.threadNotificationState = ThreadNotificationState.NO_NEW_MESSAGE
        }
    }
}
