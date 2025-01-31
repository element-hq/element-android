/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.raw

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.raw.migration.MigrateGlobalTo001
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import javax.inject.Inject

internal class GlobalRealmMigration @Inject constructor() : MatrixRealmMigration(
        dbName = "Global",
        schemaVersion = 1L,
) {
    /**
     * Forces all GlobalRealmMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is GlobalRealmMigration
    override fun hashCode() = 2000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
        if (oldVersion < 1) MigrateGlobalTo001(realm).perform()
    }
}
