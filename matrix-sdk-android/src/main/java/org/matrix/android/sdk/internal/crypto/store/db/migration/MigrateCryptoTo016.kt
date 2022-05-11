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
import org.matrix.android.sdk.internal.crypto.store.db.model.AuditTrailEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.KeyRequestReplyEntityFields
import org.matrix.android.sdk.internal.crypto.store.db.model.OutgoingKeyRequestEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

internal class MigrateCryptoTo016(realm: DynamicRealm) : RealmMigrator(realm, 16) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.remove("OutgoingGossipingRequestEntity")
        realm.schema.remove("IncomingGossipingRequestEntity")
        realm.schema.remove("GossipingEventEntity")

        // No need to migrate existing request, just start fresh

        val replySchema = realm.schema.create("KeyRequestReplyEntity")
                .addField(KeyRequestReplyEntityFields.SENDER_ID, String::class.java)
                .addField(KeyRequestReplyEntityFields.FROM_DEVICE, String::class.java)
                .addField(KeyRequestReplyEntityFields.EVENT_JSON, String::class.java)

        realm.schema.create("OutgoingKeyRequestEntity")
                .addField(OutgoingKeyRequestEntityFields.REQUEST_ID, String::class.java)
                .addIndex(OutgoingKeyRequestEntityFields.REQUEST_ID)
                .addField(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID, String::class.java)
                .addIndex(OutgoingKeyRequestEntityFields.MEGOLM_SESSION_ID)
                .addRealmListField(OutgoingKeyRequestEntityFields.REPLIES.`$`, replySchema)
                .addField(OutgoingKeyRequestEntityFields.RECIPIENTS_DATA, String::class.java)
                .addField(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR, String::class.java)
                .addIndex(OutgoingKeyRequestEntityFields.REQUEST_STATE_STR)
                .addField(OutgoingKeyRequestEntityFields.REQUESTED_INFO_STR, String::class.java)
                .addField(OutgoingKeyRequestEntityFields.ROOM_ID, String::class.java)
                .addIndex(OutgoingKeyRequestEntityFields.ROOM_ID)
                .addField(OutgoingKeyRequestEntityFields.REQUESTED_INDEX, Integer::class.java)
                .addField(OutgoingKeyRequestEntityFields.CREATION_TIME_STAMP, Long::class.java)
                .setNullable(OutgoingKeyRequestEntityFields.CREATION_TIME_STAMP, true)

        realm.schema.create("AuditTrailEntity")
                .addField(AuditTrailEntityFields.AGE_LOCAL_TS, Long::class.java)
                .setNullable(AuditTrailEntityFields.AGE_LOCAL_TS, true)
                .addField(AuditTrailEntityFields.CONTENT_JSON, String::class.java)
                .addField(AuditTrailEntityFields.TYPE, String::class.java)
                .addIndex(AuditTrailEntityFields.TYPE)

        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.GLOBAL_ENABLE_KEY_GOSSIPING, Boolean::class.java)
                ?.transform {
                    // set the default value to true
                    it.setBoolean(CryptoMetadataEntityFields.GLOBAL_ENABLE_KEY_GOSSIPING, true)
                }
    }
}
