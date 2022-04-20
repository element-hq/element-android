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

package org.matrix.android.sdk.api

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.di.DaggerMatrixComponent
import org.matrix.android.sdk.internal.network.ApiInterceptor
import org.matrix.android.sdk.internal.network.UserAgentHolder
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import org.matrix.olm.OlmManager
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * This is the main entry point to the matrix sdk.
 * To get the singleton instance, use getInstance static method.
 */
class Matrix private constructor(context: Context, matrixConfiguration: MatrixConfiguration) {

    @Inject internal lateinit var legacySessionImporter: LegacySessionImporter
    @Inject internal lateinit var authenticationService: AuthenticationService
    @Inject internal lateinit var rawService: RawService
    @Inject internal lateinit var userAgentHolder: UserAgentHolder
    @Inject internal lateinit var backgroundDetectionObserver: BackgroundDetectionObserver
    @Inject internal lateinit var olmManager: OlmManager
    @Inject internal lateinit var sessionManager: SessionManager
    @Inject internal lateinit var homeServerHistoryService: HomeServerHistoryService
    @Inject internal lateinit var apiInterceptor: ApiInterceptor
    @Inject internal lateinit var matrixWorkerFactory: MatrixWorkerFactory
    @Inject internal lateinit var lightweightSettingsStorage: LightweightSettingsStorage

    init {
        Monarchy.init(context)
        DaggerMatrixComponent.factory().create(context, matrixConfiguration).inject(this)
        if (context.applicationContext !is Configuration.Provider) {
            val configuration = Configuration.Builder()
                    .setExecutor(Executors.newCachedThreadPool())
                    .setWorkerFactory(matrixWorkerFactory)
                    .build()
            WorkManager.initialize(context, configuration)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
    }

    fun getUserAgent() = userAgentHolder.userAgent

    fun authenticationService(): AuthenticationService {
        return authenticationService
    }

    fun rawService() = rawService

    fun lightweightSettingsStorage() = lightweightSettingsStorage

    fun homeServerHistoryService() = homeServerHistoryService

    fun legacySessionImporter(): LegacySessionImporter {
        return legacySessionImporter
    }

    fun workerFactory(): WorkerFactory = matrixWorkerFactory

    fun registerApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.addListener(path, listener)
    }

    fun unregisterApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.removeListener(path, listener)
    }

    companion object {

        private lateinit var instance: Matrix
        private val isInit = AtomicBoolean(false)

        /**
         * Creates a new instance of Matrix, it's recommended to manage this instance as a singleton.
         * To make use of the built in singleton use Matrix.initialize() and/or Matrix.getInstance(context) instead
         **/
        fun createInstance(context: Context, matrixConfiguration: MatrixConfiguration): Matrix {
            return Matrix(context.applicationContext, matrixConfiguration)
        }

        /**
         * Initializes a singleton instance of Matrix for the given MatrixConfiguration
         * This instance will be returned by Matrix.getInstance(context)
         */
        @Deprecated("Use Matrix.createInstance and manage the instance manually")
        fun initialize(context: Context, matrixConfiguration: MatrixConfiguration) {
            if (isInit.compareAndSet(false, true)) {
                instance = Matrix(context.applicationContext, matrixConfiguration)
            }
        }

        /**
         * Either provides an already initialized singleton Matrix instance or queries the application context for a MatrixConfiguration.Provider
         * to lazily create and store the instance.
         */
        @Suppress("deprecation") // suppressing warning as this method is unused but is still provided for SDK clients
        @Deprecated("Use Matrix.createInstance and manage the instance manually")
        fun getInstance(context: Context): Matrix {
            if (isInit.compareAndSet(false, true)) {
                val appContext = context.applicationContext
                if (appContext is MatrixConfiguration.Provider) {
                    val matrixConfiguration = (appContext as MatrixConfiguration.Provider).providesMatrixConfiguration()
                    instance = Matrix(appContext, matrixConfiguration)
                } else {
                    throw IllegalStateException("Matrix is not initialized properly." +
                            " If you want to manage your own Matrix instance use Matrix.createInstance" +
                            " otherwise you should call Matrix.initialize or let your application implement MatrixConfiguration.Provider.")
                }
            }
            return instance
        }

        fun getSdkVersion(): String {
            return BuildConfig.SDK_VERSION + " (" + BuildConfig.GIT_SDK_REVISION + ")"
        }
    }
}
