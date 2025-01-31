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
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal fun RoomSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    val query = realm.where<RoomSummaryEntity>()
    if (roomId != null) {
        query.equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
    }
    return query
}

internal fun RoomSummaryEntity.Companion.findByAlias(realm: Realm, roomAlias: String): RoomSummaryEntity? {
    val roomSummary = realm.where<RoomSummaryEntity>()
            .equalTo(RoomSummaryEntityFields.CANONICAL_ALIAS, roomAlias)
            .findFirst()
    if (roomSummary != null) {
        return roomSummary
    }
    return realm.where<RoomSummaryEntity>()
            .contains(RoomSummaryEntityFields.FLAT_ALIASES, "|$roomAlias")
            .findFirst()
}

internal fun RoomSummaryEntity.Companion.getOrCreate(realm: Realm, roomId: String): RoomSummaryEntity {
    return where(realm, roomId).findFirst() ?: realm.createObject(roomId)
}

internal fun RoomSummaryEntity.Companion.getOrNull(realm: Realm, roomId: String): RoomSummaryEntity? {
    return where(realm, roomId).findFirst()
}

internal fun RoomSummaryEntity.Companion.getDirectRooms(
        realm: Realm,
        excludeRoomIds: Set<String>? = null
): RealmResults<RoomSummaryEntity> {
    return RoomSummaryEntity.where(realm)
            .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
            .apply {
                if (!excludeRoomIds.isNullOrEmpty()) {
                    not().`in`(RoomSummaryEntityFields.ROOM_ID, excludeRoomIds.toTypedArray())
                }
            }
            .findAll()
}

internal fun RoomSummaryEntity.Companion.isDirect(realm: Realm, roomId: String): Boolean {
    return RoomSummaryEntity.where(realm)
            .equalTo(RoomSummaryEntityFields.ROOM_ID, roomId)
            .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
            .findAll()
            .isNotEmpty()
}

internal fun RoomSummaryEntity.Companion.updateDirectUserPresence(realm: Realm, directUserId: String, userPresenceEntity: UserPresenceEntity) {
    RoomSummaryEntity.where(realm)
            .equalTo(RoomSummaryEntityFields.IS_DIRECT, true)
            .equalTo(RoomSummaryEntityFields.DIRECT_USER_ID, directUserId)
            .findFirst()
            ?.directUserPresence = userPresenceEntity
}
