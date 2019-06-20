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
import im.vector.matrix.android.BuildConfig
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.pushrules.Action
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.di.MatrixKoinHolder
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import im.vector.matrix.android.internal.network.UserAgentHolder
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import org.koin.standalone.get
import org.koin.standalone.inject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This is the main entry point to the matrix sdk.
 * This class is automatically init by a provider.
 * To get the singleton instance, use getInstance static method.
 */
class Matrix private constructor(context: Context) : MatrixKoinComponent {

    private val authenticator by inject<Authenticator>()
    private val userAgentHolder by inject<UserAgentHolder>()
    private val backgroundDetectionObserver by inject<BackgroundDetectionObserver>()
    var currentSession: Session? = null

    init {
        Monarchy.init(context)
        val matrixModule = MatrixModule(context).definition
        val networkModule = NetworkModule().definition
        val authModule = AuthModule().definition
        MatrixKoinHolder.instance.loadModules(listOf(matrixModule, networkModule, authModule))
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundDetectionObserver)
        authenticator.getLastActiveSession()?.also {
            currentSession = it
            it.open()
            it.setFilter(FilterService.FilterPreset.RiotFilter)
            it.startSync()
        }
    }

    fun authenticator(): Authenticator {
        return authenticator
    }

    /**
     * Set application flavor, to alter user agent.
     */
    fun setApplicationFlavor(flavor: String) {
        userAgentHolder.setApplicationFlavor(flavor)
    }

    fun getUserAgent() = userAgentHolder.userAgent

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

        fun getSdkVersion(): String {
            return BuildConfig.VERSION_NAME + " (" + BuildConfig.GIT_SDK_REVISION + ")"
        }
    }

}
