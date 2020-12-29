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
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntity
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntity
import org.matrix.android.sdk.internal.database.model.SpaceSummaryEntityFields

internal fun SpaceSummaryEntity.Companion.where(realm: Realm, roomId: String? = null): RealmQuery<SpaceSummaryEntity> {
    val query = realm.where<SpaceSummaryEntity>()
    query.isNotNull(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.`$`)
    if (roomId != null) {
        query.equalTo(SpaceSummaryEntityFields.SPACE_ID, roomId)
    }
    query.sort(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.DISPLAY_NAME)
    return query
}

internal fun SpaceSummaryEntity.Companion.findByAlias(realm: Realm, roomAlias: String): SpaceSummaryEntity? {
    val spaceSummary = realm.where<SpaceSummaryEntity>()
            .isNotNull(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.`$`)
            .equalTo(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.CANONICAL_ALIAS, roomAlias)
            .findFirst()
    if (spaceSummary != null) {
        return spaceSummary
    }
    return realm.where<SpaceSummaryEntity>()
            .isNotNull(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.`$`)
            .contains(SpaceSummaryEntityFields.ROOM_SUMMARY_ENTITY.FLAT_ALIASES, "|$roomAlias")
            .findFirst()
}

internal fun SpaceSummaryEntity.Companion.getOrCreate(realm: Realm, roomId: String): SpaceSummaryEntity {
    return where(realm, roomId).findFirst() ?: realm.createObject<SpaceSummaryEntity>(roomId).also {
        it.roomSummaryEntity = RoomSummaryEntity.getOrCreate(realm, roomId)
    }
}
