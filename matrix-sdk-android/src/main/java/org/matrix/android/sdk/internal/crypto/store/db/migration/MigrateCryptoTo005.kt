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
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateCryptoTo005(realm: DynamicRealm) : RealmMigrator(realm, 5) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.remove("OutgoingRoomKeyRequestEntity")
        realm.schema.remove("IncomingRoomKeyRequestEntity")

        // Not need to migrate existing request, just start fresh?
        realm.schema.create("GossipingEventEntity")
                .addField("type", String::class.java)
                .addIndex("type")
                .addField("content", String::class.java)
                .addField("sender", String::class.java)
                .addIndex("sender")
                .addField("decryptionResultJson", String::class.java)
                .addField("decryptionErrorCode", String::class.java)
                .addField("ageLocalTs", Long::class.java)
                .setNullable("ageLocalTs", true)
                .addField("sendStateStr", String::class.java)

        realm.schema.create("IncomingGossipingRequestEntity")
                .addField("requestId", String::class.java)
                .addIndex("requestId")
                .addField("typeStr", String::class.java)
                .addIndex("typeStr")
                .addField("otherUserId", String::class.java)
                .addField("requestedInfoStr", String::class.java)
                .addField("otherDeviceId", String::class.java)
                .addField("requestStateStr", String::class.java)
                .addField("localCreationTimestamp", Long::class.java)
                .setNullable("localCreationTimestamp", true)

        realm.schema.create("OutgoingGossipingRequestEntity")
                .addField("requestId", String::class.java)
                .addIndex("requestId")
                .addField("recipientsData", String::class.java)
                .addField("requestedInfoStr", String::class.java)
                .addField("typeStr", String::class.java)
                .addIndex("typeStr")
                .addField("requestStateStr", String::class.java)
    }
}
