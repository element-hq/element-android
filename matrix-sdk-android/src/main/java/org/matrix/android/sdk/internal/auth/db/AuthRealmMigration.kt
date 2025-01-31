/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo001
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo002
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo003
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo004
import org.matrix.android.sdk.internal.auth.db.migration.MigrateAuthTo005
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import javax.inject.Inject

internal class AuthRealmMigration @Inject constructor() : MatrixRealmMigration(
        dbName = "Auth",
        schemaVersion = 5L,
) {
    /**
     * Forces all AuthRealmMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is AuthRealmMigration
    override fun hashCode() = 4000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
        if (oldVersion < 1) MigrateAuthTo001(realm).perform()
        if (oldVersion < 2) MigrateAuthTo002(realm).perform()
        if (oldVersion < 3) MigrateAuthTo003(realm).perform()
        if (oldVersion < 4) MigrateAuthTo004(realm).perform()
        if (oldVersion < 5) MigrateAuthTo005(realm).perform()
    }
}
