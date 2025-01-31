/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.session.room.model.LocalRoomCreationState
import org.matrix.android.sdk.internal.database.model.LocalRoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo037(realm: DynamicRealm) : RealmMigrator(realm, 37) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("LocalRoomSummaryEntity")
                ?.addField(LocalRoomSummaryEntityFields.REPLACEMENT_ROOM_ID, String::class.java)
                ?.addField(LocalRoomSummaryEntityFields.STATE_STR, String::class.java)
                ?.transform { obj ->
                    obj.set(LocalRoomSummaryEntityFields.STATE_STR, LocalRoomCreationState.NOT_CREATED.name)
                }
    }
}
