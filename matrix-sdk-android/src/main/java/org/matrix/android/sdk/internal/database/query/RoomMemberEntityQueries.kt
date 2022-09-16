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
import org.matrix.android.sdk.internal.database.andIf
import org.matrix.android.sdk.internal.database.model.RoomMemberSummaryEntity
import org.matrix.android.sdk.internal.database.model.presence.UserPresenceEntity

internal fun RoomMemberSummaryEntity.Companion.where(realm: TypedRealm, roomId: String, userId: String? = null): RealmQuery<RoomMemberSummaryEntity> {
    return realm.query(RoomMemberSummaryEntity::class)
            .query("roomId == $0", roomId)
            .andIf(userId != null) {
                query("userId == $0", userId!!)
            }
}

internal fun RoomMemberSummaryEntity.Companion.updateUserPresence(realm: MutableRealm, userId: String, userPresenceEntity: UserPresenceEntity) {
    realm.query(RoomMemberSummaryEntity::class)
            .query("userId == $0", userId)
            .query("userPresenceEntity == nil")
            .find()
            .map {
                it.userPresenceEntity = userPresenceEntity
            }
}
