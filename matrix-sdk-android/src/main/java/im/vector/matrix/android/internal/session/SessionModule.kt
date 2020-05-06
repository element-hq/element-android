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

package im.vector.matrix.android.internal.session

import android.content.Context
import android.os.Build
import com.zhuinden.monarchy.Monarchy
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.auth.data.sessionId
import im.vector.matrix.android.api.crypto.MXCryptoConfig
import im.vector.matrix.android.api.session.InitialSyncProgressService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.accountdata.AccountDataService
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilitiesService
import im.vector.matrix.android.api.session.securestorage.SecureStorageService
import im.vector.matrix.android.api.session.securestorage.SharedSecretStorageService
import im.vector.matrix.android.internal.crypto.secrets.DefaultSharedSecretStorageService
import im.vector.matrix.android.internal.crypto.verification.VerificationMessageLiveObserver
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.database.RealmKeysUtils
import im.vector.matrix.android.internal.database.SessionRealmConfigurationFactory
import im.vector.matrix.android.internal.di.Authenticated
import im.vector.matrix.android.internal.di.DeviceId
import im.vector.matrix.android.internal.di.IdentityDatabase
import im.vector.matrix.android.internal.di.SessionCacheDirectory
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.di.SessionFilesDirectory
import im.vector.matrix.android.internal.di.SessionId
import im.vector.matrix.android.internal.di.Unauthenticated
import im.vector.matrix.android.internal.di.UserId
import im.vector.matrix.android.internal.di.UserMd5
import im.vector.matrix.android.internal.eventbus.EventBusTimberLogger
import im.vector.matrix.android.internal.network.AccessTokenInterceptor
import im.vector.matrix.android.internal.network.DefaultNetworkConnectivityChecker
import im.vector.matrix.android.internal.network.FallbackNetworkCallbackStrategy
import im.vector.matrix.android.internal.network.NetworkCallbackStrategy
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.network.PreferredNetworkCallbackStrategy
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.network.interceptors.CurlLoggingInterceptor
import im.vector.matrix.android.internal.network.token.AccessTokenProvider
import im.vector.matrix.android.internal.network.token.HomeserverAccessTokenProvider
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.homeserver.DefaultHomeServerCapabilitiesService
import im.vector.matrix.android.internal.session.identity.db.IdentityRealmModule
import im.vector.matrix.android.internal.session.room.EventRelationsAggregationUpdater
import im.vector.matrix.android.internal.session.room.create.RoomCreateEventLiveObserver
import im.vector.matrix.android.internal.session.room.prune.EventsPruner
import im.vector.matrix.android.internal.session.room.tombstone.RoomTombstoneEventLiveObserver
import im.vector.matrix.android.internal.session.securestorage.DefaultSecureStorageService
import im.vector.matrix.android.internal.session.user.accountdata.DefaultAccountDataService
import im.vector.matrix.android.internal.util.md5
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import retrofit2.Retrofit
import java.io.File
import javax.inject.Provider

@Module
internal abstract class SessionModule {

    @Module
    companion object {
        internal fun getKeyAlias(userMd5: String) = "session_db_$userMd5"

        /**
         * Rules:
         * Annotate methods with @SessionScope only the @Provides annotated methods with computation and logic.
         */

        @JvmStatic
        @Provides
        fun providesHomeServerConnectionConfig(sessionParams: SessionParams): HomeServerConnectionConfig {
            return sessionParams.homeServerConnectionConfig
        }

        @JvmStatic
        @Provides
        fun providesCredentials(sessionParams: SessionParams): Credentials {
            return sessionParams.credentials
        }

        @JvmStatic
        @UserId
        @Provides
        @SessionScope
        fun providesUserId(credentials: Credentials): String {
            return credentials.userId
        }

        @JvmStatic
        @DeviceId
        @Provides
        fun providesDeviceId(credentials: Credentials): String? {
            return credentials.deviceId
        }

        @JvmStatic
        @UserMd5
        @Provides
        @SessionScope
        fun providesUserMd5(@UserId userId: String): String {
            return userId.md5()
        }

        @JvmStatic
        @SessionId
        @Provides
        @SessionScope
        fun providesSessionId(credentials: Credentials): String {
            return credentials.sessionId()
        }

        @JvmStatic
        @Provides
        @SessionFilesDirectory
        fun providesFilesDir(@UserMd5 userMd5: String,
                             @SessionId sessionId: String,
                             context: Context): File {
            // Temporary code for migration
            val old = File(context.filesDir, userMd5)
            if (old.exists()) {
                old.renameTo(File(context.filesDir, sessionId))
            }

            return File(context.filesDir, sessionId)
        }

        @JvmStatic
        @Provides
        @SessionCacheDirectory
        fun providesCacheDir(@SessionId sessionId: String,
                             context: Context): File {
            return File(context.cacheDir, sessionId)
        }

        @JvmStatic
        @Provides
        @SessionDatabase
        @SessionScope
        fun providesRealmConfiguration(realmConfigurationFactory: SessionRealmConfigurationFactory): RealmConfiguration {
            return realmConfigurationFactory.create()
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesMonarchy(@SessionDatabase
                             realmConfiguration: RealmConfiguration): Monarchy {
            return Monarchy.Builder()
                    .setRealmConfiguration(realmConfiguration)
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
                        realmKeysUtils.configureEncryption(this, getKeyAlias(userMd5))
                    }
                    .modules(IdentityRealmModule())
                    .build()
        }

        @JvmStatic
        @Provides
        @SessionScope
        @Authenticated
        fun providesOkHttpClient(@Unauthenticated okHttpClient: OkHttpClient,
                                 @Authenticated accessTokenProvider: AccessTokenProvider): OkHttpClient {
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
        @SessionScope
        fun providesRetrofit(@Authenticated okHttpClient: Lazy<OkHttpClient>,
                             sessionParams: SessionParams,
                             retrofitFactory: RetrofitFactory): Retrofit {
            return retrofitFactory
                    .create(okHttpClient, sessionParams.homeServerConnectionConfig.homeServerUri.toString())
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesEventBus(): EventBus {
            return EventBus
                    .builder()
                    .logger(EventBusTimberLogger())
                    .build()
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesNetworkCallbackStrategy(fallbackNetworkCallbackStrategy: Provider<FallbackNetworkCallbackStrategy>,
                                            preferredNetworkCallbackStrategy: Provider<PreferredNetworkCallbackStrategy>
        ): NetworkCallbackStrategy {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                preferredNetworkCallbackStrategy.get()
            } else {
                fallbackNetworkCallbackStrategy.get()
            }
        }

        @JvmStatic
        @Provides
        @SessionScope
        fun providesMxCryptoConfig(matrixConfiguration: MatrixConfiguration): MXCryptoConfig {
            return matrixConfiguration.cryptoConfig
        }
    }

    @Binds
    @Authenticated
    abstract fun bindAccessTokenProvider(provider: HomeserverAccessTokenProvider): AccessTokenProvider

    @Binds
    abstract fun bindSession(session: DefaultSession): Session

    @Binds
    abstract fun bindNetworkConnectivityChecker(networkConnectivityChecker: DefaultNetworkConnectivityChecker): NetworkConnectivityChecker

    @Binds
    @IntoSet
    abstract fun bindGroupSummaryUpdater(groupSummaryUpdater: GroupSummaryUpdater): LiveEntityObserver

    @Binds
    @IntoSet
    abstract fun bindEventsPruner(eventsPruner: EventsPruner): LiveEntityObserver

    @Binds
    @IntoSet
    abstract fun bindEventRelationsAggregationUpdater(eventRelationsAggregationUpdater: EventRelationsAggregationUpdater): LiveEntityObserver

    @Binds
    @IntoSet
    abstract fun bindRoomTombstoneEventLiveObserver(roomTombstoneEventLiveObserver: RoomTombstoneEventLiveObserver): LiveEntityObserver

    @Binds
    @IntoSet
    abstract fun bindRoomCreateEventLiveObserver(roomCreateEventLiveObserver: RoomCreateEventLiveObserver): LiveEntityObserver

    @Binds
    @IntoSet
    abstract fun bindVerificationMessageLiveObserver(verificationMessageLiveObserver: VerificationMessageLiveObserver): LiveEntityObserver

    @Binds
    abstract fun bindInitialSyncProgressService(initialSyncProgressService: DefaultInitialSyncProgressService): InitialSyncProgressService

    @Binds
    abstract fun bindSecureStorageService(secureStorageService: DefaultSecureStorageService): SecureStorageService

    @Binds
    abstract fun bindHomeServerCapabilitiesService(homeServerCapabilitiesService: DefaultHomeServerCapabilitiesService): HomeServerCapabilitiesService

    @Binds
    abstract fun bindAccountDataService(service: DefaultAccountDataService): AccountDataService

    @Binds
    abstract fun bindSharedSecretStorageService(service: DefaultSharedSecretStorageService): SharedSecretStorageService
}
