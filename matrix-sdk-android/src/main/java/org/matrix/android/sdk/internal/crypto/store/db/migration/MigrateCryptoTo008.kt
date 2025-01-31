/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
