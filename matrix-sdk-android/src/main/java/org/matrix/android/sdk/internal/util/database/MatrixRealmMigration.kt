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
