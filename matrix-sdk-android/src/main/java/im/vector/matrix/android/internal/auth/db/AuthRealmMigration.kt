/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.matrix.android.internal.auth.db

import io.realm.DynamicRealm
import io.realm.RealmMigration
import timber.log.Timber

internal class AuthRealmMigration : RealmMigration {

    companion object {
        // Current schema version
        const val SCHEMA_VERSION = 1L
    }

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        Timber.d("Migrating Auth Realm from $oldVersion to $newVersion")

        if (oldVersion <= 0) {
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
    }
}
