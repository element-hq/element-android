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
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoRoomEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutboundGroupSessionInfoEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

// Version 12L added outbound group session persistence
internal class MigrateCryptoTo012(realm: DynamicRealm) : RealmMigrator(realm, 12) {

    override fun doMigrate(realm: DynamicRealm) {
        val outboundEntitySchema = realm.schema.create("OutboundGroupSessionInfoEntity")
                .addField(OutboundGroupSessionInfoEntityFields.SERIALIZED_OUTBOUND_SESSION_DATA, String::class.java)
                .addField(OutboundGroupSessionInfoEntityFields.CREATION_TIME, Long::class.java)
                .setNullable(OutboundGroupSessionInfoEntityFields.CREATION_TIME, true)

        realm.schema.get("CryptoRoomEntity")
                ?.addRealmObjectField(CryptoRoomEntityFields.OUTBOUND_SESSION_INFO.`$`, outboundEntitySchema)
    }
}
