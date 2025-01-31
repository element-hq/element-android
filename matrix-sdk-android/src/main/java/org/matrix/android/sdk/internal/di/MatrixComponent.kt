/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import android.content.Context
import android.content.res.Resources
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.securestorage.SecureStorageModule
import org.matrix.android.sdk.api.securestorage.SecureStorageService
import org.matrix.android.sdk.api.settings.LightweightSettingsStorage
import org.matrix.android.sdk.internal.SessionManager
import org.matrix.android.sdk.internal.auth.AuthModule
import org.matrix.android.sdk.internal.auth.SessionParamsStore
import org.matrix.android.sdk.internal.debug.DebugModule
import org.matrix.android.sdk.internal.raw.RawModule
import org.matrix.android.sdk.internal.session.MockHttpInterceptor
import org.matrix.android.sdk.internal.session.TestInterceptor
import org.matrix.android.sdk.internal.settings.SettingsModule
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.system.SystemModule
import org.matrix.android.sdk.internal.worker.MatrixWorkerFactory
import org.matrix.olm.OlmManager
import java.io.File

@Component(
        modules = [
            MatrixModule::class,
            NetworkModule::class,
            AuthModule::class,
            RawModule::class,
            DebugModule::class,
            SettingsModule::class,
            SystemModule::class,
            NoOpTestModule::class,
            SecureStorageModule::class,
        ]
)
@MatrixScope
internal interface MatrixComponent {

    fun matrixCoroutineDispatchers(): MatrixCoroutineDispatchers

    fun moshi(): Moshi

    @Unauthenticated
    fun okHttpClient(): OkHttpClient

    @MockHttpInterceptor
    fun testInterceptor(): TestInterceptor?

    fun authenticationService(): AuthenticationService

    fun rawService(): RawService

    fun lightweightSettingsStorage(): LightweightSettingsStorage

    fun homeServerHistoryService(): HomeServerHistoryService

    fun context(): Context

    fun matrixConfiguration(): MatrixConfiguration

    fun resources(): Resources

    @CacheDirectory
    fun cacheDir(): File

    fun olmManager(): OlmManager

    fun taskExecutor(): TaskExecutor

    fun sessionParamsStore(): SessionParamsStore

    fun backgroundDetectionObserver(): BackgroundDetectionObserver

    fun sessionManager(): SessionManager

    fun secureStorageService(): SecureStorageService

    fun matrixWorkerFactory(): MatrixWorkerFactory

    fun inject(matrix: Matrix)

    @Component.Factory
    interface Factory {
        fun create(
                @BindsInstance context: Context,
                @BindsInstance matrixConfiguration: MatrixConfiguration
        ): MatrixComponent
    }
}
