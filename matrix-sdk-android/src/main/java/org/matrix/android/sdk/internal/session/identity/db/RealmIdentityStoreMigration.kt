/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.session.identity.db.migration.MigrateIdentityTo001
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import javax.inject.Inject

internal class RealmIdentityStoreMigration @Inject constructor() : MatrixRealmMigration(
        dbName = "Identity",
        schemaVersion = 1L,
) {
    /**
     * Forces all RealmIdentityStoreMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is RealmIdentityStoreMigration
    override fun hashCode() = 3000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
        if (oldVersion < 1) MigrateIdentityTo001(realm).perform()
    }
}
