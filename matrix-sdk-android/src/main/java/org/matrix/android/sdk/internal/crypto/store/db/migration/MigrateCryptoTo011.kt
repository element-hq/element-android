/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.CryptoMetadataEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator

// Version 11L added deviceKeysSentToServer boolean to CryptoMetadataEntity
internal class MigrateCryptoTo011(realm: DynamicRealm) : RealmMigrator(realm, 11) {

    override fun doMigrate(realm: DynamicRealm) {
        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.DEVICE_KEYS_SENT_TO_SERVER, Boolean::class.java)
    }
}
