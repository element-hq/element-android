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
