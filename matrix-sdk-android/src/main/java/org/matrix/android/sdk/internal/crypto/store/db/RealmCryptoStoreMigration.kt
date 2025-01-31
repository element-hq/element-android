/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo001Legacy
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo002Legacy
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo003RiotX
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo004
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo005
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo006
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo007
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo008
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo009
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo010
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo011
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo012
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo013
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo014
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo015
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo016
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo017
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo018
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

/**
 * Schema version history:
 *  0, 1, 2: legacy Riot-Android;
 *  3: migrate to RiotX schema;
 *  4, 5, 6, 7, 8, 9: migrations from RiotX (which was previously 1, 2, 3, 4, 5, 6).
 */
internal class RealmCryptoStoreMigration @Inject constructor(
        private val clock: Clock,
) : MatrixRealmMigration(
        dbName = "Crypto",
        schemaVersion = 18L,
) {
    /**
     * Forces all RealmCryptoStoreMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is RealmCryptoStoreMigration
    override fun hashCode() = 5000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
        if (oldVersion < 1) MigrateCryptoTo001Legacy(realm).perform()
        if (oldVersion < 2) MigrateCryptoTo002Legacy(realm).perform()
        if (oldVersion < 3) MigrateCryptoTo003RiotX(realm).perform()
        if (oldVersion < 4) MigrateCryptoTo004(realm).perform()
        if (oldVersion < 5) MigrateCryptoTo005(realm).perform()
        if (oldVersion < 6) MigrateCryptoTo006(realm).perform()
        if (oldVersion < 7) MigrateCryptoTo007(realm).perform()
        if (oldVersion < 8) MigrateCryptoTo008(realm, clock).perform()
        if (oldVersion < 9) MigrateCryptoTo009(realm).perform()
        if (oldVersion < 10) MigrateCryptoTo010(realm).perform()
        if (oldVersion < 11) MigrateCryptoTo011(realm).perform()
        if (oldVersion < 12) MigrateCryptoTo012(realm).perform()
        if (oldVersion < 13) MigrateCryptoTo013(realm).perform()
        if (oldVersion < 14) MigrateCryptoTo014(realm).perform()
        if (oldVersion < 15) MigrateCryptoTo015(realm).perform()
        if (oldVersion < 16) MigrateCryptoTo016(realm).perform()
        if (oldVersion < 17) MigrateCryptoTo017(realm).perform()
        if (oldVersion < 18) MigrateCryptoTo018(realm).perform()
    }
}
