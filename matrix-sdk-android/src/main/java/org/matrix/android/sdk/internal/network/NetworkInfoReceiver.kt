/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

// This BroadcastReceiver is used only if the build code is below 24.
@file:Suppress("DEPRECATION")

package org.matrix.android.sdk.internal.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.content.getSystemService
import javax.inject.Inject

internal class NetworkInfoReceiver @Inject constructor() : BroadcastReceiver() {

    var isConnectedCallback: ((Boolean) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        val conn = context.getSystemService<ConnectivityManager>()!!
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo
        isConnectedCallback?.invoke(networkInfo?.isConnected ?: false)
    }
}
