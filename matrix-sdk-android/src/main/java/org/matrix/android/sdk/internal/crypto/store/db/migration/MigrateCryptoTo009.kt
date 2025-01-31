/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.DeviceInfoEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.UserEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

// Fixes duplicate devices in UserEntity#devices
internal class MigrateCryptoTo009(realm: DynamicRealm) : RealmMigrator(realm, 9) {

    override fun doMigrate(realm: DynamicRealm) {
        val userEntities = realm.where("UserEntity").findAll()
        userEntities.forEach {
            try {
                val deviceList = it.getList(UserEntityFields.DEVICES.`$`)
                        ?: return@forEach
                val distinct = deviceList.distinctBy { it.getString(DeviceInfoEntityFields.DEVICE_ID) }
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
