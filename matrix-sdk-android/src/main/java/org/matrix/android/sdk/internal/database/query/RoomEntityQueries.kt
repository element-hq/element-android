/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.query

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.RoomEntityFields

internal fun RoomEntity.Companion.where(realm: Realm, roomId: String): RealmQuery<RoomEntity> {
    return realm.where<RoomEntity>()
            .equalTo(RoomEntityFields.ROOM_ID, roomId)
}

internal fun RoomEntity.Companion.getOrCreate(realm: Realm, roomId: String): RoomEntity {
    return where(realm, roomId).findFirst() ?: realm.createObject(RoomEntity::class.java, roomId)
}

internal fun RoomEntity.Companion.where(realm: Realm, membership: Membership? = null): RealmQuery<RoomEntity> {
    val query = realm.where<RoomEntity>()
    if (membership != null) {
        query.equalTo(RoomEntityFields.MEMBERSHIP_STR, membership.name)
    }
    return query
}

internal fun RoomEntity.fastContains(eventId: String): Boolean {
    return EventEntity.where(realm, eventId = eventId).findFirst() != null
}
