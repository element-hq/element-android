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

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import timber.log.Timber

internal class MigrateCryptoTo009(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 9) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        Timber.d("Fixes duplicate devices in UserEntity#devices")
        val userEntities = migrationContext.newRealm.query("UserEntity").find()
        userEntities.forEach {
            try {
                val deviceList = it.getValueList("devices", DeviceInfoEntity::class)
                val distinct = deviceList.distinctBy { it.deviceId }
                if (distinct.size != deviceList.size) {
                    deviceList.clear()
                    deviceList.addAll(distinct)
                }
            } catch (failure: Throwable) {
                Timber.w(failure, "Crypto Data base migration error for migrateTo9")
            }
        }
    }
}
