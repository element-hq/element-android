/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.database

import io.realm.kotlin.RealmConfiguration
import org.matrix.android.sdk.internal.database.model.SESSION_REALM_SCHEMA
import org.matrix.android.sdk.internal.di.SessionFilesDirectory
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.UserMd5
import org.matrix.android.sdk.internal.session.SessionModule
import java.io.File
import javax.inject.Inject

private const val REALM_NAME = "disk_store.realm"

/**
 * This class is handling creation of RealmConfiguration for a session.
 * It will handle corrupted realm by clearing the db file. It allows to just clear cache without losing your crypto keys.
 * It's clearly not perfect but there is no way to catch the native crash.
 */
internal class SessionRealmConfigurationFactory @Inject constructor(
        private val realmKeysUtils: RealmKeysUtils,
        private val realmSessionStoreMigration: RealmSessionStoreMigration,
        @SessionFilesDirectory val directory: File,
        @SessionId val sessionId: String,
        @UserMd5 val userMd5: String,
) {

    fun create(): RealmConfiguration {
        val realmConfiguration = RealmConfiguration.Builder(SESSION_REALM_SCHEMA)
                .directory(directory.path)
                .name(REALM_NAME)
                .apply {
                    realmKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                }
                .schemaVersion(realmSessionStoreMigration.schemaVersion)
                .migration(realmSessionStoreMigration)
                .build()

        return realmConfiguration
    }
}
