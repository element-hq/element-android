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

package org.matrix.android.sdk.internal.session.room.tombstone

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.room.model.tombstone.RoomTombstoneContent
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import javax.inject.Inject

internal class RoomTombstoneEventProcessor @Inject constructor() : EventInsertLiveProcessor {

    override suspend fun process(realm: Realm, event: Event) {
        if (event.roomId == null) return
        val createRoomContent = event.getClearContent().toModel<RoomTombstoneContent>()
        if (createRoomContent?.replacementRoomId == null) return

        val predecessorRoomSummary = RoomSummaryEntity.where(realm, event.roomId).findFirst()
                ?: RoomSummaryEntity(event.roomId)
        if (predecessorRoomSummary.versioningState == VersioningState.NONE) {
            predecessorRoomSummary.versioningState = VersioningState.UPGRADED_ROOM_NOT_JOINED
        }
        realm.insertOrUpdate(predecessorRoomSummary)
    }

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        return eventType == EventType.STATE_ROOM_TOMBSTONE
    }
}
