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

package im.vector.matrix.android.api

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.MatrixConfiguration
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.common.DaggerTestMatrixComponent
import im.vector.matrix.android.internal.SessionManager
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.matrix.android.internal.crypto.attachments.MXEncryptedAttachments
import im.vector.matrix.android.internal.di.MockHttpInterceptor
import im.vector.matrix.android.internal.network.UserAgentHolder
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import okhttp3.Interceptor
import org.matrix.olm.OlmManager
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * This is the main entry point to the matrix sdk.
 * To get the singleton instance, use getInstance static method.
 */
class Matrix private constructor(context: Context, matrixConfiguration: MatrixConfiguration)  {

    @Inject internal lateinit var authenticationService: AuthenticationService
    @Inject internal lateinit var userAgentHolder: UserAgentHolder
    @Inject internal lateinit var backgroundDetectionObserver: BackgroundDetectionObserver
    @Inject internal lateinit var olmManager: OlmManager
    @Inject internal lateinit var sessionManager: SessionManager


    init {
        Monarchy.init(context)
        DaggerTestMatrixComponent.factory().create(context, matrixConfiguration).inject(this)
        if (context.applicationContext !is Configuration.Provider) {
            WorkManager.initialize(context, Configuration.Builder().setExecutor(Executors.newCachedThreadPool()).build())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
    }

    fun getUserAgent() = userAgentHolder.userAgent

    fun authenticationService(): AuthenticationService {
        return authenticationService
    }

    companion object {

        private lateinit var instance: Matrix
        private val isInit = AtomicBoolean(false)

        fun initialize(context: Context, matrixConfiguration: MatrixConfiguration) {
            if (isInit.compareAndSet(false, true)) {
                instance = Matrix(context.applicationContext, matrixConfiguration)
            }
        }

        fun getInstance(context: Context): Matrix {
            if (isInit.compareAndSet(false, true)) {
                val appContext = context.applicationContext
                if (appContext is MatrixConfiguration.Provider) {
                    val matrixConfiguration = (appContext as MatrixConfiguration.Provider).providesMatrixConfiguration()
                    instance = Matrix(appContext, matrixConfiguration)
                } else {
                    throw IllegalStateException("Matrix is not initialized properly." +
                            " You should call Matrix.initialize or let your application implements MatrixConfiguration.Provider.")
                }
            }
            return instance
        }

        fun getSdkVersion(): String {
            return BuildConfig.VERSION_NAME + " (" + BuildConfig.GIT_SDK_REVISION + ")"
        }

        fun decryptStream(inputStream: InputStream?, elementToDecrypt: ElementToDecrypt): InputStream? {
            return MXEncryptedAttachments.decryptAttachment(inputStream, elementToDecrypt)
        }
    }
}
