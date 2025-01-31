/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
