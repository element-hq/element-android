/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.internal.auth.db.PendingSessionEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateAuthTo001(realm: DynamicRealm) : RealmMigrator(realm, 1) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Create PendingSessionEntity")

        realm.schema.create("PendingSessionEntity")
                .addField(PendingSessionEntityFields.HOME_SERVER_CONNECTION_CONFIG_JSON, String::class.java)
                .setRequired(PendingSessionEntityFields.HOME_SERVER_CONNECTION_CONFIG_JSON, true)
                .addField(PendingSessionEntityFields.CLIENT_SECRET, String::class.java)
                .setRequired(PendingSessionEntityFields.CLIENT_SECRET, true)
                .addField(PendingSessionEntityFields.SEND_ATTEMPT, Integer::class.java)
                .setRequired(PendingSessionEntityFields.SEND_ATTEMPT, true)
                .addField(PendingSessionEntityFields.RESET_PASSWORD_DATA_JSON, String::class.java)
                .addField(PendingSessionEntityFields.CURRENT_SESSION, String::class.java)
                .addField(PendingSessionEntityFields.IS_REGISTRATION_STARTED, Boolean::class.java)
                .addField(PendingSessionEntityFields.CURRENT_THREE_PID_DATA_JSON, String::class.java)
    }
}
