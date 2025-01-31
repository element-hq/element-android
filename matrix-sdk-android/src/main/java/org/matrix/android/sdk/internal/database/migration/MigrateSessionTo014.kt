/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import org.matrix.android.sdk.api.session.room.model.VersioningState
import org.matrix.android.sdk.internal.database.model.RoomAccountDataEntityFields
import org.matrix.android.sdk.internal.database.model.RoomEntityFields
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo014(realm: DynamicRealm) : RealmMigrator(realm, 14) {

    override fun doMigrate(realm: DynamicRealm) {
        val roomAccountDataSchema = realm.schema.create("RoomAccountDataEntity")
                .addField(RoomAccountDataEntityFields.CONTENT_STR, String::class.java)
                .addField(RoomAccountDataEntityFields.TYPE, String::class.java, FieldAttribute.INDEXED)

        realm.schema.get("RoomEntity")
                ?.addRealmListField(RoomEntityFields.ACCOUNT_DATA.`$`, roomAccountDataSchema)

        realm.schema.get("RoomSummaryEntity")
                ?.addField(RoomSummaryEntityFields.IS_HIDDEN_FROM_USER, Boolean::class.java, FieldAttribute.INDEXED)
                ?.transform {
                    val isHiddenFromUser = it.getString(RoomSummaryEntityFields.VERSIONING_STATE_STR) == VersioningState.UPGRADED_ROOM_JOINED.name
                    it.setBoolean(RoomSummaryEntityFields.IS_HIDDEN_FROM_USER, isHiddenFromUser)
                }

        roomAccountDataSchema.isEmbedded = true
    }
}
