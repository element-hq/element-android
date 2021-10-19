/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send.queue

import org.matrix.android.sdk.api.auth.data.SessionParams
import timber.log.Timber
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

internal class HomeServerAvailabilityChecker(val sessionParams: SessionParams) {

    fun check(): Boolean {
        val host = sessionParams.homeServerConnectionConfig.homeServerUriBase.host ?: return false
        val port = sessionParams.homeServerConnectionConfig.homeServerUriBase.port.takeIf { it != -1 } ?: 80
        val timeout = 30_000
        try {
            Socket().use { socket ->
                val inetAddress: InetAddress = InetAddress.getByName(host)
                val inetSocketAddress = InetSocketAddress(inetAddress, port)
                socket.connect(inetSocketAddress, timeout)
                return true
            }
        } catch (e: IOException) {
            Timber.v("## EventSender isHostAvailable failure ${e.localizedMessage}")
            return false
        }
    }
}
