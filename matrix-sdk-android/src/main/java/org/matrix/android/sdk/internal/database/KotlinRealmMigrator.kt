/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.database

import io.realm.kotlin.migration.AutomaticSchemaMigration
import timber.log.Timber

internal abstract class KotlinRealmMigrator(
        private val migrationContext: AutomaticSchemaMigration.MigrationContext,
        private val targetSchemaVersion: Int
) {
    fun perform() {
        Timber.d("Migrate ${migrationContext.oldRealm.configuration.name} to $targetSchemaVersion")
        doMigrate(migrationContext)
    }

    protected abstract fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext)
}

val AutomaticSchemaMigration.MigrationContext.oldVersion: Long
    get() {
        return oldRealm.schemaVersion()
    }

val AutomaticSchemaMigration.MigrationContext.newVersion: Long
    get() {
        return newRealm.schemaVersion()
    }

