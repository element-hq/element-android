/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.auth

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.internal.auth.db.AuthRealmMigration
import org.matrix.android.sdk.internal.auth.db.AuthRealmModule
import org.matrix.android.sdk.internal.auth.db.RealmPendingSessionStore
import org.matrix.android.sdk.internal.auth.db.RealmSessionParamsStore
import org.matrix.android.sdk.internal.auth.login.DefaultDirectLoginTask
import org.matrix.android.sdk.internal.auth.login.DirectLoginTask
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.AuthDatabase
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

            return RealmConfiguration.Builder()
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-auth.realm")
                    .modules(AuthRealmModule())
                    .schemaVersion(authRealmMigration.schemaVersion)
                    .migration(authRealmMigration)
                    .build()
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
