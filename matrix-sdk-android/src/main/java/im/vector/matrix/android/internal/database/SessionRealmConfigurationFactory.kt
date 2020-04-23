/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.database

import im.vector.matrix.android.internal.database.model.SessionRealmModule
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.session.SessionModule
import io.realm.RealmConfiguration
import java.io.File
import javax.inject.Inject

private const val REALM_NAME = "disk_store.realm"

/**
 * This class is handling creation of RealmConfiguration for a session.
 */
internal class SessionRealmConfigurationFactory @Inject constructor(
        private val databaseKeysUtils: DatabaseKeysUtils,
        @SessionFilesDirectory val directory: File,
        @SessionId val sessionId: String,
        @UserMd5 val userMd5: String) {

    fun create(): RealmConfiguration {
        return RealmConfiguration.Builder()
                .directory(directory)
                .name(REALM_NAME)
                .apply {
                    databaseKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                }
                .modules(SessionRealmModule())
                .deleteRealmIfMigrationNeeded()
                .build()
    }

}
