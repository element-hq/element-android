/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.auth.LoginType
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntityFields
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateAuthTo005(realm: DynamicRealm) : RealmMigrator(realm, 5) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Update SessionParamsEntity to add LoginType")

        realm.schema.get("SessionParamsEntity")
                ?.addField(SessionParamsEntityFields.LOGIN_TYPE, String::class.java)
                ?.setRequired(SessionParamsEntityFields.LOGIN_TYPE, true)
                ?.transform { it.set(SessionParamsEntityFields.LOGIN_TYPE, LoginType.UNKNOWN.name) }
    }
}
