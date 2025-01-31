/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
