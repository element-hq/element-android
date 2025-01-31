/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo036(realm: DynamicRealm) : RealmMigrator(realm, 36) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("LocalRoomSummaryEntity")
                .addField(LocalRoomSummaryEntityFields.ROOM_ID, String::class.java)
                .addPrimaryKey(LocalRoomSummaryEntityFields.ROOM_ID)
                .setRequired(LocalRoomSummaryEntityFields.ROOM_ID, true)
                .addField(LocalRoomSummaryEntityFields.CREATE_ROOM_PARAMS_STR, String::class.java)
                .addRealmObjectField(LocalRoomSummaryEntityFields.ROOM_SUMMARY_ENTITY.`$`, realm.schema.get("RoomSummaryEntity")!!)
    }
}
