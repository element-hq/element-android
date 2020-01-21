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

package im.vector.matrix.android.internal.network

import android.content.Context
import androidx.annotation.WorkerThread
import com.novoda.merlin.Endpoint
import com.novoda.merlin.Merlin
import com.novoda.merlin.MerlinsBeard
import com.novoda.merlin.ResponseCodeValidator
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig
import im.vector.matrix.android.internal.session.SessionScope
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface NetworkConnectivityChecker {
    /**
     * Returns true when internet is available
     */
    @WorkerThread
    fun hasInternetAccess(forcePing: Boolean): Boolean

    /**
     * Wait until we get internet connection.
     */
    suspend fun waitUntilConnected()

    fun register(listener: Listener)
    fun unregister(listener: Listener)

    interface Listener {
        fun onConnect() {
        }

        fun onDisconnect() {
        }
    }
}

@SessionScope
internal class MerlinNetworkConnectivityChecker @Inject constructor(context: Context,
                                                                    homeServerConnectionConfig: HomeServerConnectionConfig,
                                                                    private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                                                    private val backgroundDetectionObserver: BackgroundDetectionObserver)
    : NetworkConnectivityChecker {

    private val waitingForNetwork = AtomicBoolean(false)
    private val isMerlinBounded = AtomicBoolean(false)
    private val endpointString = "${homeServerConnectionConfig.homeServerUri}/_matrix/client/versions"
    private val endpoint = Endpoint.from(endpointString)
    private val responseCodeValidator = ResponseCodeValidator { responseCode ->
        responseCode == 204 || responseCode == 400 || responseCode == 404
    }

    private val merlin = Merlin.Builder()
            .withEndpoint(endpoint)
            .withResponseCodeValidator(responseCodeValidator)
            .withAllCallbacks()
            .build(context)

    private val merlinsBeard = MerlinsBeard.Builder()
            .withEndpoint(endpoint)
            .withResponseCodeValidator(responseCodeValidator)
            .build(context)

    private val hasInternetAccess = AtomicBoolean(merlinsBeard.isConnected)

    private val listeners = Collections.synchronizedSet(LinkedHashSet<NetworkConnectivityChecker.Listener>())

    private val backgroundDetectionObserverListener = object : BackgroundDetectionObserver.Listener {
        override fun onMoveToForeground() {
            bindMerlinIfNeeded()
        }

        override fun onMoveToBackground() {
            unbindMerlinIfNeeded()
        }
    }

    /**
     * Returns true when internet is available
     */
    @WorkerThread
    override fun hasInternetAccess(forcePing: Boolean): Boolean {
        return if (forcePing) {
            merlinsBeard.hasInternetAccess()
        } else {
            hasInternetAccess.get()
        }
    }

    private fun bindMerlinIfNeeded() {
        if (isMerlinBounded.get()) {
            return
        }
        Timber.v("Bind merlin")
        isMerlinBounded.set(true)
        merlin.bind()
        merlinsBeard.hasInternetAccess {
            hasInternetAccess.set(it)
        }
        merlin.registerBindable {
            Timber.v("On Network available: ${it.isAvailable}")
        }
        merlin.registerDisconnectable {
            Timber.v("On Disconnect")
            hasInternetAccess.set(false)
            val localListeners = listeners.toList()
            localListeners.forEach {
                it.onDisconnect()
            }
        }
        merlin.registerConnectable {
            Timber.v("On Connect")
            hasInternetAccess.set(true)
            val localListeners = listeners.toList()
            localListeners.forEach {
                it.onConnect()
            }
        }
    }

    private fun unbindMerlinIfNeeded() {
        if (backgroundDetectionObserver.isInBackground && !waitingForNetwork.get() && isMerlinBounded.get()) {
            isMerlinBounded.set(false)
            Timber.v("Unbind merlin")
            merlin.unbind()
        }
    }

    override suspend fun waitUntilConnected() {
        val hasInternetAccess = withContext(coroutineDispatchers.io) {
            merlinsBeard.hasInternetAccess()
        }
        if (hasInternetAccess) {
            return
        } else {
            waitingForNetwork.set(true)
            bindMerlinIfNeeded()
            Timber.v("Waiting for network...")
            suspendCoroutine<Unit> { continuation ->
                register(object : NetworkConnectivityChecker.Listener {
                    override fun onConnect() {
                        unregister(this)
                        waitingForNetwork.set(false)
                        unbindMerlinIfNeeded()
                        Timber.v("Connected to network...")
                        continuation.resume(Unit)
                    }
                })
            }
        }
    }

    override fun register(listener: NetworkConnectivityChecker.Listener) {
        if (listeners.isEmpty()) {
            if (backgroundDetectionObserver.isInBackground) {
                unbindMerlinIfNeeded()
            } else {
                bindMerlinIfNeeded()
            }
            backgroundDetectionObserver.register(backgroundDetectionObserverListener)
        }
        listeners.add(listener)
    }

    override fun unregister(listener: NetworkConnectivityChecker.Listener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            backgroundDetectionObserver.unregister(backgroundDetectionObserverListener)
        }
    }
}
