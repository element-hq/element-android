/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.create

import io.realm.Realm
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.api.session.room.model.create.RoomCreateContent
import org.matrix.android.sdk.internal.database.model.EventInsertType
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.EventInsertLiveProcessor
import javax.inject.Inject

internal class RoomCreateEventProcessor @Inject constructor() : EventInsertLiveProcessor {

    override suspend fun process(realm: Realm, event: Event) {
        val createRoomContent = event.getClearContent().toModel<RoomCreateContent>()
        val predecessorRoomId = createRoomContent?.predecessor?.roomId ?: return

        val predecessorRoomSummary = RoomSummaryEntity.where(realm, predecessorRoomId).findFirst()
                ?: RoomSummaryEntity(predecessorRoomId)
        predecessorRoomSummary.versioningState = VersioningState.UPGRADED_ROOM_JOINED
        predecessorRoomSummary.isHiddenFromUser = true
        realm.insertOrUpdate(predecessorRoomSummary)
    }

    override fun shouldProcess(eventId: String, eventType: String, insertType: EventInsertType): Boolean {
        return eventType == EventType.STATE_ROOM_CREATE
    }
}
