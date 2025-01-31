/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.store.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.crypto.store.db.model.OlmSessionEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateCryptoTo001Legacy(realm: DynamicRealm) : RealmMigrator(realm, 1) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Add field lastReceivedMessageTs (Long) and set the value to 0")

        realm.schema.get("OlmSessionEntity")
                ?.addField(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, Long::class.java)
                ?.transform {
                    it.setLong(OlmSessionEntityFields.LAST_RECEIVED_MESSAGE_TS, 0)
                }
    }
}
