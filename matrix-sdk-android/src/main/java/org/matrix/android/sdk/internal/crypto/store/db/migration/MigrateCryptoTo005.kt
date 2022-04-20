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
import org.matrix.android.sdk.internal.crypto.store.db.model.GossipingEventEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.IncomingGossipingRequestEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingGossipingRequestEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateCryptoTo005(realm: DynamicRealm) : RealmMigrator(realm, 5) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.remove("OutgoingRoomKeyRequestEntity")
        realm.schema.remove("IncomingRoomKeyRequestEntity")

        // Not need to migrate existing request, just start fresh?

        realm.schema.create("GossipingEventEntity")
                .addField(GossipingEventEntityFields.TYPE, String::class.java)
                .addIndex(GossipingEventEntityFields.TYPE)
                .addField(GossipingEventEntityFields.CONTENT, String::class.java)
                .addField(GossipingEventEntityFields.SENDER, String::class.java)
                .addIndex(GossipingEventEntityFields.SENDER)
                .addField(GossipingEventEntityFields.DECRYPTION_RESULT_JSON, String::class.java)
                .addField(GossipingEventEntityFields.DECRYPTION_ERROR_CODE, String::class.java)
                .addField(GossipingEventEntityFields.AGE_LOCAL_TS, Long::class.java)
                .setNullable(GossipingEventEntityFields.AGE_LOCAL_TS, true)
                .addField(GossipingEventEntityFields.SEND_STATE_STR, String::class.java)

        realm.schema.create("IncomingGossipingRequestEntity")
                .addField(IncomingGossipingRequestEntityFields.REQUEST_ID, String::class.java)
                .addIndex(IncomingGossipingRequestEntityFields.REQUEST_ID)
                .addField(IncomingGossipingRequestEntityFields.TYPE_STR, String::class.java)
                .addIndex(IncomingGossipingRequestEntityFields.TYPE_STR)
                .addField(IncomingGossipingRequestEntityFields.OTHER_USER_ID, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.REQUESTED_INFO_STR, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.OTHER_DEVICE_ID, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.REQUEST_STATE_STR, String::class.java)
                .addField(IncomingGossipingRequestEntityFields.LOCAL_CREATION_TIMESTAMP, Long::class.java)
                .setNullable(IncomingGossipingRequestEntityFields.LOCAL_CREATION_TIMESTAMP, true)

        realm.schema.create("OutgoingGossipingRequestEntity")
                .addField(OutgoingGossipingRequestEntityFields.REQUEST_ID, String::class.java)
                .addIndex(OutgoingGossipingRequestEntityFields.REQUEST_ID)
                .addField(OutgoingGossipingRequestEntityFields.RECIPIENTS_DATA, String::class.java)
                .addField(OutgoingGossipingRequestEntityFields.REQUESTED_INFO_STR, String::class.java)
                .addField(OutgoingGossipingRequestEntityFields.TYPE_STR, String::class.java)
                .addIndex(OutgoingGossipingRequestEntityFields.TYPE_STR)
                .addField(OutgoingGossipingRequestEntityFields.REQUEST_STATE_STR, String::class.java)
    }
}
