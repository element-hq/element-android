/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import io.realm.RealmList
import org.matrix.android.sdk.internal.database.model.RoomSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo035(realm: DynamicRealm) : RealmMigrator(realm, 35) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("RoomSummaryEntity")
                ?.addRealmListField(RoomSummaryEntityFields.DIRECT_PARENT_NAMES.`$`, String::class.java)
                ?.transform { it.setList(RoomSummaryEntityFields.DIRECT_PARENT_NAMES.`$`, RealmList("")) }
    }
}
