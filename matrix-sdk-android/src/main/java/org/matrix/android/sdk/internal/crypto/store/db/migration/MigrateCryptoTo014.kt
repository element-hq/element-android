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
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import org.matrix.android.sdk.internal.database.safeEnumerate
import timber.log.Timber

// Version 14L Update the way we remember key sharing
internal class MigrateCryptoTo014(context: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(context, 14) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        Timber.d("Update SharedSessionEntity")
        migrationContext.safeEnumerate("SharedSessionEntity") { oldObject, newObject ->
            if (newObject == null) return@safeEnumerate
            val sharedUserId = oldObject.getNullableValue("userId", String::class)
            val sharedDeviceId = oldObject.getNullableValue("deviceId", String::class)
            val knownDevice = migrationContext.newRealm.query("DeviceInfoEntity")
                    .query("userId == $0", sharedUserId)
                    .query("deviceId == $0", sharedDeviceId)
                    .first()
                    .find()

            val deviceIdentityKey = knownDevice?.getNullableValue("identityKey", String::class)
            newObject.set("deviceIdentityKey", deviceIdentityKey)
        }
    }
}
