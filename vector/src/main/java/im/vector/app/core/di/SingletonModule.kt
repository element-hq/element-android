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
import im.vector.app.core.dispatchers.CoroutineDispatchers
import im.vector.app.core.error.DefaultErrorFormatter
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.features.invite.AutoAcceptInvites
import im.vector.app.features.invite.CompileTimeAutoAcceptInvites
import im.vector.app.features.navigation.DefaultNavigator
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.SharedPrefPinCodeStore
import im.vector.app.features.ui.SharedPreferencesUiStateRepository
import im.vector.app.features.ui.UiStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class VectorBindModule {

    @Binds
    abstract fun bindNavigator(navigator: DefaultNavigator): Navigator

    @Binds
    abstract fun bindErrorFormatter(formatter: DefaultErrorFormatter): ErrorFormatter

    @Binds
    abstract fun bindUiStateRepository(repository: SharedPreferencesUiStateRepository): UiStateRepository

    @Binds
    abstract fun bindPinCodeStore(store: SharedPrefPinCodeStore): PinCodeStore

    @Binds
    abstract fun bindAutoAcceptInvites(autoAcceptInvites: CompileTimeAutoAcceptInvites): AutoAcceptInvites
}

@InstallIn(SingletonComponent::class)
@Module
object VectorStaticModule {

    @Provides
    @JvmStatic
    fun providesContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @JvmStatic
    fun providesResources(context: Context): Resources {
        return context.resources
    }

    @Provides
    @JvmStatic
    fun providesSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("im.vector.riot", MODE_PRIVATE)
    }

    @Provides
    @JvmStatic
    fun providesMatrix(context: Context): Matrix {
        return Matrix.getInstance(context)
    }

    @Provides
    @JvmStatic
    fun providesCurrentSession(activeSessionHolder: ActiveSessionHolder): Session {
        // TODO: handle session injection better
        return activeSessionHolder.getActiveSession()
    }

    @Provides
    @JvmStatic
    fun providesLegacySessionImporter(matrix: Matrix): LegacySessionImporter {
        return matrix.legacySessionImporter()
    }

    @Provides
    @JvmStatic
    fun providesAuthenticationService(matrix: Matrix): AuthenticationService {
        return matrix.authenticationService()
    }

    @Provides
    @JvmStatic
    fun providesRawService(matrix: Matrix): RawService {
        return matrix.rawService()
    }

    @Provides
    @JvmStatic
    fun providesHomeServerHistoryService(matrix: Matrix): HomeServerHistoryService {
        return matrix.homeServerHistoryService()
    }

    @Provides
    @JvmStatic
    @Singleton
    fun providesApplicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @JvmStatic
    fun providesCoroutineDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers(io = Dispatchers.IO, computation = Dispatchers.Default)
    }
}
