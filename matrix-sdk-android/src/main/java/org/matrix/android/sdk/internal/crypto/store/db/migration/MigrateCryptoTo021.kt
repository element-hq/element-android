/*
 * Copyright (c) 2023 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.api.crypto.MEGOLM_DEFAULT_ROTATION_MSGS
import org.matrix.android.sdk.api.crypto.MEGOLM_DEFAULT_ROTATION_PERIOD_MS
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

/**
 * This migration stores the rotation parameters for megolm oubound sessions.
 */
internal class MigrateCryptoTo021(realm: DynamicRealm) : RealmMigrator(realm, 21) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("CryptoRoomEntity")
                ?.addField(CryptoRoomEntityFields.ROTATION_PERIOD_MS, Long::class.java)
                ?.setNullable(CryptoRoomEntityFields.ROTATION_PERIOD_MS, true)
                ?.addField(CryptoRoomEntityFields.ROTATION_PERIOD_MSGS, Long::class.java)
                ?.setNullable(CryptoRoomEntityFields.ROTATION_PERIOD_MSGS, true)
                ?.transform {
                    // As a migration we set the default (will be on par with existing code)
                    // A clear cache will have the correct values.
                    it.setLong(CryptoRoomEntityFields.ROTATION_PERIOD_MS, MEGOLM_DEFAULT_ROTATION_PERIOD_MS)
                    it.setLong(CryptoRoomEntityFields.ROTATION_PERIOD_MSGS, MEGOLM_DEFAULT_ROTATION_MSGS)
                }
    }
}
