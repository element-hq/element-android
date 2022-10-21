/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asLiveData
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import org.matrix.android.sdk.api.session.events.model.LocalEcho
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.mapper.TimelineEventMapper
import org.matrix.android.sdk.internal.database.model.TimelineEventEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.util.mapOptional

/**
 * This class takes care of handling case where local echo is replaced by the synced event in the db.
 */
internal class LiveTimelineEvent(
        private val realmInstance: RealmInstance,
        private val timelineEventMapper: TimelineEventMapper,
        private val roomId: String,
        private val eventId: String
) :
        MediatorLiveData<Optional<TimelineEvent>>() {

    init {
        buildAndObserveQuery()
    }

    private var initialLiveData: LiveData<Optional<TimelineEvent>>? = null

    private fun buildAndObserveQuery() {
        val liveData = realmInstance.queryFirstMapped(timelineEventMapper::map) {
            TimelineEventEntity.where(it, roomId = roomId, eventId = eventId).first()
        }.asLiveData()

        addSource(liveData) { newValue ->
            value = newValue
        }
        initialLiveData = liveData
        if (LocalEcho.isLocalEchoId(eventId)) {
            observeTimelineEventWithTxId()
        }
    }

    private fun observeTimelineEventWithTxId() {
        val liveData = realmInstance.queryFirstMapped(timelineEventMapper::map) {
            it.queryTimelineEventWithTxId().first()
        }.asLiveData()
        addSource(liveData) { newValue ->
            if (newValue.hasValue()) {
                initialLiveData?.also { removeSource(it) }
                value = newValue
            }
        }
    }

    private fun TypedRealm.queryTimelineEventWithTxId(): RealmQuery<TimelineEventEntity> {
        return query(TimelineEventEntity::class)
                .query("roomId == $0", roomId)
                .query("root.unsignedData LIKE $0", """{*"transaction_id":*"$eventId"*}""")
    }
}
