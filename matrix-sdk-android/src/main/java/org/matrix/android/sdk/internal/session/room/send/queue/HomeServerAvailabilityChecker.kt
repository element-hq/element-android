/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
