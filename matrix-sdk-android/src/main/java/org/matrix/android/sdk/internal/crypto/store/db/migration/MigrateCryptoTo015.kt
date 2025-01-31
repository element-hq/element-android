/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

// Version 15L adds wasEncryptedOnce field to CryptoRoomEntity
internal class MigrateCryptoTo015(realm: DynamicRealm) : RealmMigrator(realm, 15) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("CryptoRoomEntity")
                ?.addField(CryptoRoomEntityFields.WAS_ENCRYPTED_ONCE, Boolean::class.java)
                ?.setNullable(CryptoRoomEntityFields.WAS_ENCRYPTED_ONCE, true)
                ?.transform {
                    val currentAlgorithm = it.getString(CryptoRoomEntityFields.ALGORITHM)
                    it.set(CryptoRoomEntityFields.WAS_ENCRYPTED_ONCE, currentAlgorithm == MXCRYPTO_ALGORITHM_MEGOLM)
                }
    }
}
