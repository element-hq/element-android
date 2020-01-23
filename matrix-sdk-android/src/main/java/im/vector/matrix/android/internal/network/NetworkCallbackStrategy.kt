/*
 * Copyright 2020 New Vector Ltd
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

import android.annotation.TargetApi
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import javax.inject.Inject

internal interface NetworkCallbackStrategy {
    fun register(hasChanged: () -> Unit)
    fun unregister()
}

internal class FallbackNetworkCallbackStrategy @Inject constructor(private val context: Context,
                                                                   private val networkInfoReceiver: NetworkInfoReceiver) : NetworkCallbackStrategy {

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

@TargetApi(android.os.Build.VERSION_CODES.N)
internal class PreferredNetworkCallbackStrategy @Inject constructor(context: Context) : NetworkCallbackStrategy {

    private var hasChangedCallback: (() -> Unit)? = null
    private val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        conn.registerDefaultNetworkCallback(networkCallback)
    }

    override fun unregister() {
        hasChangedCallback = null
        conn.unregisterNetworkCallback(networkCallback)
    }
}
