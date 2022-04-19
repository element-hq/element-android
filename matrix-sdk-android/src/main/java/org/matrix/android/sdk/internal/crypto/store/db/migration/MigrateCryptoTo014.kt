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
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

// Version 14L Update the way we remember key sharing
internal class MigrateCryptoTo014(realm: DynamicRealm) : RealmMigrator(realm, 14) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("SharedSessionEntity")
                ?.addField(SharedSessionEntityFields.DEVICE_IDENTITY_KEY, String::class.java)
                ?.addIndex(SharedSessionEntityFields.DEVICE_IDENTITY_KEY)
                ?.transform {
                    val sharedUserId = it.getString(SharedSessionEntityFields.USER_ID)
                    val sharedDeviceId = it.getString(SharedSessionEntityFields.DEVICE_ID)
                    val knownDevice = realm.where("DeviceInfoEntity")
                            .equalTo(DeviceInfoEntityFields.USER_ID, sharedUserId)
                            .equalTo(DeviceInfoEntityFields.DEVICE_ID, sharedDeviceId)
                            .findFirst()
                    it.setString(SharedSessionEntityFields.DEVICE_IDENTITY_KEY, knownDevice?.getString(DeviceInfoEntityFields.IDENTITY_KEY))
                }
    }
}
