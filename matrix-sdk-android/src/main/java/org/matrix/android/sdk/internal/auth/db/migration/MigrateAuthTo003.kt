/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db.migration

import io.realm.DynamicRealm
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateAuthTo003(realm: DynamicRealm) : RealmMigrator(realm, 3) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Update SessionParamsEntity primary key, to allow several sessions with the same userId")

        realm.schema.get("SessionParamsEntity")
                ?.removePrimaryKey()
                ?.addField(SessionParamsEntityFields.SESSION_ID, String::class.java)
                ?.setRequired(SessionParamsEntityFields.SESSION_ID, true)
                ?.transform {
                    val credentialsJson = it.getString(SessionParamsEntityFields.CREDENTIALS_JSON)

                    val credentials = MoshiProvider.providesMoshi()
                            .adapter(Credentials::class.java)
                            .fromJson(credentialsJson)

                    it.set(SessionParamsEntityFields.SESSION_ID, credentials!!.sessionId())
                }
                ?.addPrimaryKey(SessionParamsEntityFields.SESSION_ID)
    }
}
