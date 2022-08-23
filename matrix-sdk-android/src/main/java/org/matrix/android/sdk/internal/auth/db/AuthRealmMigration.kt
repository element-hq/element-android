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
