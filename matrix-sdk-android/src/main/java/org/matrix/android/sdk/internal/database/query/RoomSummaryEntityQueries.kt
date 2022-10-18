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

import io.realm.kotlin.MutableRealm
import io.realm.kotlin.TypedRealm
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmResults
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity
import org.matrix.android.sdk.internal.database.queryNotIn
import org.matrix.android.sdk.internal.query.process

internal fun RoomSummaryEntity.Companion.where(realm: TypedRealm, roomId: String? = null): RealmQuery<RoomSummaryEntity> {
    return realm.query(RoomSummaryEntity::class)
            .andIf(roomId != null) {
                query("roomId == $0", roomId!!)
            }
}

internal fun RoomSummaryEntity.Companion.where(realm: TypedRealm, roomId: String, memberships: List<Membership>): RealmQuery<RoomSummaryEntity> {
    return realm.query(RoomSummaryEntity::class)
            .query("roomId == $0", roomId)
            .process("membershipStr", memberships)
}

internal fun RoomSummaryEntity.Companion.findByAlias(realm: TypedRealm, roomAlias: String): RoomSummaryEntity? {
    val roomSummary = realm.query(RoomSummaryEntity::class)
            .query("canonicalAlias == $0", roomAlias)
            .first()
            .find()
    if (roomSummary != null) {
        return roomSummary
    }
    return realm.query(RoomSummaryEntity::class)
            .query("flatAliases CONTAINS $0", "|$roomAlias")
            .first()
            .find()
}

private fun RoomSummaryEntity.Companion.create(realm: MutableRealm, roomId: String): RoomSummaryEntity {
    val roomSummaryEntity = RoomSummaryEntity().apply {
        this.roomId = roomId
    }
    return realm.copyToRealm(roomSummaryEntity)
}

internal fun RoomSummaryEntity.Companion.getOrCreate(realm: MutableRealm, roomId: String): RoomSummaryEntity {
    return getOrNull(realm, roomId) ?: create(realm, roomId)
}

internal fun RoomSummaryEntity.Companion.getOrNull(realm: TypedRealm, roomId: String): RoomSummaryEntity? {
    return where(realm, roomId).first().find()
}

internal fun RoomSummaryEntity.Companion.getDirectRooms(
        realm: TypedRealm,
        excludeRoomIds: Set<String>? = null
): RealmResults<RoomSummaryEntity> {
    return RoomSummaryEntity.where(realm)
            .query("isDirect == true")
            .andIf(!excludeRoomIds.isNullOrEmpty()) {
                queryNotIn("roomId", excludeRoomIds.orEmpty().toList())
            }
            .find()
}

internal fun RoomSummaryEntity.Companion.isDirect(realm: TypedRealm, roomId: String): Boolean {
    return RoomSummaryEntity.where(realm)
            .query("roomId == $0", roomId)
            .query("isDirect == true")
            .find()
            .isNotEmpty()
}

internal fun RoomSummaryEntity.Companion.updateDirectUserPresence(realm: MutableRealm, directUserId: String, userPresenceEntity: UserPresenceEntity) {
    RoomSummaryEntity.where(realm)
            .query("isDirect == true", true)
            .query("directUserId == $0", directUserId)
            .first()
            .find()
            ?.directUserPresence = userPresenceEntity
}
