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

package org.matrix.android.sdk.internal.session.room.create

import com.zhuinden.monarchy.Monarchy
import io.realm.kotlin.where
import kotlinx.coroutines.TimeoutCancellationException
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams
import org.matrix.android.sdk.api.session.room.model.tombstone.RoomTombstoneContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.internal.database.awaitNotEmptyResult
import org.matrix.android.sdk.internal.database.mapper.toEntity
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntity
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.copyToRealmOrIgnore
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.whereRoomId
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.state.StateEventDataSource
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Create a room on the server from a local room.
 * The configuration of the local room will be use to configure the new room.
 * The potential local room members will also be invited to this new room.
 *
 * A local tombstone event will be created to indicate that the local room has been replacing by the new one.
 */
internal interface CreateRoomFromLocalRoomTask : Task<CreateRoomFromLocalRoomTask.Params, String> {
    data class Params(val localRoomId: String)
}

internal class DefaultCreateRoomFromLocalRoomTask @Inject constructor(
        @UserId private val userId: String,
        @SessionDatabase private val monarchy: Monarchy,
        private val createRoomTask: CreateRoomTask,
        private val stateEventDataSource: StateEventDataSource,
        private val clock: Clock,
) : CreateRoomFromLocalRoomTask {

    private val realmConfiguration
        get() = monarchy.realmConfiguration

    override suspend fun execute(params: CreateRoomFromLocalRoomTask.Params): String {
        val replacementRoomId = stateEventDataSource.getStateEvent(params.localRoomId, EventType.STATE_ROOM_TOMBSTONE, QueryStringValue.IsEmpty)
                ?.content.toModel<RoomTombstoneContent>()
                ?.replacementRoomId

        if (replacementRoomId != null) {
            return replacementRoomId
        }

        var createRoomParams: CreateRoomParams? = null
        var isEncrypted = false
        monarchy.doWithRealm { realm ->
            realm.where<LocalRoomSummaryEntity>()
                    .equalTo(LocalRoomSummaryEntityFields.ROOM_ID, params.localRoomId)
                    .findFirst()
                    ?.let {
                        createRoomParams = it.createRoomParams
                        isEncrypted = it.roomSummaryEntity?.isEncrypted.orFalse()
                    }
        }
        val roomId = createRoomTask.execute(createRoomParams!!)

        try {
            // Wait for all the room events before triggering the replacement room
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                realm.where(RoomSummaryEntity::class.java)
                        .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
                        .equalTo(RoomSummaryEntityFields.INVITED_MEMBERS_COUNT, createRoomParams?.invitedUserIds?.size ?: 0)
            }
            awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                EventEntity.whereRoomId(realm, roomId)
                        .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_HISTORY_VISIBILITY)
            }
            if (isEncrypted) {
                awaitNotEmptyResult(realmConfiguration, TimeUnit.MINUTES.toMillis(1L)) { realm ->
                    EventEntity.whereRoomId(realm, roomId)
                            .equalTo(EventEntityFields.TYPE, EventType.STATE_ROOM_ENCRYPTION)
                }
            }
        } catch (exception: TimeoutCancellationException) {
            throw CreateRoomFailure.CreatedWithTimeout(roomId)
        }

        createTombstoneEvent(params, roomId)
        return roomId
    }

    /**
     * Create a Tombstone event to indicate that the local room has been replaced by a new one.
     */
    private suspend fun createTombstoneEvent(params: CreateRoomFromLocalRoomTask.Params, roomId: String) {
        val now = clock.epochMillis()
        val event = Event(
                type = EventType.STATE_ROOM_TOMBSTONE,
                senderId = userId,
                originServerTs = now,
                stateKey = "",
                eventId = UUID.randomUUID().toString(),
                content = RoomTombstoneContent(
                        replacementRoomId = roomId
                ).toContent()
        )
        monarchy.awaitTransaction { realm ->
            val eventEntity = event.toEntity(params.localRoomId, SendState.SYNCED, now).copyToRealmOrIgnore(realm, EventInsertType.INCREMENTAL_SYNC)
            if (event.stateKey != null && event.type != null && event.eventId != null) {
                CurrentStateEventEntity.getOrCreate(realm, params.localRoomId, event.stateKey, event.type).apply {
                    eventId = event.eventId
                    root = eventEntity
                }
            }
        }
    }
}
