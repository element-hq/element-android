/*
 * Copyright (c) 2022 New Vector Ltd
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
import kotlin.system.measureTimeMillis

abstract class MatrixAutomaticSchemaMigration(
        private val dbName: String,
        val schemaVersion: Long
) : AutomaticSchemaMigration {

    final override fun migrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        val oldVersion = migrationContext.oldVersion
        val newVersion = migrationContext.newVersion
        Timber.d("Migrating Realm $dbName from $oldVersion to $newVersion")
        val duration = measureTimeMillis {
            doMigrate(oldVersion, migrationContext)
        }
        Timber.d("Migrating Realm $dbName from $oldVersion to $newVersion took $duration ms.")
    }

    abstract fun doMigrate(oldVersion: Long, migrationContext: AutomaticSchemaMigration.MigrationContext)
}
