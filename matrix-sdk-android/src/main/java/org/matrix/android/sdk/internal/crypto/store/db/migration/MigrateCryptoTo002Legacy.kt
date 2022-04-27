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

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.deserializeFromRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.KeysBackupDataEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateCryptoTo002Legacy(realm: DynamicRealm) : RealmMigrator(realm, 2) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Update IncomingRoomKeyRequestEntity format: requestBodyString field is exploded into several fields")
        realm.schema.get("IncomingRoomKeyRequestEntity")
                ?.addFieldIfNotExists("requestBodyAlgorithm", String::class.java)
                ?.addFieldIfNotExists("requestBodyRoomId", String::class.java)
                ?.addFieldIfNotExists("requestBodySenderKey", String::class.java)
                ?.addFieldIfNotExists("requestBodySessionId", String::class.java)
                ?.transform { dynamicObject ->
                    try {
                        val requestBodyString = dynamicObject.getString("requestBodyString")
                        // It was a map before
                        val map: Map<String, String>? = deserializeFromRealm(requestBodyString)

                        map?.let {
                            dynamicObject.setString("requestBodyAlgorithm", it["algorithm"])
                            dynamicObject.setString("requestBodyRoomId", it["room_id"])
                            dynamicObject.setString("requestBodySenderKey", it["sender_key"])
                            dynamicObject.setString("requestBodySessionId", it["session_id"])
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }
                ?.removeFieldIfExists("requestBodyString")

        Timber.d("Update IncomingRoomKeyRequestEntity format: requestBodyString field is exploded into several fields")
        realm.schema.get("OutgoingRoomKeyRequestEntity")
                ?.addFieldIfNotExists("requestBodyAlgorithm", String::class.java)
                ?.addFieldIfNotExists("requestBodyRoomId", String::class.java)
                ?.addFieldIfNotExists("requestBodySenderKey", String::class.java)
                ?.addFieldIfNotExists("requestBodySessionId", String::class.java)
                ?.transform { dynamicObject ->
                    try {
                        val requestBodyString = dynamicObject.getString("requestBodyString")
                        // It was a map before
                        val map: Map<String, String>? = deserializeFromRealm(requestBodyString)

                        map?.let {
                            dynamicObject.setString("requestBodyAlgorithm", it["algorithm"])
                            dynamicObject.setString("requestBodyRoomId", it["room_id"])
                            dynamicObject.setString("requestBodySenderKey", it["sender_key"])
                            dynamicObject.setString("requestBodySessionId", it["session_id"])
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error")
                    }
                }
                ?.removeFieldIfExists("requestBodyString")

        Timber.d("Create KeysBackupDataEntity")
        if (!realm.schema.contains("KeysBackupDataEntity")) {
            realm.schema.create("KeysBackupDataEntity")
                    .addField(KeysBackupDataEntityFields.PRIMARY_KEY, Integer::class.java)
                    .addPrimaryKey(KeysBackupDataEntityFields.PRIMARY_KEY)
                    .setRequired(KeysBackupDataEntityFields.PRIMARY_KEY, true)
                    .addField(KeysBackupDataEntityFields.BACKUP_LAST_SERVER_HASH, String::class.java)
                    .addField(KeysBackupDataEntityFields.BACKUP_LAST_SERVER_NUMBER_OF_KEYS, Integer::class.java)
        }
    }
}
