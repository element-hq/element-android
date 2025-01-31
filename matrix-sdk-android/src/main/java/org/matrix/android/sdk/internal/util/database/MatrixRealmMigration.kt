/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util.database

import io.realm.DynamicRealm
import io.realm.RealmMigration
import timber.log.Timber
import kotlin.system.measureTimeMillis

internal abstract class MatrixRealmMigration(
        private val dbName: String,
        val schemaVersion: Long,
) : RealmMigration {
    final override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.d("Migrating Realm $dbName from $oldVersion to $newVersion")
        val duration = measureTimeMillis {
            doMigrate(realm, oldVersion)
        }
        Timber.d("Migrating Realm $dbName from $oldVersion to $newVersion took $duration ms.")
    }

    abstract fun doMigrate(realm: DynamicRealm, oldVersion: Long)
}
