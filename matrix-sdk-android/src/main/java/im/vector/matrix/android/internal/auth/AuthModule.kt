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
import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.internal.auth.db.AuthRealmMigration
import im.vector.matrix.android.internal.auth.db.AuthRealmModule
import im.vector.matrix.android.internal.auth.db.RealmPendingSessionStore
import im.vector.matrix.android.internal.auth.db.RealmSessionParamsStore
import im.vector.matrix.android.internal.auth.wellknown.DefaultDirectLoginTask
import im.vector.matrix.android.internal.auth.wellknown.DefaultGetWellknownTask
import im.vector.matrix.android.internal.auth.wellknown.DirectLoginTask
import im.vector.matrix.android.internal.auth.wellknown.GetWellknownTask
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.AuthDatabase
import io.realm.RealmConfiguration
import java.io.File

@Module
internal abstract class AuthModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-auth"

        @JvmStatic
        @Provides
        @AuthDatabase
        fun providesRealmConfiguration(context: Context, realmKeysUtils: RealmKeysUtils): RealmConfiguration {
            val old = File(context.filesDir, "matrix-sdk-auth")
            if (old.exists()) {
                old.renameTo(File(context.filesDir, "matrix-sdk-auth.realm"))
            }

            return RealmConfiguration.Builder()
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-auth.realm")
                    .modules(AuthRealmModule())
                    .schemaVersion(AuthRealmMigration.SCHEMA_VERSION)
                    .migration(AuthRealmMigration)
                    .build()
        }
    }

    @Binds
    abstract fun bindSessionParamsStore(store: RealmSessionParamsStore): SessionParamsStore

    @Binds
    abstract fun bindPendingSessionStore(store: RealmPendingSessionStore): PendingSessionStore

    @Binds
    abstract fun bindAuthenticationService(service: DefaultAuthenticationService): AuthenticationService

    @Binds
    abstract fun bindSessionCreator(creator: DefaultSessionCreator): SessionCreator

    @Binds
    abstract fun bindGetWellknownTask(task: DefaultGetWellknownTask): GetWellknownTask

    @Binds
    abstract fun bindDirectLoginTask(task: DefaultDirectLoginTask): DirectLoginTask
}
