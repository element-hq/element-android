/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService
import org.matrix.android.sdk.api.extensions.orFalse
import timber.log.Timber
import javax.inject.Inject

class WifiDetector @Inject constructor(
        context: Context
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    fun isConnectedToWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork
                    ?.let { connectivityManager.getNetworkCapabilities(it) }
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    .orFalse()
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
                .also { Timber.d("isConnected to WiFi: $it") }
    }
}
