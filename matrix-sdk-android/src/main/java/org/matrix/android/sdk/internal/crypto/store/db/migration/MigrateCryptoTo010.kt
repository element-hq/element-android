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
import org.matrix.android.sdk.internal.crypto.store.db.model.SharedSessionEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.WithHeldSessionEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

// Version 10L added WithHeld Keys Info (MSC2399)
internal class MigrateCryptoTo010(realm: DynamicRealm) : RealmMigrator(realm, 10) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.create("WithHeldSessionEntity")
                .addField(WithHeldSessionEntityFields.ROOM_ID, String::class.java)
                .addField(WithHeldSessionEntityFields.ALGORITHM, String::class.java)
                .addField(WithHeldSessionEntityFields.SESSION_ID, String::class.java)
                .addIndex(WithHeldSessionEntityFields.SESSION_ID)
                .addField(WithHeldSessionEntityFields.SENDER_KEY, String::class.java)
                .addIndex(WithHeldSessionEntityFields.SENDER_KEY)
                .addField(WithHeldSessionEntityFields.CODE_STRING, String::class.java)
                .addField(WithHeldSessionEntityFields.REASON, String::class.java)

        realm.schema.create("SharedSessionEntity")
                .addField(SharedSessionEntityFields.ROOM_ID, String::class.java)
                .addField(SharedSessionEntityFields.ALGORITHM, String::class.java)
                .addField(SharedSessionEntityFields.SESSION_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.SESSION_ID)
                .addField(SharedSessionEntityFields.USER_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.USER_ID)
                .addField(SharedSessionEntityFields.DEVICE_ID, String::class.java)
                .addIndex(SharedSessionEntityFields.DEVICE_ID)
                .addField(SharedSessionEntityFields.CHAIN_INDEX, Long::class.java)
                .setNullable(SharedSessionEntityFields.CHAIN_INDEX, true)
    }
}
