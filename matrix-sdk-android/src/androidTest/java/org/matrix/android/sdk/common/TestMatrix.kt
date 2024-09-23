/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.BuildConfig
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.network.ApiInterceptor
import org.matrix.android.sdk.internal.network.UserAgentHolder
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This mimics the Matrix class but using TestMatrixComponent internally instead of regular MatrixComponent.
 */
internal class TestMatrix(context: Context, matrixConfiguration: MatrixConfiguration) {

    @Inject internal lateinit var authenticationService: AuthenticationService
    @Inject internal lateinit var rawService: RawService
    @Inject internal lateinit var userAgentHolder: UserAgentHolder
    @Inject internal lateinit var backgroundDetectionObserver: BackgroundDetectionObserver
    @Inject internal lateinit var sessionManager: SessionManager
    @Inject internal lateinit var homeServerHistoryService: HomeServerHistoryService
    @Inject internal lateinit var apiInterceptor: ApiInterceptor
    @Inject internal lateinit var matrixWorkerFactory: MatrixWorkerFactory

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        val appContext = context.applicationContext
        Monarchy.init(appContext)
        DaggerTestMatrixComponent.factory().create(appContext, matrixConfiguration).inject(this)
        val configuration = Configuration.Builder()
                .setExecutor(Executors.newCachedThreadPool())
                .setWorkerFactory(matrixWorkerFactory)
                .build()
        val delegate = WorkManagerImpl(
                context,
                configuration,
                WorkManagerTaskExecutor(configuration.taskExecutor)
        )
        WorkManagerImpl.setDelegate(delegate)
        uiHandler.post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
        }
    }

    fun getUserAgent() = userAgentHolder.userAgent

    fun authenticationService(): AuthenticationService {
        return authenticationService
    }

    fun rawService() = rawService

    fun homeServerHistoryService() = homeServerHistoryService

    fun registerApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.addListener(path, listener)
    }

    fun unregisterApiInterceptorListener(path: ApiPath, listener: ApiInterceptorListener) {
        apiInterceptor.removeListener(path, listener)
    }

    companion object {
        fun getSdkVersion(): String {
            return BuildConfig.SDK_VERSION + " (" + BuildConfig.GIT_SDK_REVISION + ")"
        }
    }
}
