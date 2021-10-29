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
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntityFields
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal fun RoomMemberSummaryEntity.Companion.where(realm: Realm, roomId: String, userId: String? = null): RealmQuery<RoomMemberSummaryEntity> {
    val query = realm
            .where<RoomMemberSummaryEntity>()
            .equalTo(RoomMemberSummaryEntityFields.ROOM_ID, roomId)

    if (userId != null) {
        query.equalTo(RoomMemberSummaryEntityFields.USER_ID, userId)
    }
    return query
}

internal fun RoomMemberSummaryEntity.Companion.updateUserPresence(realm: Realm, userId: String, userPresenceEntity: UserPresenceEntity) {
    realm.where<RoomMemberSummaryEntity>()
            .equalTo(RoomMemberSummaryEntityFields.USER_ID, userId)
            .isNull(RoomMemberSummaryEntityFields.USER_PRESENCE_ENTITY.`$`)
            .findAll()
            .map {
                it.userPresenceEntity = userPresenceEntity
            }
}
