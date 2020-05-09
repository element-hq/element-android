/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.identity

import dagger.Binds
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.di.AuthenticatedIdentity
import im.vector.matrix.android.internal.di.IdentityDatabase
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.network.AccessTokenInterceptor
import im.vector.matrix.android.internal.network.interceptors.CurlLoggingInterceptor
import im.vector.matrix.android.internal.network.token.AccessTokenProvider
import im.vector.matrix.android.internal.session.SessionModule
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.session.identity.db.IdentityRealmModule
import im.vector.matrix.android.internal.session.identity.db.IdentityServiceStore
import im.vector.matrix.android.internal.session.identity.db.RealmIdentityServiceStore
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import java.io.File

@Module
internal abstract class IdentityModule {

    @Module
    companion object {
        @JvmStatic
        @Provides
        @SessionScope
        @AuthenticatedIdentity
        fun providesOkHttpClient(@Unauthenticated okHttpClient: OkHttpClient,
                                 @AuthenticatedIdentity accessTokenProvider: AccessTokenProvider): OkHttpClient {
            // TODO Create an helper because there is code duplication
            return okHttpClient.newBuilder()
                    .apply {
                        // Remove the previous CurlLoggingInterceptor, to add it after the accessTokenInterceptor
                        val existingCurlInterceptors = interceptors().filterIsInstance<CurlLoggingInterceptor>()
                        interceptors().removeAll(existingCurlInterceptors)

                        addInterceptor(AccessTokenInterceptor(accessTokenProvider))

                        // Re add eventually the curl logging interceptors
                        existingCurlInterceptors.forEach {
                            addInterceptor(it)
                        }
                    }
                    .build()
        }

        @JvmStatic
        @Provides
        @IdentityDatabase
        @SessionScope
        fun providesIdentityRealmConfiguration(realmKeysUtils: RealmKeysUtils,
                                               @SessionFilesDirectory directory: File,
                                               @UserMd5 userMd5: String): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .directory(directory)
                    .name("matrix-sdk-identity.realm")
                    .apply {
                        realmKeysUtils.configureEncryption(this, SessionModule.getKeyAlias(userMd5))
                    }
                    .modules(IdentityRealmModule())
                    // TODO Handle migration properly
                    .deleteRealmIfMigrationNeeded()
                    .build()
        }

    }

    @Binds
    @AuthenticatedIdentity
    abstract fun bindAccessTokenProvider(provider: IdentityAccessTokenProvider): AccessTokenProvider

    @Binds
    abstract fun bindIdentityServiceStore(store: RealmIdentityServiceStore): IdentityServiceStore

    @Binds
    abstract fun bindIdentityRegisterTask(task: DefaultIdentityRegisterTask): IdentityRegisterTask

    @Binds
    abstract fun bindIdentityRequestTokenForBindingTask(task: DefaultIdentityRequestTokenForBindingTask): IdentityRequestTokenForBindingTask

    @Binds
    abstract fun bindBulkLookupTask(task: DefaultBulkLookupTask): BulkLookupTask

    @Binds
    abstract fun bindIdentityDisconnectTask(task: DefaultIdentityDisconnectTask): IdentityDisconnectTask
}
