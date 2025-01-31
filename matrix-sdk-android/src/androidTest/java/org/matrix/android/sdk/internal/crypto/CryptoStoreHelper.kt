/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto

import io.realm.RealmConfiguration
import org.matrix.android.sdk.internal.crypto.store.IMXCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStore
import org.matrix.android.sdk.internal.crypto.store.db.RealmCryptoStoreModule
import org.matrix.android.sdk.internal.crypto.store.db.mapper.CrossSigningKeysMapper
import org.matrix.android.sdk.internal.crypto.store.db.mapper.MyDeviceLastSeenInfoEntityMapper
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.time.DefaultClock
import kotlin.random.Random

internal class CryptoStoreHelper {

    fun createStore(): IMXCryptoStore {
        return RealmCryptoStore(
                realmConfiguration = RealmConfiguration.Builder()
                        .name("test.realm")
                        .modules(RealmCryptoStoreModule())
                        .build(),
                crossSigningKeysMapper = CrossSigningKeysMapper(MoshiProvider.providesMoshi()),
                userId = "userId_" + Random.nextInt(),
                deviceId = "deviceId_sample",
                clock = DefaultClock(),
                myDeviceLastSeenInfoEntityMapper = MyDeviceLastSeenInfoEntityMapper()
        )
    }
}
