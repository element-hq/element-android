/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk

class FakeNetworkCapabilities {
    val instance = mockk<NetworkCapabilities>()

    fun givenTransports(vararg type: Int) {
        every { instance.hasTransport(any()) } answers {
            val input = it.invocation.args.first() as Int
            type.contains(input)
        }
    }
}
