/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verifyAll
import org.junit.Test
import org.matrix.android.sdk.api.NoOpMatrixCallback

class RefreshDevicesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val refreshDevicesUseCase = RefreshDevicesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given current session when refreshing then devices list and keys are fetched`() {
        val session = fakeActiveSessionHolder.fakeSession
        every { session.cryptoService().fetchDevicesList(any()) } just runs
        every { session.cryptoService().downloadKeys(any(), any(), any()) } just runs

        refreshDevicesUseCase.execute()

        verifyAll {
            session.cryptoService().fetchDevicesList(match { it is NoOpMatrixCallback })
            session.cryptoService().downloadKeys(listOf(session.myUserId), true, match { it is NoOpMatrixCallback })
        }
    }
}
