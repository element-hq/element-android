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
import com.novoda.merlin.Merlin
import com.novoda.merlin.MerlinsBeard
import im.vector.matrix.android.internal.di.MatrixScope
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@MatrixScope
internal class NetworkConnectivityChecker @Inject constructor(private val context: Context,
                                                              private val backgroundDetectionObserver: BackgroundDetectionObserver)
    : BackgroundDetectionObserver.Listener {

    private val merlin = Merlin.Builder()
            .withConnectableCallbacks()
            .withDisconnectableCallbacks()
            .build(context)

    private val merlinsBeard = MerlinsBeard.Builder().build(context)

    private val listeners = Collections.synchronizedSet(LinkedHashSet<Listener>())
    private var hasInternetAccess = merlinsBeard.isConnected

    init {
        backgroundDetectionObserver.register(this)
    }

    /**
     * Returns true when internet is available
     */
    @WorkerThread
    fun hasInternetAccess(): Boolean {
        // If we are in background we have unbound merlin, so we have to check
        return if (backgroundDetectionObserver.isInBackground) {
            merlinsBeard.hasInternetAccess()
        } else {
            hasInternetAccess
        }
    }

    override fun onMoveToForeground() {
        merlin.bind()
        merlinsBeard.hasInternetAccess {
            hasInternetAccess = it
        }
        merlin.registerDisconnectable {
            if (hasInternetAccess) {
                Timber.v("On Disconnect")
                hasInternetAccess = false
                val localListeners = listeners.toList()
                localListeners.forEach {
                    it.onDisconnect()
                }
            }
        }
        merlin.registerConnectable {
            if (!hasInternetAccess) {
                Timber.v("On Connect")
                hasInternetAccess = true
                val localListeners = listeners.toList()
                localListeners.forEach {
                    it.onConnect()
                }
            }
        }
    }

    override fun onMoveToBackground() {
        merlin.unbind()
    }

    // In background you won't get notification as merlin is unbound
    suspend fun waitUntilConnected() {
        if (hasInternetAccess) {
            return
        } else {
            Timber.v("Waiting for network...")
            suspendCoroutine<Unit> { continuation ->
                register(object : Listener {
                    override fun onConnect() {
                        unregister(this)
                        Timber.v("Connected to network...")
                        continuation.resume(Unit)
                    }
                })
            }
        }
    }

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    interface Listener {
        fun onConnect() {
        }

        fun onDisconnect() {
        }
    }
}
