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

internal fun RoomSummaryEntity.Companion.getDirectRooms(realm: Realm,
                                                        excludeRoomIds: Set<String>? = null): RealmResults<RoomSummaryEntity> {
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
