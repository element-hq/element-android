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
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.MyDeviceLastSeenInfoEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import org.matrix.android.sdk.internal.util.time.Clock

internal class MigrateCryptoTo008(
        realm: DynamicRealm,
        private val clock: Clock,
) : RealmMigrator(realm, 8) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("MyDeviceLastSeenInfoEntity")
                .addField(MyDeviceLastSeenInfoEntityFields.DEVICE_ID, String::class.java)
                .addPrimaryKey(MyDeviceLastSeenInfoEntityFields.DEVICE_ID)
                .addField(MyDeviceLastSeenInfoEntityFields.DISPLAY_NAME, String::class.java)
                .addField(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_IP, String::class.java)
                .addField(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_TS, Long::class.java)
                .setNullable(MyDeviceLastSeenInfoEntityFields.LAST_SEEN_TS, true)

        val now = clock.epochMillis()
        realm.schema.get("DeviceInfoEntity")
                ?.addField(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, Long::class.java)
                ?.setNullable(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, true)
                ?.transform { deviceInfoEntity ->
                    tryOrNull {
                        deviceInfoEntity.setLong(DeviceInfoEntityFields.FIRST_TIME_SEEN_LOCAL_TS, now)
                    }
                }
    }
}
