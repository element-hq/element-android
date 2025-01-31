/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.network

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.core.content.getSystemService
import timber.log.Timber
import javax.inject.Inject

internal interface NetworkCallbackStrategy {
    fun register(hasChanged: () -> Unit)
    fun unregister()
}

internal class FallbackNetworkCallbackStrategy @Inject constructor(
        private val context: Context,
        private val networkInfoReceiver: NetworkInfoReceiver
) : NetworkCallbackStrategy {

    @Suppress("DEPRECATION")
    val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    override fun register(hasChanged: () -> Unit) {
        networkInfoReceiver.isConnectedCallback = {
            hasChanged()
        }
        context.registerReceiver(networkInfoReceiver, filter)
    }

    override fun unregister() {
        networkInfoReceiver.isConnectedCallback = null
        context.unregisterReceiver(networkInfoReceiver)
    }
}

@TargetApi(Build.VERSION_CODES.N)
internal class PreferredNetworkCallbackStrategy @Inject constructor(context: Context) : NetworkCallbackStrategy {

    private var hasChangedCallback: (() -> Unit)? = null
    private val conn = context.getSystemService<ConnectivityManager>()!!
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) {
            hasChangedCallback?.invoke()
        }

        override fun onAvailable(network: Network) {
            hasChangedCallback?.invoke()
        }
    }

    override fun register(hasChanged: () -> Unit) {
        hasChangedCallback = hasChanged
        // Add a try catch for safety
        // XXX: It happens when running all tests in CI, at some points we reach a limit here causing TooManyRequestsException
        // and crashing the sync thread. We might have problem here, would need some investigation
        // for now adding a catch to allow CI to continue running
        try {
            conn.registerDefaultNetworkCallback(networkCallback)
        } catch (t: Throwable) {
            Timber.e(t, "Unable to register default network callback")
        }
    }

    override fun unregister() {
        // It can crash after an application update, if not registered
        val doUnregister = hasChangedCallback != null
        hasChangedCallback = null
        if (doUnregister) {
            // Add a try catch for safety
            try {
                conn.unregisterNetworkCallback(networkCallback)
            } catch (t: Throwable) {
                Timber.e(t, "Unable to unregister network callback")
            }
        }
    }
}
