/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
