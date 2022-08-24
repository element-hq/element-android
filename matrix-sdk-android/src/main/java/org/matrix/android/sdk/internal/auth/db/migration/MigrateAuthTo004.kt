/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.auth.db.migration

import android.net.Uri
import io.realm.kotlin.migration.AutomaticSchemaMigration
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.internal.database.KotlinRealmMigrator
import org.matrix.android.sdk.internal.di.MoshiProvider
import timber.log.Timber

internal class MigrateAuthTo004(migrationContext: AutomaticSchemaMigration.MigrationContext) : KotlinRealmMigrator(migrationContext, targetSchemaVersion = 4) {

    override fun doMigrate(migrationContext: AutomaticSchemaMigration.MigrationContext) {
        Timber.d("Update SessionParamsEntity to add HomeServerConnectionConfig.homeServerUriBase value")

        val adapter = MoshiProvider.providesMoshi()
                .adapter(HomeServerConnectionConfig::class.java)

        migrationContext.enumerate("SessionParamsEntity") { oldObj, newObj ->
            val homeserverConnectionConfigJson = oldObj.getValue("homeServerConnectionConfigJson", String::class)
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
            newObj?.set("homeServerConnectionConfigJson", adapter.toJson(alteredHomeserverConnectionConfig))
        }
    }
}
