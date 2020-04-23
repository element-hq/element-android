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

package im.vector.matrix.android.internal.auth

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.internal.auth.realm.AuthRealmMigration
import im.vector.matrix.android.internal.auth.realm.AuthRealmModule
import im.vector.matrix.android.internal.auth.sqlite.AuthSchema
import im.vector.matrix.android.internal.auth.sqlite.SqlitePendingSessionStore
import im.vector.matrix.android.internal.auth.sqlite.SqliteSessionParamsStore
import im.vector.matrix.android.internal.database.DatabaseKeysUtils
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.sqldelight.auth.AuthDatabase
import io.realm.RealmConfiguration
import java.io.File

@Module
internal abstract class AuthModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-auth"

        @JvmStatic
        @Provides
        @im.vector.matrix.android.internal.di.RealmAuthDatabase
        @MatrixScope
        fun providesRealmConfiguration(context: Context, databaseKeysUtils: DatabaseKeysUtils): RealmConfiguration {
            val old = File(context.filesDir, "matrix-sdk-auth")
            if (old.exists()) {
                old.renameTo(File(context.filesDir, "matrix-sdk-auth.realm"))
            }
            return RealmConfiguration.Builder()
                    .apply {
                        databaseKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-auth.realm")
                    .modules(AuthRealmModule())
                    .schemaVersion(AuthRealmMigration.SCHEMA_VERSION)
                    .migration(AuthRealmMigration)
                    .build()
        }

        @JvmStatic
        @Provides
        @MatrixScope
        fun providesAuthDatabase(context: Context, authSchema: AuthSchema, databaseKeysUtils: DatabaseKeysUtils): AuthDatabase {
            val supportFactory = databaseKeysUtils.createEncryptedSQLiteOpenHelperFactory(DB_ALIAS)
            val driver = AndroidSqliteDriver(authSchema, context, "matrix-sdk-auth.db", factory = supportFactory)
            return AuthDatabase.invoke(driver)
        }
    }

    @Binds
    abstract fun bindSessionParamsStore(sessionParamsStore: SqliteSessionParamsStore): SessionParamsStore

    @Binds
    abstract fun bindPendingSessionStore(pendingSessionStore: SqlitePendingSessionStore): PendingSessionStore

    @Binds
    abstract fun bindAuthenticationService(authenticationService: DefaultAuthenticationService): AuthenticationService

    @Binds
    abstract fun bindSessionCreator(sessionCreator: DefaultSessionCreator): SessionCreator
}
