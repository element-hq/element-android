/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import androidx.annotation.WorkerThread
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.homeserver.HomeServerPinger
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal interface NetworkConnectivityChecker {
    /**
     * Returns true when internet is available.
     */
    @WorkerThread
    fun hasInternetAccess(forcePing: Boolean): Boolean

    fun register(listener: Listener)
    fun unregister(listener: Listener)

    interface Listener {
        fun onConnectivityChanged()
    }
}

@SessionScope
internal class DefaultNetworkConnectivityChecker @Inject constructor(
        private val homeServerPinger: HomeServerPinger,
        private val backgroundDetectionObserver: BackgroundDetectionObserver,
        private val networkCallbackStrategy: NetworkCallbackStrategy
) :
        NetworkConnectivityChecker {

    private val hasInternetAccess = AtomicBoolean(true)
    private val listeners = Collections.synchronizedSet(LinkedHashSet<NetworkConnectivityChecker.Listener>())
    private val backgroundDetectionObserverListener = object : BackgroundDetectionObserver.Listener {
        override fun onMoveToForeground() {
            bind()
        }

        override fun onMoveToBackground() {
            unbind()
        }
    }

    /**
     * Returns true when internet is available.
     */
    @WorkerThread
    override fun hasInternetAccess(forcePing: Boolean): Boolean {
        return if (forcePing) {
            runBlocking {
                homeServerPinger.canReachHomeServer()
            }
        } else {
            hasInternetAccess.get()
        }
    }

    override fun register(listener: NetworkConnectivityChecker.Listener) {
        if (listeners.isEmpty()) {
            if (!backgroundDetectionObserver.isInBackground) {
                bind()
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

    private fun bind() {
        networkCallbackStrategy.register {
            val localListeners = listeners.toList()
            localListeners.forEach {
                it.onConnectivityChanged()
            }
        }
        homeServerPinger.canReachHomeServer {
            hasInternetAccess.set(it)
        }
    }

    private fun unbind() {
        networkCallbackStrategy.unregister()
    }
}
