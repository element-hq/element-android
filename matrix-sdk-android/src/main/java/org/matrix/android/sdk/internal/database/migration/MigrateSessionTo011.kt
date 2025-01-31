/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateSessionTo011(realm: DynamicRealm) : RealmMigrator(realm, 11) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("EventEntity")
                ?.addField(EventEntityFields.SEND_STATE_DETAILS, String::class.java)
    }
}
