/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.api.network.ApiInterceptorListener
import org.matrix.android.sdk.api.network.ApiPath
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.network.ApiInterceptor
import org.matrix.android.sdk.internal.network.UserAgentHolder
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import org.matrix.olm.OlmManager
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * This mimics the Matrix class but using TestMatrixComponent internally instead of regular MatrixComponent.
 */
internal class TestMatrix(context: Context, matrixConfiguration: MatrixConfiguration) {

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

    fun legacySessionImporter(): LegacySessionImporter {
        return legacySessionImporter
    }

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
