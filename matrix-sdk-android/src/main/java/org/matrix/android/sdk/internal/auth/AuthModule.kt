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

package org.matrix.android.sdk.internal.auth

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.internal.auth.db.AUTH_REALM_SCHEMA
import org.matrix.android.sdk.internal.auth.db.AuthRealmMigration
import org.matrix.android.sdk.internal.auth.db.RealmPendingSessionStore
import org.matrix.android.sdk.internal.auth.db.RealmSessionParamsStore
import org.matrix.android.sdk.internal.auth.login.DefaultDirectLoginTask
import org.matrix.android.sdk.internal.auth.login.DirectLoginTask
import org.matrix.android.sdk.internal.database.RealmInstance
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.AuthDatabase
import org.matrix.android.sdk.internal.di.MatrixCoroutineScope
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.legacy.DefaultLegacySessionImporter
import org.matrix.android.sdk.internal.wellknown.WellknownModule
import java.io.File

@Module(includes = [WellknownModule::class])
internal abstract class AuthModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-auth"

        @JvmStatic
        @Provides
        @AuthDatabase
        fun providesRealmConfiguration(
                context: Context,
                realmKeysUtils: RealmKeysUtils,
                authRealmMigration: AuthRealmMigration
        ): RealmConfiguration {
            val old = File(context.filesDir, "matrix-sdk-auth")
            if (old.exists()) {
                old.renameTo(File(context.filesDir, "matrix-sdk-auth.realm"))
            }

            return RealmConfiguration.Builder(AUTH_REALM_SCHEMA)
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-auth.realm")
                    .schemaVersion(authRealmMigration.schemaVersion)
                    .migration(authRealmMigration)
                    .build()
        }

        @JvmStatic
        @Provides
        @AuthDatabase
        @MatrixScope
        fun providesRealmInstance(
                @AuthDatabase realmConfiguration: RealmConfiguration,
                @MatrixCoroutineScope matrixCoroutineScope: CoroutineScope,
                matrixCoroutineDispatchers: MatrixCoroutineDispatchers
        ): RealmInstance {
            return RealmInstance(
                    coroutineScope = matrixCoroutineScope,
                    realmConfiguration = realmConfiguration,
                    coroutineDispatcher = matrixCoroutineDispatchers.io
            )
        }
    }

    @Binds
    abstract fun bindLegacySessionImporter(importer: DefaultLegacySessionImporter): LegacySessionImporter

    @Binds
    abstract fun bindSessionParamsStore(store: RealmSessionParamsStore): SessionParamsStore

    @Binds
    abstract fun bindPendingSessionStore(store: RealmPendingSessionStore): PendingSessionStore

    @Binds
    abstract fun bindAuthenticationService(service: DefaultAuthenticationService): AuthenticationService

    @Binds
    abstract fun bindSessionCreator(creator: DefaultSessionCreator): SessionCreator

    @Binds
    abstract fun bindSessionParamsCreator(creator: DefaultSessionParamsCreator): SessionParamsCreator

    @Binds
    abstract fun bindDirectLoginTask(task: DefaultDirectLoginTask): DirectLoginTask

    @Binds
    abstract fun bindIsValidClientServerApiTask(task: DefaultIsValidClientServerApiTask): IsValidClientServerApiTask

    @Binds
    abstract fun bindHomeServerHistoryService(service: DefaultHomeServerHistoryService): HomeServerHistoryService
}
