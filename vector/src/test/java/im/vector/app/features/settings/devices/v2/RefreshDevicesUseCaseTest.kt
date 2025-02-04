/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.coEvery
import io.mockk.coVerifyAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap

class RefreshDevicesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val refreshDevicesUseCase = RefreshDevicesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given current session when refreshing then devices list and keys are fetched`() {
        val session = fakeActiveSessionHolder.fakeSession
        coEvery { session.cryptoService().fetchDevicesList() } returns emptyList()
        coEvery { session.cryptoService().downloadKeysIfNeeded(any(), any()) } returns MXUsersDevicesMap()

        runBlocking {
            refreshDevicesUseCase.execute()
        }

        coVerifyAll {
            session.cryptoService().fetchDevicesList()
            session.cryptoService().downloadKeysIfNeeded(listOf(session.myUserId), true)
        }
    }
}
