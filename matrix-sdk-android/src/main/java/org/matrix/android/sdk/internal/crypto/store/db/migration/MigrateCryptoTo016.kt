/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
