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

package im.vector.matrix.android.internal.di

import android.content.Context
import android.content.res.Resources
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.auth.SessionParamsStore
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import okhttp3.OkHttpClient
import org.matrix.olm.OlmManager


@Component(modules = [MatrixModule::class, NetworkModule::class, AuthModule::class])
@MatrixScope
internal interface MatrixComponent {

    fun matrixCoroutineDispatchers(): MatrixCoroutineDispatchers

    fun moshi(): Moshi

    @Unauthenticated
    fun okHttpClient(): OkHttpClient

    fun authenticator(): Authenticator

    fun context(): Context

    fun resources(): Resources

    fun olmManager(): OlmManager

    fun taskExecutor(): TaskExecutor

    fun sessionParamsStore(): SessionParamsStore

    fun networkConnectivityChecker(): NetworkConnectivityChecker

    fun backgroundDetectionObserver(): BackgroundDetectionObserver

    fun sessionManager(): SessionManager

    fun inject(matrix: Matrix)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): MatrixComponent
    }

}