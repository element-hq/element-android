/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.store.db

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.migration.MigrateCryptoTo024
import org.matrix.android.sdk.internal.util.database.MatrixRealmMigration
import javax.inject.Inject

/**
 * Schema version history:
 *  0, 1, 2: legacy Riot-Android;
 *  3: migrate to RiotX schema;
 *  4, 5, 6, 7, 8, 9: migrations from RiotX (which was previously 1, 2, 3, 4, 5, 6).
 *  24: Delete nearly all the crypto DB
 */
internal class RealmCryptoStoreMigration @Inject constructor() : MatrixRealmMigration(
        dbName = "Crypto",
        schemaVersion = 24L,
) {
    /**
     * Forces all RealmCryptoStoreMigration instances to be equal.
     * Avoids Realm throwing when multiple instances of the migration are set.
     */
    override fun equals(other: Any?) = other is RealmCryptoStoreMigration
    override fun hashCode() = 5000

    override fun doMigrate(realm: DynamicRealm, oldVersion: Long) {
        if (oldVersion < 24) MigrateCryptoTo024(realm).perform()
    }
}
