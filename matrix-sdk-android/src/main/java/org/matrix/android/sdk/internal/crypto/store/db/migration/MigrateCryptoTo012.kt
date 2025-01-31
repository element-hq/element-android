/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
