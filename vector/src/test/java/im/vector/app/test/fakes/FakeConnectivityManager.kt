/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk

class FakeConnectivityManager {
    val instance = mockk<ConnectivityManager>()

    fun givenNoActiveConnection() {
        every { instance.activeNetwork } returns null
    }

    fun givenHasActiveConnection() {
        val network = mockk<Network>()
        every { instance.activeNetwork } returns network

        val networkCapabilities = FakeNetworkCapabilities()
        networkCapabilities.givenTransports(
                NetworkCapabilities.TRANSPORT_CELLULAR,
                NetworkCapabilities.TRANSPORT_WIFI,
                NetworkCapabilities.TRANSPORT_VPN
        )
        every { instance.getNetworkCapabilities(network) } returns networkCapabilities.instance
    }
}
