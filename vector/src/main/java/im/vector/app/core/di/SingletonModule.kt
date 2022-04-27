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

package im.vector.app.core.di

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Resources
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.BuildConfig
import im.vector.app.EmojiCompatWrapper
import im.vector.app.EmojiSpanify
import im.vector.app.config.analyticsConfig
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.error.DefaultErrorFormatter
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.time.Clock
import im.vector.app.core.time.DefaultClock
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.impl.DefaultVectorAnalytics
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.CompileTimeAutoAcceptInvites
import im.vector.app.features.navigation.DefaultNavigator
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.SharedPrefPinCodeStore
import im.vector.app.features.room.VectorRoomDisplayNameFallbackProvider
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.SharedPreferencesUiStateRepository
import im.vector.app.features.ui.UiStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class VectorBindModule {

    @Binds
    abstract fun bindNavigator(navigator: DefaultNavigator): Navigator

    @Binds
    abstract fun bindVectorAnalytics(analytics: DefaultVectorAnalytics): VectorAnalytics

    @Binds
    abstract fun bindAnalyticsTracker(analytics: DefaultVectorAnalytics): AnalyticsTracker

    @Binds
    abstract fun bindErrorFormatter(formatter: DefaultErrorFormatter): ErrorFormatter

    @Binds
    abstract fun bindUiStateRepository(repository: SharedPreferencesUiStateRepository): UiStateRepository

    @Binds
    abstract fun bindPinCodeStore(store: SharedPrefPinCodeStore): PinCodeStore

    @Binds
    abstract fun bindAutoAcceptInvites(autoAcceptInvites: CompileTimeAutoAcceptInvites): AutoAcceptInvites

    @Binds
    abstract fun bindDefaultClock(clock: DefaultClock): Clock

    @Binds
    abstract fun bindEmojiSpanify(emojiCompatWrapper: EmojiCompatWrapper): EmojiSpanify
}

@InstallIn(SingletonComponent::class)
@Module
object VectorStaticModule {

    @Provides
    fun providesContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    fun providesResources(context: Context): Resources {
        return context.resources
    }

    @Provides
    fun providesSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("im.vector.riot", MODE_PRIVATE)
    }

    @Provides
    fun providesMatrixConfiguration(
            vectorPreferences: VectorPreferences,
            vectorRoomDisplayNameFallbackProvider: VectorRoomDisplayNameFallbackProvider): MatrixConfiguration {
        return MatrixConfiguration(
                applicationFlavor = BuildConfig.FLAVOR_DESCRIPTION,
                roomDisplayNameFallbackProvider = vectorRoomDisplayNameFallbackProvider,
                threadMessagesEnabledDefault = vectorPreferences.areThreadMessagesEnabled(),
        )
    }

    @Provides
    @Singleton
    fun providesMatrix(context: Context, configuration: MatrixConfiguration): Matrix {
        return Matrix.createInstance(context, configuration)
    }

    @Provides
    fun providesCurrentSession(activeSessionHolder: ActiveSessionHolder): Session {
        // TODO: handle session injection better
        return activeSessionHolder.getActiveSession()
    }

    @Provides
    fun providesLegacySessionImporter(matrix: Matrix): LegacySessionImporter {
        return matrix.legacySessionImporter()
    }

    @Provides
    fun providesAuthenticationService(matrix: Matrix): AuthenticationService {
        return matrix.authenticationService()
    }

    @Provides
    fun providesRawService(matrix: Matrix): RawService {
        return matrix.rawService()
    }

    @Provides
    fun providesLightweightSettingsStorage(matrix: Matrix): LightweightSettingsStorage {
        return matrix.lightweightSettingsStorage()
    }

    @Provides
    fun providesHomeServerHistoryService(matrix: Matrix): HomeServerHistoryService {
        return matrix.homeServerHistoryService()
    }

    @Provides
    @Singleton
    fun providesApplicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    fun providesCoroutineDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers(io = Dispatchers.IO, computation = Dispatchers.Default)
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    @Provides
    @NamedGlobalScope
    fun providesGlobalScope(): CoroutineScope {
        return GlobalScope
    }

    @Provides
    fun providesAnalyticsConfig(): AnalyticsConfig {
        return analyticsConfig
    }
}
