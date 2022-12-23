/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.test.fakes

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
