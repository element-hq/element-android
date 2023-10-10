/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.RustEncryptionConfiguration
import org.matrix.android.sdk.internal.session.MigrateEAtoEROperation
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import java.io.File

/**
 * This migration creates the rust database and migrates from legacy crypto.
 */
internal class MigrateCryptoTo022(
        realm: DynamicRealm,
        private val rustDirectory: File,
        private val rustEncryptionConfiguration: RustEncryptionConfiguration,
        private val migrateMegolmGroupSessions: Boolean = false
) : RealmMigrator(
        realm,
        22
) {
    override fun doMigrate(realm: DynamicRealm) {
        // Migrate to rust!
        val migrateOperation = MigrateEAtoEROperation(migrateMegolmGroupSessions)
        migrateOperation.dynamicExecute(realm, rustDirectory, rustEncryptionConfiguration.getDatabasePassphrase())

        // wa can't delete all for now, but we can do some cleaning
        realm.schema.get("OlmSessionEntity")?.transform {
            it.deleteFromRealm()
        }

        // a future migration will clean the rest
    }
}
