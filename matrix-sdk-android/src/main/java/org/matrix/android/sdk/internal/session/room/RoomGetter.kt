/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room

import io.realm.Realm
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.RealmSessionProvider
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

internal interface RoomGetter {
    fun getRoom(roomId: String): Room?

    fun getDirectRoomWith(otherUserId: String): String?
}

@SessionScope
internal class DefaultRoomGetter @Inject constructor(
        private val realmSessionProvider: RealmSessionProvider,
        private val roomFactory: RoomFactory
) : RoomGetter {

    override fun getRoom(roomId: String): Room? {
        return realmSessionProvider.withRealm { realm ->
            createRoom(realm, roomId)
        }
    }

    override fun getDirectRoomWith(otherUserId: String): String? {
        return realmSessionProvider.withRealm { realm ->
            RoomSummaryEntity.where(realm)
                    .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
                    .equalTo(RoomSummaryEntityFields.MEMBERSHIP_STR, Membership.JOIN.name)
                    .findAll()
                    .firstOrNull { dm -> dm.otherMemberIds.size == 1 && dm.otherMemberIds.first(null) == otherUserId }
                    ?.roomId
        }
    }

    private fun createRoom(realm: Realm, roomId: String): Room? {
        return RoomEntity.where(realm, roomId).findFirst()
                ?.let { roomFactory.create(roomId) }
    }
}
