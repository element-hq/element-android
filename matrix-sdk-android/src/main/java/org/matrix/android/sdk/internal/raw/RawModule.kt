/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.raw

import com.zhuinden.monarchy.Monarchy
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.GlobalDatabase
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory

@Module
internal abstract class RawModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-global"

        @JvmStatic
        @Provides
        @GlobalDatabase
        fun providesMonarchy(@GlobalDatabase realmConfiguration: RealmConfiguration): Monarchy {
            return Monarchy.Builder()
                    .setRealmConfiguration(realmConfiguration)
                    .build()
        }

        @JvmStatic
        @Provides
        @GlobalDatabase
        @MatrixScope
        fun providesRealmConfiguration(
                realmKeysUtils: RealmKeysUtils,
                globalRealmMigration: GlobalRealmMigration
        ): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-global.realm")
                    .schemaVersion(globalRealmMigration.schemaVersion)
                    .migration(globalRealmMigration)
                    .allowWritesOnUiThread(true)
                    .modules(GlobalRealmModule())
                    .build()
        }

        @Provides
        @JvmStatic
        fun providesRawAPI(
                @Unauthenticated okHttpClient: Lazy<OkHttpClient>,
                retrofitFactory: RetrofitFactory
        ): RawAPI {
            return retrofitFactory.create(okHttpClient, "https://example.org").create(RawAPI::class.java)
        }
    }

    @Binds
    abstract fun bindRawService(service: DefaultRawService): RawService

    @Binds
    abstract fun bindGetUrlTask(task: DefaultGetUrlTask): GetUrlTask

    @Binds
    abstract fun bindCleanRawCacheTask(task: DefaultCleanRawCacheTask): CleanRawCacheTask
}
