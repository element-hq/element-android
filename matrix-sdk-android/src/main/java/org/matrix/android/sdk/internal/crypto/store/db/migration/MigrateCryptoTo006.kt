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
import timber.log.Timber

internal class MigrateCryptoTo006(realm: DynamicRealm) : RealmMigrator(realm, 6) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Updating CryptoMetadataEntity table")
        realm.schema.get("CryptoMetadataEntity")
                ?.addField(CryptoMetadataEntityFields.KEY_BACKUP_RECOVERY_KEY, String::class.java)
                ?.addField(CryptoMetadataEntityFields.KEY_BACKUP_RECOVERY_KEY_VERSION, String::class.java)
    }
}
