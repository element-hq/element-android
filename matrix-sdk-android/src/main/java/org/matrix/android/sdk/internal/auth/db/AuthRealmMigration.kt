/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.auth.db

import android.net.Uri
import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.auth.data.sessionId
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal object AuthRealmMigration : RealmMigration {

    // Current schema version
    const val SCHEMA_VERSION = 4L

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.d("Migrating Auth Realm from $oldVersion to $newVersion")

        if (oldVersion <= 0) migrateTo1(realm)
        if (oldVersion <= 1) migrateTo2(realm)
        if (oldVersion <= 2) migrateTo3(realm)
        if (oldVersion <= 3) migrateTo4(realm)
    }

    private fun migrateTo1(realm: DynamicRealm) {
        Timber.d("Step 0 -> 1")
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

    private fun migrateTo2(realm: DynamicRealm) {
        Timber.d("Step 1 -> 2")
        Timber.d("Add boolean isTokenValid in SessionParamsEntity, with value true")

        realm.schema.get("SessionParamsEntity")
                ?.addField(SessionParamsEntityFields.IS_TOKEN_VALID, Boolean::class.java)
                ?.transform { it.set(SessionParamsEntityFields.IS_TOKEN_VALID, true) }
    }

    private fun migrateTo3(realm: DynamicRealm) {
        Timber.d("Step 2 -> 3")
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

    private fun migrateTo4(realm: DynamicRealm) {
        Timber.d("Step 3 -> 4")
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
