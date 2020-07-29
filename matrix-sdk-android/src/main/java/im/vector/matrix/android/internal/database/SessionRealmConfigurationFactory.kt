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

import android.content.Context
import androidx.core.content.edit
import im.vector.matrix.android.internal.database.model.SessionRealmModule
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.session.SessionModule
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import java.io.File
import javax.inject.Inject

private const val REALM_SHOULD_CLEAR_FLAG_ = "REALM_SHOULD_CLEAR_FLAG_"
private const val REALM_NAME = "disk_store.realm"

/**
 * This class is handling creation of RealmConfiguration for a session.
 * It will handle corrupted realm by clearing the db file. It allows to just clear cache without losing your crypto keys.
 * It's clearly not perfect but there is no way to catch the native crash.
 */
internal class SessionRealmConfigurationFactory @Inject constructor(
        private val realmKeysUtils: RealmKeysUtils,
        @SessionFilesDirectory val directory: File,
        @SessionId val sessionId: String,
        @UserMd5 val userMd5: String,
        val migration: RealmSessionStoreMigration,
        context: Context) {

    companion object {
        const val SESSION_STORE_SCHEMA_VERSION = 1L
    }

    private val sharedPreferences = context.getSharedPreferences("im.vector.matrix.android.realm", Context.MODE_PRIVATE)

    fun create(): RealmConfiguration {
        val shouldClearRealm = sharedPreferences.getBoolean("$REALM_SHOULD_CLEAR_FLAG_$sessionId", false)
        if (shouldClearRealm) {
            Timber.v("************************************************************")
            Timber.v("The realm file session was corrupted and couldn't be loaded.")
            Timber.v("The file has been deleted to recover.")
            Timber.v("************************************************************")
            deleteRealmFiles()
        }
        sharedPreferences.edit {
            putBoolean("$REALM_SHOULD_CLEAR_FLAG_$sessionId", true)
        }

        val realmConfiguration = RealmConfiguration.Builder()
                .compactOnLaunch()
                .directory(directory)
                .name(REALM_NAME)
                .apply {
                    realmKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                }
                .modules(SessionRealmModule())
                .schemaVersion(SESSION_STORE_SCHEMA_VERSION)
                .migration(migration)
                .build()

        // Try creating a realm instance and if it succeeds we can clear the flag
        Realm.getInstance(realmConfiguration).use {
            Timber.v("Successfully create realm instance")
            sharedPreferences.edit {
                putBoolean("$REALM_SHOULD_CLEAR_FLAG_$sessionId", false)
            }
        }
        return realmConfiguration
    }

    // Delete all the realm files of the session
    private fun deleteRealmFiles() {
        listOf(REALM_NAME, "$REALM_NAME.lock", "$REALM_NAME.note", "$REALM_NAME.management").forEach { file ->
            try {
                File(directory, file).deleteRecursively()
            } catch (e: Exception) {
                Timber.e(e, "Unable to delete files")
            }
        }
    }
}
