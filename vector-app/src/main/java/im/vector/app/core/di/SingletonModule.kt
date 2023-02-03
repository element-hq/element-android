/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.di

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.EmojiCompatWrapper
import im.vector.app.EmojiSpanify
import im.vector.app.SpaceStateHandler
import im.vector.app.SpaceStateHandlerImpl
import im.vector.app.config.Config
import im.vector.app.core.debug.FlipperProxy
import im.vector.app.core.device.DefaultGetDeviceInfoUseCase
import im.vector.app.core.device.GetDeviceInfoUseCase
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.error.DefaultErrorFormatter
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.time.Clock
import im.vector.app.core.time.DefaultClock
import im.vector.app.core.utils.AndroidSystemSettingsProvider
import im.vector.app.core.utils.SystemSettingsProvider
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.VectorAnalytics
import im.vector.app.features.analytics.errors.ErrorTracker
import im.vector.app.features.analytics.impl.DefaultVectorAnalytics
import im.vector.app.features.analytics.metrics.VectorPlugins
import im.vector.app.features.configuration.VectorCustomEventTypesProvider
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.CompileTimeAutoAcceptInvites
import im.vector.app.features.navigation.DefaultNavigator
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.SharedPrefPinCodeStore
import im.vector.app.features.room.VectorRoomDisplayNameFallbackProvider
import im.vector.app.features.settings.FontScalePreferences
import im.vector.app.features.settings.FontScalePreferencesImpl
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.SharedPreferencesUiStateRepository
import im.vector.app.features.ui.UiStateRepository
import im.vector.application.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.SyncConfig
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.sync.filter.SyncFilterParams
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import javax.inject.Singleton

@InstallIn(SingletonComponent::class) @Module abstract class VectorBindModule {

    @Binds
    abstract fun bindNavigator(navigator: DefaultNavigator): Navigator

    @Binds
    abstract fun bindVectorAnalytics(analytics: DefaultVectorAnalytics): VectorAnalytics

    @Binds
    abstract fun bindErrorTracker(analytics: DefaultVectorAnalytics): ErrorTracker

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

    @Binds
    abstract fun bindFontScale(fontScale: FontScalePreferencesImpl): FontScalePreferences

    @Binds
    abstract fun bindSystemSettingsProvide(provider: AndroidSystemSettingsProvider): SystemSettingsProvider

    @Binds
    abstract fun bindSpaceStateHandler(spaceStateHandlerImpl: SpaceStateHandlerImpl): SpaceStateHandler

    @Binds
    abstract fun bindGetDeviceInfoUseCase(getDeviceInfoUseCase: DefaultGetDeviceInfoUseCase): GetDeviceInfoUseCase
}

@InstallIn(SingletonComponent::class) @Module object VectorStaticModule {

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
            vectorRoomDisplayNameFallbackProvider: VectorRoomDisplayNameFallbackProvider,
            flipperProxy: FlipperProxy,
            vectorPlugins: VectorPlugins,
            vectorCustomEventTypesProvider: VectorCustomEventTypesProvider,
    ): MatrixConfiguration {
        return MatrixConfiguration(
                applicationFlavor = BuildConfig.FLAVOR_DESCRIPTION,
                roomDisplayNameFallbackProvider = vectorRoomDisplayNameFallbackProvider,
                threadMessagesEnabledDefault = vectorPreferences.areThreadMessagesEnabled(),
                networkInterceptors = listOfNotNull(
                        flipperProxy.networkInterceptor(),
                ),
                metricPlugins = vectorPlugins.plugins(),
                customEventTypesProvider = vectorCustomEventTypesProvider,
                syncConfig = SyncConfig(
                        syncFilterParams = SyncFilterParams(lazyLoadMembersForStateEvents = true, useThreadNotifications = true)
                )
        )
    }

    @Provides
    @Singleton
    fun providesMatrix(context: Context, configuration: MatrixConfiguration): Matrix {
        return Matrix(context, configuration)
    }

    @Provides
    fun providesCurrentSession(activeSessionHolder: ActiveSessionHolder): Session {
        // TODO handle session injection better
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

    @OptIn(DelicateCoroutinesApi::class)
    @Provides
    @NamedGlobalScope
    fun providesGlobalScope(): CoroutineScope {
        return GlobalScope
    }

    @Provides
    fun providesPhoneNumberUtil(): PhoneNumberUtil = PhoneNumberUtil.getInstance()

    @Provides
    @Singleton
    fun providesBuildMeta() = BuildMeta(
            isDebug = BuildConfig.DEBUG,
            applicationId = BuildConfig.APPLICATION_ID,
            lowPrivacyLoggingEnabled = Config.LOW_PRIVACY_LOG_ENABLE,
            versionName = BuildConfig.VERSION_NAME,
            gitRevision = BuildConfig.GIT_REVISION,
            gitRevisionDate = BuildConfig.GIT_REVISION_DATE,
            gitBranchName = BuildConfig.GIT_BRANCH_NAME,
            flavorDescription = BuildConfig.FLAVOR_DESCRIPTION,
            flavorShortDescription = BuildConfig.SHORT_FLAVOR_DESCRIPTION,
    )

    @Provides
    @Singleton
    @DefaultPreferences
    fun providesDefaultSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }
}
