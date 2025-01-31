/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth.db.migration

import android.net.Uri
import io.realm.DynamicRealm
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.auth.db.SessionParamsEntityFields
import org.matrix.android.sdk.internal.di.MoshiProvider
import org.matrix.android.sdk.internal.util.database.RealmMigrator
import timber.log.Timber

internal class MigrateAuthTo004(realm: DynamicRealm) : RealmMigrator(realm, 4) {

    override fun doMigrate(realm: DynamicRealm) {
        Timber.d("Update SessionParamsEntity to add HomeServerConnectionConfig.homeServerUriBase value")

        val adapter = MoshiProvider.providesMoshi()
                .adapter(HomeServerConnectionConfig::class.java)

        realm.schema.get("SessionParamsEntity")
                ?.transform {
                    val homeserverConnectionConfigJson = it.getString(SessionParamsEntityFields.HOME_SERVER_CONNECTION_CONFIG_JSON)

                    val homeserverConnectionConfig = adapter
                            .fromJson(homeserverConnectionConfigJson)

                    val homeserverUrl = homeserverConnectionConfig?.homeServerUri?.toString()
                    // Special case for matrix.org. Old session may use "https://matrix.org", newer one may use
                    // "https://matrix-client.matrix.org". So fix that here
                    val alteredHomeserverConnectionConfig =
                            if (homeserverUrl == "https://matrix.org" || homeserverUrl == "https://matrix-client.matrix.org") {
                                homeserverConnectionConfig.copy(
                                        homeServerUri = Uri.parse("https://matrix.org"),
                                        homeServerUriBase = Uri.parse("https://matrix-client.matrix.org")
                                )
                            } else {
                                homeserverConnectionConfig
                            }
                    it.set(SessionParamsEntityFields.HOME_SERVER_CONNECTION_CONFIG_JSON, adapter.toJson(alteredHomeserverConnectionConfig))
                }
    }
}
