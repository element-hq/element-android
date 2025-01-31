/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.SpaceChildSummaryEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo013(realm: DynamicRealm) : RealmMigrator(realm, 13) {

    override fun doMigrate(realm: DynamicRealm) {
        // Fix issue with the nightly build. Eventually play again the migration which has been included in migrateTo12()
        realm.schema.get("SpaceChildSummaryEntity")
                ?.takeIf { !it.hasField(SpaceChildSummaryEntityFields.SUGGESTED) }
                ?.addField(SpaceChildSummaryEntityFields.SUGGESTED, Boolean::class.java)
                ?.setNullable(SpaceChildSummaryEntityFields.SUGGESTED, true)
    }
}
