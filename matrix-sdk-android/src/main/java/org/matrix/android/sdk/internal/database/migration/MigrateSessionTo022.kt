/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.internal.database.model.CurrentStateEventEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateSessionTo022(realm: DynamicRealm) : RealmMigrator(realm, 22) {

    override fun doMigrate(realm: DynamicRealm) {
        val listJoinedRoomIds = realm.where("RoomEntity")
                .equalTo(RoomEntityFields.MEMBERSHIP_STR, Membership.JOIN.name).findAll()
                .map { it.getString(RoomEntityFields.ROOM_ID) }

        val hasMissingStateEvent = realm.where("CurrentStateEventEntity")
                .`in`(CurrentStateEventEntityFields.ROOM_ID, listJoinedRoomIds.toTypedArray())
                .isNull(CurrentStateEventEntityFields.ROOT.`$`).findFirst() != null

        if (hasMissingStateEvent) {
            Timber.v("Has some missing state event, clear session cache")
            realm.deleteAll()
        }
    }
}
