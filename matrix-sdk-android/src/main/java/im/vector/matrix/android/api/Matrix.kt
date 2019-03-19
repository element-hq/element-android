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

package im.vector.matrix.android.api

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MatrixKoinHolder
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import im.vector.matrix.android.internal.network.UserAgentHolder
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import org.koin.standalone.inject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This is the main entry point to the matrix sdk.
 * This class is automatically init by a provider.
 * To get the singleton instance, use getInstance static method.
 */
class Matrix private constructor(context: Context) : MatrixKoinComponent {

    private val authenticator by inject<Authenticator>()
    private val userAgent by inject<UserAgentHolder>()
    private val backgroundDetectionObserver by inject<BackgroundDetectionObserver>()
    lateinit var currentSession: Session

    init {
        Monarchy.init(context)
        val matrixModule = MatrixModule(context).definition
        val networkModule = NetworkModule().definition
        val authModule = AuthModule().definition
        MatrixKoinHolder.instance.loadModules(listOf(matrixModule, networkModule, authModule))
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
        val lastActiveSession = authenticator.getLastActiveSession()
        if (lastActiveSession != null) {
            currentSession = lastActiveSession
            currentSession.open()
        }
    }

    fun authenticator(): Authenticator {
        return authenticator
    }

    /**
     * Set application flavor, to alter user agent.
     */
    fun setApplicationFlavor(flavor: String) {
        userAgent.setApplicationFlavor(flavor)
    }

    companion object {
        private lateinit var instance: Matrix
        private val isInit = AtomicBoolean(false)

        internal fun initialize(context: Context) {
            if (isInit.compareAndSet(false, true)) {
                instance = Matrix(context.applicationContext)
            }
        }

        fun getInstance(): Matrix {
            return instance
        }

    }

}
