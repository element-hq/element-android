/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.query

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntity
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.createPrimaryKey

/**
 * Get or create a device info.
 */
internal fun DeviceInfoEntity.Companion.getOrCreate(realm: Realm, userId: String, deviceId: String): DeviceInfoEntity {
    val key = DeviceInfoEntity.createPrimaryKey(userId, deviceId)

    return realm.where<DeviceInfoEntity>()
            .equalTo(DeviceInfoEntityFields.PRIMARY_KEY, key)
            .findFirst()
            ?: realm.createObject<DeviceInfoEntity>(key)
                    .apply {
                        this.deviceId = deviceId
                    }
}
